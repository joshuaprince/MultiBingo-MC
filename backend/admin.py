from django.contrib import admin

from backend.models import Board, Square, PlayerBoard


@admin.register(Board)
class BoardAdmin(admin.ModelAdmin):
    pass


@admin.register(Square)
class SquareAdmin(admin.ModelAdmin):
    pass


@admin.register(PlayerBoard)
class PlayerBoardAdmin(admin.ModelAdmin):
    pass
