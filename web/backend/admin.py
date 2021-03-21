from django.contrib import admin

from backend.models.board import Board
from backend.models.space import Space
from backend.models.player_board import PlayerBoard


@admin.register(Board)
class BoardAdmin(admin.ModelAdmin):
    pass


@admin.register(Space)
class SpaceAdmin(admin.ModelAdmin):
    pass


@admin.register(PlayerBoard)
class PlayerBoardAdmin(admin.ModelAdmin):
    pass
