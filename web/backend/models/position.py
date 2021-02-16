from django.db import models


class Position(models.Model):
    x = models.IntegerField()
    y = models.IntegerField()

    def __str__(self):
        return str((self.x, self.y))

    def to_json(self):
        return {
            'x': self.x,
            'y': self.y,
        }

    class Meta:
        ordering = ['y', 'x']  # Y first so it goes left-right then top-down
