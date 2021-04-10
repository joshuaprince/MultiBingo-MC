from typing import Optional

from rest_framework import serializers

from backend.models.board import Board
from backend.models.player_board_marking import PlayerBoardMarking
from backend.models.space import Space
from backend.serializers.position import PositionSerializer
from generation.goals import ConcreteGoal


# noinspection PyMethodMayBeStatic
class SpacePlayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Space
        fields = ['space_id', 'position', 'text', 'tooltip', 'auto']

    space_id = serializers.IntegerField(source='pk')
    position = PositionSerializer()
    text = serializers.SerializerMethodField()
    tooltip = serializers.SerializerMethodField()
    auto = serializers.SerializerMethodField()

    def get_text(self, obj):
        return ConcreteGoal.from_space(obj).description()

    def get_tooltip(self, obj):
        return ConcreteGoal.from_space(obj).tooltip()

    def get_auto(self, obj):
        markings = self.context.get('player_board_markings')
        return obj.pk in (m.space_id for m in markings if m.auto_marker_client_id != '')


class BoardPlayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = ['obscured', 'shape', 'spaces']

    spaces = SpacePlayerSerializer(many=True, source='space_set')

    @staticmethod
    def from_id(board_id: int, player_board_id: Optional[int]):
        board = Board.objects.prefetch_related('space_set__position')\
            .prefetch_related('space_set__setvariable_set').get(pk=board_id)
        markings = PlayerBoardMarking.objects.filter(
            player_board__board_id=board_id, player_board_id=player_board_id
        )
        return BoardPlayerSerializer(board, context={'player_board_markings': markings}).data
