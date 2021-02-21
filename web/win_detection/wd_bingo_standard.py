from typing import Optional, List

from django.db.models import F

from win_detection.registry import win_detector
from win_detection.winning_markings import WINNING_MARKINGS

BOARD_SIZE = 5


@win_detector("Standard Bingo rules", ['square'])
def bingo_standard(pboard) -> Optional[List]:
    winning_spaces = set()

    # Rows and columns
    for i in range(BOARD_SIZE):
        row = list(pboard.playerboardmarking_set.filter(space__position__y=i))
        if len(row) > 0 and all(spc.color in WINNING_MARKINGS for spc in row):
            winning_spaces.update(row)

        col = list(pboard.playerboardmarking_set.filter(space__position__x=i))
        if len(col) > 0 and all(spc.color in WINNING_MARKINGS for spc in col):
            winning_spaces.update(col)

        if len(row) == len(col) == 0:
            # Out of spaces
            break

    # Diagonals
    # Top left - bottom right
    tlbr = pboard.playerboardmarking_set.filter(space__position__x=F('space__position__y'))
    if len(tlbr) > 0 and all(spc.color in WINNING_MARKINGS for spc in tlbr):
        winning_spaces.update(tlbr)
    # Top right - bottom left
    trbl = pboard.playerboardmarking_set.filter(space__position__x=BOARD_SIZE - 1 - F('space__position__y'))
    if len(trbl) > 0 and all(spc.color in WINNING_MARKINGS for spc in trbl):
        winning_spaces.update(trbl)

    return list(winning_spaces) or None
