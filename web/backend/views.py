"""
Views defined here are for the HTTP API, which does not provide the majority of functionality
for this app.

You probably want `<consumers.py>`_ instead!
"""
from rest_framework.generics import CreateAPIView

from backend.serializers import GenerateBoardSerializer


class GenerateBoardView(CreateAPIView):
    serializer_class = GenerateBoardSerializer
