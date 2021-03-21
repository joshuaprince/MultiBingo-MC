from __future__ import annotations

from functools import lru_cache
from timeit import default_timer
from typing import Optional, List, TYPE_CHECKING, Tuple, FrozenSet

from win_detection.registry import win_detector
from win_detection.winning_markings import WINNING_MARKINGS

if TYPE_CHECKING:
    from backend.models.player_board import PlayerBoard
    from backend.models.player_board_marking import PlayerBoardMarking
    from backend.models.position import Position
    from backend.models.space import Space


WIN_LENGTH = 6
DEPTH_LIMIT = 6
"""
The greatest number of spaces that can be in a chain. This problem is NP-hard, so it takes a very
long time if set too high.
"""


@win_detector("Hexagonal snaking win", ['hexagon'])
def hex_snake(pboard, markings) -> Optional[List[Space]]:
    return _hex_snake(pboard, markings, True)


@win_detector("Hexagonal snaking, no neighbor", ['hexagon'])
def hex_snake_neighborless(pboard, markings) -> Optional[List[Space]]:
    return _hex_snake(pboard, markings, False)


def _hex_snake(pboard: PlayerBoard, markings: List[PlayerBoardMarking],
               allow_neighbors: bool) -> Optional[List[Space]]:
    positions = (pbm.space.position for pbm in markings if pbm.color in WINNING_MARKINGS)
    start_time = default_timer()
    longest = _longest_chain(tuple(), frozenset(positions), allow_neighbors)
    end_time = default_timer()
    # _makes_snake(positions)
    print(end_time - start_time)
    print(_longest_chain.cache_info())
    return [pos.space for pos in longest] if len(longest) >= WIN_LENGTH else None


@lru_cache(maxsize=10000)
def _longest_chain(current: Tuple[Position, ...],
                   candidates: FrozenSet[Position],
                   allow_neigh: bool):
    if len(current) == 0:
        next_node_choices = candidates
    else:
        if allow_neigh:
            next_node_choices = _neighbors(current[-1], candidates)
        else:
            next_node_choices = frozenset(n for n in _neighbors(current[-1], candidates)
                                          if len(current) < 2
                                          or n not in _neighbors(current[-2], candidates))

    if len(current) >= DEPTH_LIMIT:
        return current

    longest_chain = current
    for neigh in next_node_choices:
        new_chain = current + (neigh,)
        new_candidates = frozenset(c for c in candidates if c != neigh)
        longest_from = _longest_chain(new_chain, new_candidates, allow_neigh)
        if len(longest_from) > len(longest_chain):
            longest_chain = longest_from
        if len(longest_chain) >= DEPTH_LIMIT:
            break
    return longest_chain


def _neighbors(of: Position, candidates: FrozenSet[Position]):
    """
    Get a list of neighbors to a specific hexagon that occur in `candidates`.
    """
    ret = set()
    for other in candidates:
        if _is_neighbor(of, other):
            ret.add(other)
    return frozenset(ret)


def _is_neighbor(left: Position, right: Position):
    if left.x == right.x:
        return abs(left.y - right.y) == 1
    if left.y == right.y:
        return abs(left.z() - right.z()) == 1
    if left.z() == right.z():
        return abs(left.x - right.x) == 1
    return False
