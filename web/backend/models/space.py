from django.db import models

from backend.models.color import Color
from generation.goals import ConcreteGoal


class Space(models.Model):
    board = models.ForeignKey('Board', on_delete=models.CASCADE)
    position = models.OneToOneField('Position', on_delete=models.CASCADE)

    text = models.CharField(max_length=256)
    tooltip = models.CharField(max_length=512)

    xml_id = models.SlugField(max_length=256)
    """`id` field of the goal from XML. Should only be used to pull trigger data."""

    def __str__(self):
        return str(self.board) + " @" + str(self.position)

    class Meta:
        ...
        # unique_together = ['board']
        # TODO uniqueness validation for spaces

    def initial_state(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        if cg.template.type == 'negative':
            return Color.NOT_INVALIDATED
        else:
            return Color.UNMARKED
