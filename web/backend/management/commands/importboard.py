import json
from django.core.management import BaseCommand

from backend.models.board import Board


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument('game_code', nargs=1, type=str)
        parser.add_argument('json_file', nargs=1, type=str)

    def handle(self, *args, **options):
        game_code = options['game_code'][0]
        json_file = options['json_file'][0]

        with open(json_file, 'r') as fp:
            js = json.load(fp)

        print(js)

        board = Board.objects.create(game_code=game_code, seed='', difficulty=2)
        spaces = board.space_set.order_by('position')
        for pos, goal in enumerate(js):
            print(pos)
            spc = spaces[pos]
            spc.text = goal['name']
            spc.tooltip = goal['tooltip']
            spc.save()
