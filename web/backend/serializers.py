from rest_framework import serializers

from backend.models import Board
from generation.board_generator import generate_board


class GenerateBoardSerializer(serializers.ModelSerializer):
    class Meta:
        model = Board
        fields = ['game_code', 'shape', 'board_difficulty', 'seed', 'forced_goals']
        extra_kwargs = {'game_code': {'required': False}, 'shape': {'required': False}}

    board_difficulty = serializers.IntegerField(required=False, write_only=True)  # TODO max_value
    seed = serializers.CharField(required=False, write_only=True, max_length=256)
    forced_goals = serializers.ListField(required=False, write_only=True,
                                         child=serializers.CharField(max_length=256))

    def create(self, validated_data):
        return generate_board(**validated_data)

    def update(self, instance, validated_data):
        raise NotImplementedError("Cannot update a GenerateBoardSerializer.")
