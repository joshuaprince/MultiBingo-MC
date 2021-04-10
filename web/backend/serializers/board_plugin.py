from rest_framework import serializers

from backend.models.board import Board
from backend.models.space import Space
from generation.goals import ConcreteGoal


# noinspection PyMethodMayBeStatic
class SpacePluginSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ['space_id', 'goal_id', 'text', 'type', 'variables']

    space_id = serializers.IntegerField(source='pk')
    goal_id = serializers.SerializerMethodField()
    text = serializers.SerializerMethodField()
    type = serializers.SerializerMethodField()
    variables = serializers.SerializerMethodField()

    def get_goal_id(self, obj):
        return ConcreteGoal.from_space(obj).template.id

    def get_text(self, obj):
        return ConcreteGoal.from_space(obj).description()

    def get_type(self, obj):
        return ConcreteGoal.from_space(obj).template.type

    def get_variables(self, obj):
        return ConcreteGoal.from_space(obj).variables


class BoardPluginSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = ['spaces']

    spaces = SpacePluginSerializer(many=True, source='space_set')

    @staticmethod
    def from_id(board_id: int):
        board = Board.objects.prefetch_related('space_set__setvariable_set').get(pk=board_id)
        return BoardPluginSerializer(board).data
