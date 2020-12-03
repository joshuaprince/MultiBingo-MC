from django.urls import path

from . import consumers

websocket_urlpatterns = [
    path(r'ws/board/<slug:game_code>/<player_name>/', consumers.BoardChangeConsumer.as_asgi()),
    path(r'ws/board/<slug:game_code>/', consumers.BoardChangeConsumer.as_asgi()),
]
