import random
from typing import List

from .goals import get_goals


class BoardGenerator:
    def __init__(self, board_difficulty: int, seed=None, forced_goals: List[str] = None):
        self.board_difficulty = board_difficulty
        self.rand = random.Random(seed)
        self.forced_goals = forced_goals or []

    def generate(self):
        diff_spread = self._get_difficulty_spread()
        goals = get_goals(self.rand, diff_spread, forced_goals=self.forced_goals)
        return goals

    def _get_difficulty_spread(self):
        """
        Returns tuple of length len(Difficulty) of how many squares of each difficulty to place
        """
        if self.board_difficulty == 0:
            return 25, 0, 0, 0, 0

        if self.board_difficulty == 1:
            num_ez = self.rand.randint(15, 19)
            return (25 - num_ez), num_ez, 0, 0, 0

        if self.board_difficulty == 2:
            num_med = self.rand.randint(15, 19)
            return 0, (25 - num_med), num_med, 0, 0

        if self.board_difficulty == 3:
            num_hard = self.rand.randint(15, 19)
            return 0, 0, (25 - num_hard), num_hard, 0

        if self.board_difficulty == 4:
            num_vhard = self.rand.randint(15, 19)
            return 0, 0, 0, (25 - num_vhard), num_vhard

        # The Even spread
        if self.board_difficulty == 8:
            return 5, 5, 5, 5, 5

        # The Josh spread
        if self.board_difficulty == 9:
            return 2, 5, 8, 6, 4

        raise ValueError(f"Unknown board difficulty: {self.board_difficulty}")
