from django.db import models

from backend.models.color import Color


class PlayerBoardMarking(models.Model):
    space = models.ForeignKey('Space', on_delete=models.CASCADE)
    player_board = models.ForeignKey('PlayerBoard', on_delete=models.CASCADE)
    auto_marker_client_id = models.CharField(max_length=1024, blank=True)

    color = models.IntegerField(choices=Color.choices)
    covert_marked = models.BooleanField(default=False)

    def to_json(self, include_covert: bool = False):
        ret = {
            'space_id': self.space_id,
            'color': self.color,
        }

        if include_covert:
            ret['covert_marked'] = self.covert_marked

        return ret
