import json

from channels.db import database_sync_to_async as db_acc
from channels.generic.websocket import AsyncJsonWebsocketConsumer

from backend.models import PlayerBoard, Board


class BoardChangeConsumer(AsyncJsonWebsocketConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.game_code = None
        self.board_obj = None

    async def connect(self):
        self.game_code = self.scope['url_route']['kwargs']['game_code']
        self.board_obj = await db_acc(Board.objects.get)(seed=self.game_code)

        await self.accept()
        board_states = await self.get_board_states()
        await self.send(text_data=json.dumps(board_states))

    async def receive(self, text_data: str = None, **kwargs):
        text_data_json = json.loads(text_data)
        player_name = text_data_json['player_name']
        pos = int(text_data_json['position'])
        to_state = int(text_data_json['to_state'])

        await self.mark_square(player_name, pos, to_state)

        board_states = await self.get_board_states()
        await self.send(text_data=json.dumps(board_states))

    async def disconnect(self, code):
        pass

    @db_acc
    def get_board_states(self):
        pboards = PlayerBoard.objects.filter(board=self.board_obj)
        data = [{
            'player_name': pb.player_name,
            'board': pb.squares,
        } for pb in pboards]
        return data

    @db_acc
    def mark_square(self, player: str, pos: int, to_state: int):
        player_board_obj, created = \
            PlayerBoard.objects.get_or_create(board=self.board_obj, player_name=player)
        player_board_obj.mark_square(pos, to_state)
        player_board_obj.save()
        print(f"Updated board for {player}")

