from django.contrib import admin

from backend.models import board, Space, PlayerBoard


@admin.register(board)
class BoardAdmin(admin.ModelAdmin):
    pass


@admin.register(Space)
class SpaceAdmin(admin.ModelAdmin):
    pass


@admin.register(PlayerBoard)
class PlayerBoardAdmin(admin.ModelAdmin):
    pass
