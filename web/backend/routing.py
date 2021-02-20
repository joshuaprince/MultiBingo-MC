from django.urls import path, re_path

from . import consumers, views

websocket_urlpatterns = [
    path(r'ws/board/<slug:game_code>/<player_name>', consumers.PlayerWebConsumer.as_asgi()),
    path(r'ws/board/<slug:game_code>', consumers.PlayerWebConsumer.as_asgi()),
    path(r'ws/board-plugin/<slug:game_code>', consumers.PluginBackendConsumer.as_asgi()),
]

urlpatterns = [
    path(r'rest/generate_board', views.GenerateBoardView.as_view())
]
