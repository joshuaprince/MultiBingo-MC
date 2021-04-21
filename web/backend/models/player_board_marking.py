from django.db import models

from backend.models.color import Color


class PlayerBoardMarking(models.Model):
    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)

    auto_marker_client_id = models.CharField(max_length=1024, blank=True)
    color = models.IntegerField(choices=Color.choices)
    covert_marked = models.BooleanField(default=False)

    announced = models.BooleanField(default=False)
