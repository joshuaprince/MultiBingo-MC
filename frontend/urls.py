from django.urls import path

from . import views

urlpatterns = [
    path('game/<slug:game_code>/', views.board, name='board'),
    path('', views.index, name='index'),
]
