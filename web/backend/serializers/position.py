from rest_framework import serializers

from backend.models.position import Position


class PositionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Position
        fields = ['x', 'y']
