from typing import List

WIN_DETECTORS = []


def win_detector(friendly_name: str, board_shapes: List = None):
    """
    Usage: decorate a win detection function with this decorator. Detector function will receive
    a PlayerBoard object and should return either a list of spaces that constitute a "win", or None
    if this board is not a winner.

    Don't forget to add the new win detector to __init__ so that it is imported/discovered.
    """
    def inner(func):
        func.friendly_name = friendly_name
        func.board_shapes = board_shapes
        WIN_DETECTORS.append(func)
        return func
    return inner
