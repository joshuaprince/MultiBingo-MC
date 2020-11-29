import json

from channels.generic.websocket import WebsocketConsumer

from backend.models import PlayerBoard, Board


class BoardChangeConsumer(WebsocketConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.game_code = None
        self.board_obj = None

    def connect(self):
        self.game_code = self.scope['url_route']['kwargs']['game_code']
        self.board_obj = Board.objects.get(seed=self.game_code)

        self.accept()

    def receive(self, text_data: str = None, bytes_data=None):
        text_data_json = json.loads(text_data)
        player_name = text_data_json['player_name']
        pos = int(text_data_json['position'])
        to_state = int(text_data_json['to_state'])

        player_board_obj, created = \
            PlayerBoard.objects.get_or_create(board=self.board_obj, player_name=player_name)

        player_board_obj.mark_square(pos, to_state)
        player_board_obj.save()

        print(f"Updated board for {player_name}")

        self.send_board_states()

    def disconnect(self, code):
        pass

    def send_board_states(self):
        pboards = PlayerBoard.objects.filter(board=self.board_obj)
        data = [{
            'player_name': pb.player_name,
            'board': pb.squares,
        } for pb in pboards]

        self.send(text_data=json.dumps(data))
