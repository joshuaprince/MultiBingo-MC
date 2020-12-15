import random

from .difficulty import Difficulty
from .goals import get_goals


class BoardGenerator:
    def __init__(self, board_difficulty: Difficulty, seed=None):
        self.board_difficulty = board_difficulty
        self.rand = random.Random(seed)

    def generate(self):
        diff_spread = self._get_difficulty_spread()
        goals = get_goals(self.rand, diff_spread)

    def _get_difficulty_spread(self):
        """
        Returns tuple of length len(Difficulty) of how many squares of each difficulty to place
        """
        if self.board_difficulty == Difficulty.VERY_EASY:
            return 25, 0, 0, 0, 0

        if self.board_difficulty == Difficulty.EASY:
            num_ez = self.rand.randint(15, 19)
            return (25 - num_ez), num_ez, 0, 0, 0

        if self.board_difficulty == Difficulty.MEDIUM:
            num_med = self.rand.randint(15, 19)
            return 0, (25 - num_med), num_med, 0, 0

        if self.board_difficulty == Difficulty.HARD:
            num_hard = self.rand.randint(15, 19)
            return 0, 0, (25 - num_hard), num_hard, 0

        if self.board_difficulty == Difficulty.VERY_HARD:
            num_vhard = self.rand.randint(15, 19)
            return 0, 0, 0, (25 - num_vhard), num_vhard

        raise ValueError(f"Unknown board difficulty: {self.board_difficulty}")

    def _get_square(self, difficulty: Difficulty):
        """
        Get a square with given difficulty
        """
