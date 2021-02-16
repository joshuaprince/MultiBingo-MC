from django.db import models

from backend.models.player_board_marking import PlayerBoardMarking
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

    def is_autoactivated(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        return cg.template.is_autoactivated

    def initial_state(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        if cg.template.type == 'negative':
            return PlayerBoardMarking.Marking.NOT_INVALIDATED
        else:
            return PlayerBoardMarking.Marking.UNMARKED

    def to_player_json(self):
        return {
            'space_id': self.pk,
            'position': self.position.to_json(),
            'text': self.text,
            'tooltip': self.tooltip,
            'auto': self.is_autoactivated(),
        }

    def to_plugin_json(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        return {
            'space_id': self.pk,
            'goal_id': cg.template.id,
            'text': self.text,
            'type': cg.template.type,
            'variables': cg.variables,
            'triggers': cg.triggers(),
        }


