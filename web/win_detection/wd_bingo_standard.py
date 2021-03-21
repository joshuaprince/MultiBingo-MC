from __future__ import annotations

from typing import Optional, List, Set, TYPE_CHECKING

from win_detection.registry import win_detector
from win_detection.winning_markings import WINNING_MARKINGS

if TYPE_CHECKING:
    from backend.models.player_board import PlayerBoard
    from backend.models.player_board_marking import PlayerBoardMarking
    from backend.models.space import Space

BOARD_SIZE = 5


@win_detector("Standard Bingo rules", ['square'])
def bingo_standard(pboard: PlayerBoard, markings: List[PlayerBoardMarking]) -> Optional[List[Space]]:
    winning_pbms = set()  # type: Set[PlayerBoardMarking]

    # Rows and columns
    for i in range(BOARD_SIZE):
        row = list(p for p in markings if p.space.position.y == i)
        if len(row) > 0 and all(spc.color in WINNING_MARKINGS for spc in row):
            winning_pbms.update(row)

        col = list(p for p in markings if p.space.position.x == i)
        if len(col) > 0 and all(spc.color in WINNING_MARKINGS for spc in col):
            winning_pbms.update(col)

        if len(row) == len(col) == 0:
            # Out of spaces
            break

    # Diagonals
    # Top left - bottom right
    tlbr = list(p for p in markings if p.space.position.x == p.space.position.y)
    if len(tlbr) > 0 and all(spc.color in WINNING_MARKINGS for spc in tlbr):
        winning_pbms.update(tlbr)
    # Top right - bottom left
    trbl = list(p for p in markings if p.space.position.x == BOARD_SIZE - 1 - p.space.position.y)
    if len(trbl) > 0 and all(spc.color in WINNING_MARKINGS for spc in trbl):
        winning_pbms.update(trbl)

    return [pbm.space for pbm in winning_pbms]
