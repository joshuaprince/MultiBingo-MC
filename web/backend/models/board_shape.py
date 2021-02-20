from django.db import models


class BoardShape(models.TextChoices):
    SQUARE = 'square'
    HEXAGON = 'hexagon'
