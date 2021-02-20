from django.contrib import admin

from backend.models import Board, Space, PlayerBoard


@admin.register(Board)
class BoardAdmin(admin.ModelAdmin):
    pass


@admin.register(Space)
class SpaceAdmin(admin.ModelAdmin):
    pass


@admin.register(PlayerBoard)
class PlayerBoardAdmin(admin.ModelAdmin):
    pass
