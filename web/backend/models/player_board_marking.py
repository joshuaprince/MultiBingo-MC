from django.db import models

from backend.models.color import Color


class PlayerBoardMarking(models.Model):
    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)

    color = models.IntegerField(choices=Color.choices)
    covert_marked = models.BooleanField(default=False)

    auto_marker_client_id = models.CharField(max_length=1024, blank=True)
    """
    Contains the Client ID of a plugin that is performing auto-marking of this space for this 
    player.
    """

    marked_by_player = models.BooleanField(default=False)
    """
    If true, prevents any plugins from auto-marking this space for this player, because the player
    has modified the marking themselves.
    """

    announced = models.BooleanField(default=False)
