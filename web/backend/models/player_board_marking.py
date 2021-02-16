from django.db import models


class PlayerBoardMarking(models.Model):
    class Marking(models.IntegerChoices):
        UNMARKED = 0
        COMPLETE = 1
        REVERTED = 2
        INVALIDATED = 3
        NOT_INVALIDATED = 4

    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)
    color = models.IntegerField()

    def to_json(self):
        return {
            'space_id': self.space.pk,
            # 'position': self.space.position.to_json(),
            'color': self.color,
        }
