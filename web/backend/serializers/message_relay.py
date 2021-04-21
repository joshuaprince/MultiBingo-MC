from rest_framework import serializers


class MessageRelaySerializer(serializers.Serializer):
    sender = serializers.CharField(max_length=1024)
    json = serializers.JSONField(required=False)

    def update(self, instance, validated_data):
        raise NotImplementedError("Cannot update a MessageSerializer.")

    def create(self, validated_data):
        raise NotImplementedError("Cannot create a MessageSerializer.")
