from datetime import timedelta
from typing import Optional

from django.db.models import Q
from django.utils import timezone
from rest_framework import serializers

from backend.models.player_board import PlayerBoard
from backend.models.player_board_marking import PlayerBoardMarking
from win_detection.win_detection import winning_space_ids


class PlayerBoardMarkingSerializer(serializers.ModelSerializer):
    class Meta:
        model = PlayerBoardMarking
        fields = ['space_id', 'color', 'covert_marked']

    covert_marked = serializers.SerializerMethodField()

    def get_covert_marked(self, obj: PlayerBoardMarking):
        if obj.player_board_id == self.context.get('for_player_pboard_id'):
            return obj.covert_marked
        else:
            return False


class PlayerBoardSerializer(serializers.ModelSerializer):
    class Meta:
        model = PlayerBoard
        fields = ['player_id', 'player_name', 'markings', 'win', 'disconnected_at']

    player_id = serializers.IntegerField(source='pk')
    markings = PlayerBoardMarkingSerializer(many=True, source='playerboardmarking_set')
    win = serializers.SerializerMethodField()

    def get_win(self, obj: PlayerBoard):
        return winning_space_ids(obj)

    @staticmethod
    def from_board_id(board_id: int, for_player_pboard_id: Optional[int] = None):
        # Only collects board states of players who are not disconnected
        recent_dc_time = timezone.now() - timedelta(minutes=1)

        pboards = PlayerBoard.objects.select_related('board').prefetch_related('playerboardmarking_set').filter(
            Q(disconnected_at=None) | Q(disconnected_at__gt=recent_dc_time),
            board_id=board_id
        ).order_by('pk')

        return {
            'pboards': PlayerBoardSerializer(pboards, many=True, context={
                'for_player_pboard_id': for_player_pboard_id
            }).data
        }
