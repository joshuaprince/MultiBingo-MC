from django.db import models

from backend.models.board_shape import BoardShape


class Board(models.Model):
    game_code = models.SlugField(unique=True, max_length=128)
    shape = models.CharField(max_length=16, choices=BoardShape.choices)

    obscured = models.BooleanField(default=True)

    def __str__(self):
        return self.game_code
