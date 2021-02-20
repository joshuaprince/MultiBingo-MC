from django.db import models

from backend.models.board_shape import BoardShape


class Position(models.Model):
    """
    Representation of where a Space exists on a board.

    For `square` boards, the position in the upper left of the board is represented as (x = y = 0).
    Each space to the right increments `x` by 1, and down `y`.

    For `hexagon` boards, positions are stored as `axial coordinates with x=q and y=r
    <https://www.redblobgames.com/grids/hexagons/#coordinates-axial>`_. The upper left is
    represented as (x = y = 0). Each space to the right increments `x` by 1. Each space down and to
    the right increments `y` by 1. This makes it possible for coordinates to be negative. However,
    all coordinates must be no further above or to the left of (0, 0).
    """
    x = models.IntegerField()
    y = models.IntegerField()

    def z(self) -> int:
        """
        Get the implied z coordinate of this position if it is a hexagonal space. By the definition
        of `axial coordinates <https://www.redblobgames.com/grids/hexagons/#coordinates-axial>`_,
        the x, y, and z coordinates must add to 0.
        :raises: ValueError if this is not a hexagonal space
        :return: This space's z coordinate
        """
        # Avoiding circular import
        if self.space.board.shape != BoardShape.HEXAGON:
            raise ValueError("Cannot determine Z coordinate of non-hexagonal position")
        return -self.x - self.y

    def __str__(self):
        if self.space.board.shape == BoardShape.HEXAGON:
            return str((self.x, self.y, self.z()))
        else:
            return str((self.x, self.y))

    def to_json(self):
        return {
            'x': self.x,
            'y': self.y,
        }

    class Meta:
        ordering = ['y', 'x']  # Y first so it goes left-right then top-down
