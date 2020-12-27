from django.urls import path

from . import consumers

websocket_urlpatterns = [
    path(r'ws/board/<slug:game_code>/<player_name>/', consumers.PlayerWebConsumer.as_asgi()),
    path(r'ws/board/<slug:game_code>/', consumers.PlayerWebConsumer.as_asgi()),
    path(r'ws/board-plugin/<slug:game_code>/', consumers.PluginBackendConsumer.as_asgi()),
]
