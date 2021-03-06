"""
Views defined here are for the HTTP API, which does not provide the majority of functionality
for this app.

You probably want `<consumers.py>`_ instead!
"""
from rest_framework.generics import CreateAPIView

from backend.models import Board
from backend.serializers import GenerateBoardSerializer


class GenerateBoardView(CreateAPIView):
    queryset = Board.objects.all()
    serializer_class = GenerateBoardSerializer