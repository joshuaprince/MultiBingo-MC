from rest_framework import serializers

from backend.models.board import Board
from backend.models.space import Space
from generation.goals import ConcreteGoal


class SpacePluginSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ['space_id', 'goal_id', 'text', 'type', 'variables', 'triggers']

    space_id = serializers.IntegerField(source='pk')
    goal_id = serializers.SerializerMethodField()
    type = serializers.SerializerMethodField()
    variables = serializers.SerializerMethodField()
    triggers = serializers.SerializerMethodField()

    @staticmethod
    def _concrete_goal(instance):
        return ConcreteGoal.from_xml_id(instance.xml_id) if instance else None

    def get_goal_id(self, obj):
        return self._concrete_goal(obj).template.id

    def get_type(self, obj):
        return self._concrete_goal(obj).template.type

    def get_variables(self, obj):
        return self._concrete_goal(obj).variables

    def get_triggers(self, obj):
        return self._concrete_goal(obj).triggers()


class BoardPluginSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = ['spaces']

    spaces = SpacePluginSerializer(many=True, source='space_set')

    @staticmethod
    def from_id(board_id: int):
        board = Board.objects.prefetch_related('space_set').get(pk=board_id)
        return BoardPluginSerializer(board).data
