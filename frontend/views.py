from django.shortcuts import render, get_object_or_404

from backend.models import Board


def index(request):
    return render(request, 'bingo/index.html')


def board(request, game_code):
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
        'board': squares
    }
    return render(request, 'bingo/board.html', context=context)
