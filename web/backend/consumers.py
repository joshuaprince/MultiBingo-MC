import json
from abc import ABC
from datetime import timedelta
from typing import Optional

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.db.models import Q
from django.utils import timezone

from web.backend.models import PlayerBoard, Board, Square


class BaseWebConsumer(AsyncJsonWebsocketConsumer, ABC):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.allowed_actions = []
        self.game_code = None
        self.board_id = None

        # Optional - only set if this is representative of a single Player and not a Spectator
        self.player_name = None  # Remains None if this represents a Spectator
        self.player_board_id = None  # Remains None if this represents a Spectator

    async def connect(self):
        self.game_code = self.scope['url_route']['kwargs']['game_code']

        board_obj = await get_board(self.game_code)
        if not board_obj:
            print(f"Error getting board: {self.game_code} (player: {self.player_name})")
            return  # reject connection
        self.board_id = board_obj.pk

        await self.channel_layer.group_add(self.game_code, self.channel_name)
        await self.accept()
        await self.send_boards_all_consumers()

    async def receive(self, text_data: str = None, **kwargs):
        broadcast_boards = False
        text_data_json = json.loads(text_data)
        action = text_data_json.get('action')

        if action not in self.allowed_actions:
            print("WebSocket " + self.player_name + " attempted disallowed action " + action)
            return

        if action == 'board_mark' and self.player_board_id:
            pos = int(text_data_json['position'])
            to_state = int(text_data_json['to_state'])
            await self.rx_mark_board(pos, to_state)
            broadcast_boards = True

        if action == 'board_mark_admin':
            pos = int(text_data_json['position'])
            to_state = int(text_data_json['to_state'])
            player_name = text_data_json['player']
            broadcast_boards = await self.rx_mark_board_admin(pos, to_state, player_name)

        if action == 'reveal_board':
            await self.rx_reveal_board()
            broadcast_boards = True

        if broadcast_boards:
            await self.send_boards_all_consumers()
        else:
            # only send to the client that sent this message (i.e. for a ping)
            await self.send_boards_to_ws()

    async def disconnect(self, code):
        await self.channel_layer.group_discard(
            self.game_code,
            self.channel_name
        )

    async def rx_mark_board(self, pos, to_state):
        await mark_square(self.player_board_id, pos, to_state)
        await mark_disconnected(self.player_board_id, False)

    async def rx_mark_board_admin(self, pos, to_state, player):
        return await mark_square_admin(self.board_id, player, pos, to_state)

    async def rx_reveal_board(self):
        await reveal_board(self.board_id)

    async def send_boards_all_consumers(self):
        await self.channel_layer.group_send(
            self.game_code, {
                'type': 'send_boards_to_ws'
            }
        )

    async def send_boards_to_ws(self, event=None):
        board_states = await get_board_states(self.board_id)
        await self.send(text_data=json.dumps(board_states))


class PlayerWebConsumer(BaseWebConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.allowed_actions = [
            'board_mark',
            'reveal_board'
        ]

    async def connect(self):
        await super().connect()
        self.player_name = self.scope['url_route']['kwargs'].get('player_name')

        if self.player_name:
            player_board_obj = await get_player_board(self.board_id, self.player_name)
            self.player_board_id = player_board_obj.pk
            await mark_disconnected(self.player_board_id, False)

        await self.send_board_defs_to_ws()
        print(f"{self.player_name if self.player_name else 'Spectator'} joined game {self.game_code}.")

    async def disconnect(self, code):
        await super().disconnect(code)
        if self.player_board_id is not None:
            await mark_disconnected(self.player_board_id, True)
            await self.send_boards_all_consumers()

        print(f"{self.player_name if self.player_name else 'Spectator'} disconnected from game {self.game_code}.")

    async def send_board_defs_to_ws(self):
        board_goals = await get_player_goals(self.board_id)
        await self.send(text_data=json.dumps({
            'squares': board_goals
        }))


class PluginBackendConsumer(BaseWebConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.allowed_actions = [
            'board_mark_admin',
            'reveal_board'
        ]

    async def connect(self):
        await super().connect()
        await self.send_board_xml_to_ws()
        print(f"Plugin Backend joined game {self.game_code}.")

    async def disconnect(self, code):
        await super().disconnect(code)
        print(f"Plugin Backend disconnected from game {self.game_code}.")

    async def send_board_xml_to_ws(self):
        board_goals = await get_plugin_goals(self.board_id)
        await self.send(text_data=json.dumps({
            'squares': board_goals
        }))


@database_sync_to_async
def get_board_states(board_id: int):
    # Only sends board states of players who are not disconnected
    recent_dc_time = timezone.now() - timedelta(minutes=1)
    board_obj = Board.objects.filter(pk=board_id).first()
    pboards = PlayerBoard.objects.filter(
        Q(disconnected_at=None) | Q(disconnected_at__gt=recent_dc_time),
        board_id=board_id
    )
    data = {
        'obscured': board_obj.obscured,
        'pboards': [pb.to_json() for pb in pboards],
    }
    return data


@database_sync_to_async
def get_board(game_code: str):
    return Board.objects.get_or_create(game_code=game_code, defaults={'seed': game_code})[0]


@database_sync_to_async
def get_player_board(board_id: int, player: str):
    return PlayerBoard.objects.get_or_create(board_id=board_id, player_name=player)[0]


@database_sync_to_async
def reveal_board(board_id: str, revealed: bool = True):
    board = Board.objects.filter(pk=board_id).first()
    if not board:
        print("Tried to reveal nonexisting board ID: " + board_id)
        return

    board.obscured = not revealed
    board.save()


@database_sync_to_async
def mark_square(player_board_id: int, pos: int, to_state: int):
    """
    Mark a square on a player's board.
    :return: True if the board was changed, False otherwise.
    """
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
    return player_board_obj.mark_square(pos, to_state)


@database_sync_to_async
def mark_square_admin(board_id: int, player_name: str, pos: int, to_state: int):
    """
    Mark a square on a player's board.
    :return: True if the board was changed, False otherwise.
    """
    player_board_obj = PlayerBoard.objects.get(board_id=board_id, player_name=player_name)
    return player_board_obj.mark_square(pos, to_state)


@database_sync_to_async
def mark_disconnected(player_board_id: int, disconnected: bool):
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
    player_board_obj.disconnected_at = timezone.now() if disconnected else None
    player_board_obj.save()


@database_sync_to_async
def get_player_goals(board_id: int):
    squares = Square.objects.filter(board_id=board_id).order_by('position')
    return [sq.to_player_json() for sq in squares]


@database_sync_to_async
def get_plugin_goals(board_id: int):
    squares = Square.objects.filter(board_id=board_id).order_by('position')
    return [sq.to_plugin_json() for sq in squares]
