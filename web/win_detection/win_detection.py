from __future__ import annotations

from typing import List, Optional, TYPE_CHECKING

from win_detection.registry import WIN_DETECTORS
from backend.models.board_shape import BoardShape
from backend.models.player_board_marking import PlayerBoardMarking

if TYPE_CHECKING:
    from backend.models.player_board import PlayerBoard
    from backend.models.space import Space

# Import all win detectors
# noinspection PyUnresolvedReferences
from . import *


def win_detector_choices():
    return [(func.__name__, func.friendly_name) for func in WIN_DETECTORS]


def get_win_detector(name: str):
    return next((func for func in WIN_DETECTORS if func.__name__ == name), None)


def get_default_win_detector(shape: BoardShape):
    if shape == BoardShape.SQUARE:
        return wd_bingo_standard.bingo_standard
    elif shape == BoardShape.HEXAGON:
        return wd_hex_snake.hex_snake_neighborless
    else:
        return None


def winning_space_ids(pboard: PlayerBoard) -> Optional[List[int]]:
    detector_func = get_win_detector(pboard.board.win_detector)

    if not detector_func:
        return None

    try:
        markings = list(PlayerBoardMarking.objects
                        .filter(player_board=pboard)
                        .select_related('space__position', 'space__board'))
        win_markings = detector_func(pboard, markings)  # type: List[Space]
    except Exception as e:
        win_markings = []
        print(e)
    if win_markings:
        return [space.pk for space in win_markings]
    else:
        return None
