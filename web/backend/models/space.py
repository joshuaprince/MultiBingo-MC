from django.db import models

from backend.models.color import Color
from generation.goals import ConcreteGoal


class Space(models.Model):
    board = models.ForeignKey('Board', on_delete=models.CASCADE)
    position = models.OneToOneField('Position', on_delete=models.CASCADE)

    goal_id = models.SlugField(max_length=256)

    def __str__(self):
        return str(self.board) + " @" + str(self.position)

    class Meta:
        ...
        # unique_together = ['board']
        # TODO uniqueness validation for spaces

    def initial_state(self):
        cg = ConcreteGoal.from_space(self)
        if cg.template.type == 'negative':
            return Color.NOT_INVALIDATED
        else:
            return Color.UNMARKED
