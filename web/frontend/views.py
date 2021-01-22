import random
import string

from django.core.exceptions import ValidationError
from django.shortcuts import render

from web.backend.models import Board, PlayerBoard


def index(request):
    room_placeholder = ''.join(random.choice(string.ascii_uppercase) for _ in range(5))
    context = {
        'room_placeholder': room_placeholder
    }
    return render(request, 'bingo/index.html', context=context)


def board(request, game_code):
    player_name = request.GET.get('name', '')
    forced_goals = request.GET.getlist('force', [])

    board_obj, created = Board.objects.prefetch_related('square_set').get_or_create(
        game_code=game_code,
        defaults={'seed': game_code, 'forced_goals': ';'.join(forced_goals)}
    )
    if created:
        print(f"Created a new board with game code {game_code}")

    player_board_obj = None
    if player_name:
        try:
            player_board_obj, created = PlayerBoard.objects.get_or_create(
                board_id=board_obj.pk,
                player_name=player_name
            )
            if created:
                print(f"Created a new player board with game code {game_code}, player {player_name}")
        except ValidationError:
            print(f"Player name {player_name} raised ValidationError")
            player_name = ''

    squares = []
    for row in range(5):
        squares.append([])
        for col in range(5):
            pos = 5 * row + col
            squares[row].append(board_obj.square_set.get(position=pos))

    context = {
        'game_code': game_code,
        'player_id': player_board_obj.pk if player_board_obj else None,
        'player_name': player_name,
        'board': squares,
        'obscured': board_obj.obscured,
        'num_mark_colors': len(PlayerBoard.Marking)
    }
    return render(request, 'bingo/board.html', context=context)
