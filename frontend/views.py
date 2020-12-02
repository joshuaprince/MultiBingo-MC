import random
import string

from django.http import HttpResponseBadRequest
from django.shortcuts import render

from backend.models import Board


def index(request):
    room_placeholder = ''.join(random.choice(string.ascii_uppercase) for _ in range(5))
    context = {
        'room_placeholder': room_placeholder
    }
    return render(request, 'bingo/index.html', context=context)


def board(request, game_code):
    player_name = request.GET.get('name', '')
    if player_name and not player_name.isalnum():
        return HttpResponseBadRequest('Your name must only consist of letters and numbers.')

    board_obj, created = Board.objects.prefetch_related('square_set').get_or_create(seed=game_code)

    if created:
        print(f"Created a new board with game code {game_code}")

    squares = []
    for row in range(5):
        squares.append([])
        for col in range(5):
            pos = 5 * row + col
            squares[row].append(board_obj.square_set.get(position=pos))

    context = {
        'game_code': game_code,
        'player_name': player_name,
        'board': squares,
    }
    return render(request, 'bingo/board.html', context=context)
