from django.db import models

from backend.models.color import Color


class PlayerBoardMarking(models.Model):
    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)
    color = models.IntegerField(choices=Color.choices)

    def to_json(self):
        return {
            'space_id': self.space.pk,
            'color': self.color,
        }
