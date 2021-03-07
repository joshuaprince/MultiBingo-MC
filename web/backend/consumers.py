import json
from abc import ABC, abstractmethod
from datetime import timedelta

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.db.models import Q
from django.utils import timezone

from backend.models import Board, BoardShape, PlayerBoard, Space
from generation.board_generator import generate_board


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
        await self.send_board_to_ws()
        await self.send_pboards_all_consumers()

    async def receive(self, text_data: str = None, **kwargs):
        broadcast_board = False
        broadcast_pboards = False
        text_data_json = json.loads(text_data)
        action = text_data_json.get('action')

        if action not in self.allowed_actions:
            print("WebSocket " + self.player_name + " attempted disallowed action " + action)
            return

        if action == 'board_mark' and self.player_board_id:
            space_id = int(text_data_json['space_id'])
            to_state = int(text_data_json['to_state'])
            await self.rx_mark_board(space_id, to_state)
            broadcast_pboards = True

        if action == 'board_mark_admin':
            space_id = int(text_data_json['space_id'])
            to_state = int(text_data_json['to_state'])
            player_name = text_data_json['player']
            broadcast_pboards = await self.rx_mark_board_admin(space_id, to_state, player_name)

        if action == 'reveal_board':
            revealed = await self.rx_reveal_board()
            if revealed:
                broadcast_board = True
                broadcast_pboards = True

        if broadcast_board:
            await self.send_board_all_consumers()

        if broadcast_pboards:
            await self.send_pboards_all_consumers()
        else:
            # only send to the client that sent this message (i.e. for a ping)
            await self.send_pboards_to_ws()

    async def disconnect(self, code):
        await self.channel_layer.group_discard(
            self.game_code,
            self.channel_name
        )

    async def rx_mark_board(self, space_id, to_state):
        await mark_space(self.player_board_id, space_id, to_state)
        await mark_disconnected(self.player_board_id, False)

    async def rx_mark_board_admin(self, space_id, to_state, player):
        return await mark_space_admin(self.board_id, player, space_id, to_state)

    async def rx_reveal_board(self):
        changed = await reveal_board(self.board_id)
        return changed

    async def send_board_all_consumers(self):
        await self.channel_layer.group_send(
            self.game_code, {
                'type': 'send_board_to_ws'
            }
        )

    @abstractmethod
    async def send_board_to_ws(self, event=None):
        # Board format depends on Consumer type. Override in child consumers.
        ...

    async def send_pboards_all_consumers(self):
        await self.channel_layer.group_send(
            self.game_code, {
                'type': 'send_pboards_to_ws'
            }
        )

    async def send_pboards_to_ws(self, event=None):
        board_states = await get_pboard_states(self.board_id)
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

        print(f"{self.player_name if self.player_name else 'Spectator'} joined game {self.game_code}.")

    async def disconnect(self, code):
        await super().disconnect(code)
        if self.player_board_id is not None:
            await mark_disconnected(self.player_board_id, True)
            await self.send_pboards_all_consumers()

        print(f"{self.player_name if self.player_name else 'Spectator'} disconnected from game {self.game_code}.")

    async def send_board_to_ws(self, event=None):
        board = await get_board_player(self.board_id)
        await self.send(text_data=json.dumps({
            'board': board,
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
        print(f"Plugin Backend joined game {self.game_code}.")

    async def disconnect(self, code):
        await super().disconnect(code)
        print(f"Plugin Backend disconnected from game {self.game_code}.")

    async def send_board_to_ws(self, event=None):
        board = await get_board_plugin(self.board_id)
        await self.send(text_data=json.dumps({
            'board': board,
        }))


@database_sync_to_async
def get_pboard_states(board_id: int):
    # Only sends board states of players who are not disconnected
    recent_dc_time = timezone.now() - timedelta(minutes=1)
    pboards = PlayerBoard.objects.filter(
        Q(disconnected_at=None) | Q(disconnected_at__gt=recent_dc_time),
        board_id=board_id
    )
    data = {
        'pboards': [pb.to_json() for pb in pboards],
    }
    return data


@database_sync_to_async
def get_board(game_code: str):
    if Board.objects.filter(game_code=game_code).exists():
        return Board.objects.get(game_code=game_code)
    else:
        return generate_board(game_code, BoardShape.SQUARE, board_difficulty=2)


@database_sync_to_async
def get_player_board(board_id: int, player: str):
    return PlayerBoard.objects.get_or_create(board_id=board_id, player_name=player)[0]


@database_sync_to_async
def reveal_board(board_id: str, revealed: bool = True):
    """
    Reveal the board for all players.
    :return: True if the board reveal state changed, False otherwise.
    """
    board = Board.objects.filter(pk=board_id).first()
    if not board:
        print("Tried to reveal nonexisting board ID: " + board_id)
        return

    new_obscured = not revealed
    if board.obscured != new_obscured:
        board.obscured = not revealed
        board.save()
        return True
    else:
        return False


@database_sync_to_async
def mark_space(player_board_id: int, space_id: int, to_state: int):
    """
    Mark a space on a player's board.
    :return: True if the board was changed, False otherwise.
    """
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
    return player_board_obj.mark_space(space_id, to_state)


@database_sync_to_async
def mark_space_admin(board_id: int, player_name: str, space_id: int, to_state: int):
    """
    Mark a space on a player's board.
    :return: True if the board was changed, False otherwise.
    """
    player_board_obj = PlayerBoard.objects.get_or_create(board_id=board_id, player_name=player_name)[0]
    return player_board_obj.mark_space(space_id, to_state)


@database_sync_to_async
def mark_disconnected(player_board_id: int, disconnected: bool):
    player_board_obj = PlayerBoard.objects.get(pk=player_board_id)
    player_board_obj.disconnected_at = timezone.now() if disconnected else None
    player_board_obj.save()


@database_sync_to_async
def get_board_player(board_id: int):
    board = Board.objects.get(pk=board_id)
    spaces = board.space_set.order_by('position')
    return {
        'obscured': board.obscured,
        'shape': board.shape,
        'spaces': [spc.to_player_json() for spc in spaces],
    }


@database_sync_to_async
def get_board_plugin(board_id: int):
    spaces = Space.objects.filter(board_id=board_id).order_by('position')
    return {
        'spaces': [spc.to_plugin_json() for spc in spaces],
    }
