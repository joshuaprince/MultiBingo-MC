from win_detection.registry import WIN_DETECTORS

# Import all win detectors
# noinspection PyUnresolvedReferences
from . import *


def win_detector_choices():
    return [(func.__name__, func.friendly_name) for func in WIN_DETECTORS]


def get_win_detector(name: str):
    return next((func for func in WIN_DETECTORS if func.__name__ == name), None)


def winning_space_ids(pboard):
    detector_func = get_win_detector(pboard.board.win_detector)

    if not detector_func:
        return None

    win_markings = detector_func(pboard)
    if win_markings:
        return [pbm.space.pk for pbm in win_markings]
    else:
        return None
