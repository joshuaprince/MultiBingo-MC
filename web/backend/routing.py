from django.conf import settings
from django.http import HttpResponseRedirect
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

if settings.DEBUG:
    # Special redirection case - while running in development mode, we should redirect any attempts
    #  to access the webpage (/game) to the Frontend.
    def redirect(request, **kwargs):
        redir_path = '{scheme}://{host}{path}'.format(
            scheme=request.scheme,
            host=settings.FRONTEND_HOST,
            path=request.get_full_path(),
        )
        return HttpResponseRedirect(redir_path)
    urlpatterns += [
        re_path(r'^game/', redirect),
    ]
