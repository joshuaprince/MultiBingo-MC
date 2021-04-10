import random
import string
from typing import List

from django.db import transaction
from rest_framework.exceptions import ValidationError

from backend.models.board_shape import BoardShape
from backend.models.board import Board
from backend.models.position import Position
from backend.models.space import Space
from backend.models.set_variable import SetVariable
from win_detection.win_detection import get_win_detector, get_default_win_detector
from .goals import get_goals


@transaction.atomic
def generate_board(game_code: str = None,
                   shape: BoardShape = BoardShape.HEXAGON,
                   win_detector: str = None,
                   seed: str = None,
                   forced_goals: List[str] = None) -> Board:
    """
    Generate a board and all spaces with the given parameters.
    :param game_code: Unique identifier for the board, or None for a random string.
    :param shape: Shape of the board, square or hexagon.
    :param win_detector: Win detector function to use for this board, or None to use the board shape
                         default.
    :param seed: Seed to use in generation, or None to use a random seed.
    :param forced_goals: List of goal IDs that will be forced to be on the board.
    :return: The newly created Board instance.
    """
    game_code = game_code or _get_random_game_code()
    rand = random.Random(seed)
    easy_proportion = rand.uniform(0.25, 0.4)  # Proportion of goals on the board that are easier

    if win_detector:
        wd_func = get_win_detector(win_detector)
        if shape not in wd_func.board_shapes:
            raise ValidationError("Win detector incompatible with board shape")
    else:
        wd_func = get_default_win_detector(shape)
    win_detector = wd_func.__name__

    board = Board.objects.create(game_code=game_code, shape=shape, win_detector=win_detector)

    positions = _get_positions(shape)
    goals = get_goals(rand, len(positions), easy_proportion, forced_goals=forced_goals)
    for pos, goal in zip(positions, goals):
        pos.save()
        spc = Space.objects.create(board=board, position=pos, goal_id=goal.template.id)
        for variable_name, variable_value in goal.variables.items():
            SetVariable.objects.create(space=spc, name=variable_name, value=variable_value)

    print(f"Created a new {shape} board {game_code}")
    return board


def _get_positions(shape: BoardShape):
    """
    Get a list of positions for the spaces on this board.
    :param shape: Shape of the board, square or heagon.
    :return: A list of Position objects that are NOT saved to the database.
    """
    if shape == BoardShape.HEXAGON:
        positions = [
            (1, 0), (2, 0), (3, 0), (4, 0),
            (0, 1), (1, 1), (2, 1), (3, 1), (4, 1),
            (-1, 2), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2),
            (-1, 3), (0, 3), (1, 3), (2, 3), (3, 3),
            (-1, 4), (0, 4), (1, 4), (2, 4),
        ]
    else:
        positions = [((i % 5), (i // 5)) for i in range(25)]

    return [Position(x=x, y=y) for (x, y) in positions]


def _get_random_game_code():
    return ''.join(random.choices(string.ascii_uppercase, k=6))
