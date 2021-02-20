import random
import string
from typing import List

from django.db import transaction

from backend.models import BoardShape, Board, Position, Space
from .goals import get_goals


@transaction.atomic
def generate_board(game_code: str = None,
                   shape: BoardShape = BoardShape.SQUARE,
                   board_difficulty: int = 2,
                   seed: str = None,
                   forced_goals: List[str] = None) -> Board:
    """
    Generate a board and all spaces with the given parameters.
    :param game_code: Unique identifier for the board, or None for a random string.
    :param shape: Shape of the board, square or hexagon.
    :param board_difficulty: Overall difficulty of the board, used to generate a spread of
                             difficulties for each space.
    :param seed: Seed to use in generation, or None to use a random seed.
    :param forced_goals: List of goal IDs that will be forced to be on the board.
    :return: The newly created Board instance.
    """
    game_code = game_code or _get_random_game_code()
    rand = random.Random(seed)
    difficulty_spread = _get_difficulty_spread(board_difficulty, rand)

    board = Board.objects.create(game_code=game_code, shape=shape)

    positions = _get_positions(shape)
    goals = get_goals(rand, difficulty_spread, forced_goals=forced_goals)  # TODO always 25
    for pos, goal in zip(positions, goals):
        pos.save()
        Space.objects.create(board=board, position=pos, text=goal.description(),
                             tooltip=goal.tooltip(), xml_id=goal.xml_id())

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
            (1, 0), (2, 0), (3, 0),
            (0, 1), (1, 1), (2, 1), (3, 1),
            (-1, 2), (0, 2), (1, 2), (2, 2), (3, 2),
            (-1, 3), (0, 3), (1, 3), (2, 3),
            (-1, 4), (0, 4), (1, 4)
        ]
    else:
        positions = [((i % 5), (i // 5)) for i in range(25)]

    return [Position(x=x, y=y) for (x, y) in positions]


def _get_difficulty_spread(board_difficulty, rand: random.Random):
    """
    Returns tuple of length len(Difficulty) of how many spaces of each difficulty to place
    """
    if board_difficulty == 0:
        return 25, 0, 0, 0, 0

    if board_difficulty == 1:
        num_ez = rand.randint(15, 19)
        return (25 - num_ez), num_ez, 0, 0, 0

    if board_difficulty == 2:
        num_med = rand.randint(15, 19)
        return 0, (25 - num_med), num_med, 0, 0

    if board_difficulty == 3:
        num_hard = rand.randint(15, 19)
        return 0, 0, (25 - num_hard), num_hard, 0

    if board_difficulty == 4:
        num_vhard = rand.randint(15, 19)
        return 0, 0, 0, (25 - num_vhard), num_vhard

    # The Even spread
    if board_difficulty == 8:
        return 5, 5, 5, 5, 5

    # The Josh spread
    if board_difficulty == 9:
        return 2, 5, 8, 6, 4

    raise ValueError(f"Unknown board difficulty: {board_difficulty}")


def _get_random_game_code():
    return ''.join(random.choices(string.ascii_uppercase, k=6))
