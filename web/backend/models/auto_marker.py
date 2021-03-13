from django.db import models


class AutoMarker(models.Model):
    """
    Tracks a connection between a Space, Player, and WebSocket client, indicating that the Space is
    being automatically marked by that WebSocket client for that player.
    """
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)
    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    client_id = models.CharField(max_length=1024)
