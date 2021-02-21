from django.db import models


class Color(models.IntegerChoices):
    UNMARKED = 0
    COMPLETE = 1
    REVERTED = 2
    INVALIDATED = 3
    NOT_INVALIDATED = 4
