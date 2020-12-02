import json

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer

from backend.models import PlayerBoard, Board


class BoardChangeConsumer(AsyncJsonWebsocketConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.game_code = None
        self.player_name = None
        self.board_id = None
        self.player_board_id = None

    async def connect(self):
        self.game_code = self.scope['url_route']['kwargs']['game_code']
        self.player_name = self.scope['url_route']['kwargs']['player_name']

        board_obj, player_board_obj = \
            await get_board_and_player_board(self.game_code, self.player_name)
        if not board_obj:
            print(f"Error getting board: {self.game_code} (player: {self.player_name})")
            return  # reject connection
        if not player_board_obj:
            print(f"Error getting Player board: {self.player_name} (game code: {self.game_code})")
            return  # reject connection

        self.board_id = board_obj.pk
        self.player_board_id = player_board_obj.pk

        await self.channel_layer.group_add(
            self.game_code,
            self.channel_name
        )

        await self.accept()
        await self.send_boards()

    async def receive(self, text_data: str = None, **kwargs):
        text_data_json = json.loads(text_data)
        pos = int(text_data_json['position'])
        to_state = int(text_data_json['to_state'])

        await mark_square(self.player_board_id, pos, to_state)
        await self.channel_layer.group_send(
            self.game_code, {
                'type': 'send_boards'
            }
        )

    async def disconnect(self, code):
        await self.channel_layer.group_discard(
            self.game_code,
            self.channel_name
        )

    async def send_boards(self, event=None):
        board_states = await get_board_states(self.board_id)
        await self.send(text_data=json.dumps(board_states))


@database_sync_to_async
def get_board_states(board_id: int):
    pboards = PlayerBoard.objects.filter(board_id=board_id)
    data = [{
        'player_name': pb.player_name,
        'board': pb.squares,
    } for pb in pboards]
    return data


@database_sync_to_async
def get_board_and_player_board(game_code: str, player: str) -> (Board, PlayerBoard):
    board = Board.objects.get(seed=game_code)
    if not board:
        return None, None

    player_board_obj, created = \
        PlayerBoard.objects.get_or_create(board_id=board.pk, player_name=player)
    if created:
        print(f"Created new PlayerBoard object for {player}")

    return board, player_board_obj


@database_sync_to_async
def mark_square(player_board_id: int, pos: int, to_state: int):
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
    player_board_obj.mark_square(pos, to_state)
    player_board_obj.save()
    print(f"Updated board for {player_board_obj.player_name}")


@database_sync_to_async
def mark_disconnected(player_board_id: int):
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
