from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver

from generation import BoardGenerator
from generation.goals import ConcreteGoal


class Board(models.Model):
    game_code = models.SlugField(unique=True, max_length=128)

    seed = models.SlugField(max_length=128)
    """If blank, goals will not be auto-generated on this board."""

    difficulty = models.IntegerField(default=2)
    forced_goals = models.CharField(default='', max_length=256)  # Semicolon-delimited

    obscured = models.BooleanField(default=True)

    def __str__(self):
        return self.game_code

    def generate_goals(self):
        """
        Populate all spaces with goals based on the seed.
        :return:
        """
        gen = BoardGenerator(self.difficulty, self.seed or None,
                             forced_goals=self.forced_goals.split(';'))
        goals = gen.generate()  # TODO 25 goals always

        spaces = list(self.space_set.order_by('position'))

        for i, space in enumerate(spaces):
            goal = goals[i]
            space.text = goal.description()
            space.tooltip = goal.tooltip()
            space.xml_id = goal.xml_id()
            space.save()


class Position(models.Model):
    x = models.IntegerField()
    y = models.IntegerField()
    z = models.IntegerField(default=0)

    def __str__(self):
        return str((self.x, self.y, self.z))

    def to_json(self):
        return {
            'x': self.x,
            'y': self.y,
            'z': self.z,
        }

    class Meta:
        ordering = ['y', 'x', 'z']


class Space(models.Model):
    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    position = models.OneToOneField(Position, on_delete=models.CASCADE)

    text = models.CharField(max_length=256)
    tooltip = models.CharField(max_length=512)

    xml_id = models.SlugField(max_length=256)
    """`id` field of the goal from XML. Should only be used to pull trigger data."""

    def __str__(self):
        return str(self.board) + " @" + str(self.position)

    class Meta:
        ...
        # unique_together = ['board']
        # TODO uniqueness validation for spaces

    def is_autoactivated(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        return cg.template.is_autoactivated

    def initial_state(self) -> 'PlayerBoard.Marking':
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        if cg.template.type == 'negative':
            return PlayerBoard.Marking.NOT_INVALIDATED
        else:
            return PlayerBoard.Marking.UNMARKED

    def to_player_json(self):
        return {
            'space_id': self.pk,
            'position': self.position.to_json(),
            'text': self.text,
            'tooltip': self.tooltip,
            'auto': self.is_autoactivated(),
        }

    def to_plugin_json(self):
        cg = ConcreteGoal.from_xml_id(self.xml_id)
        return {
            'space_id': self.pk,
            'goal_id': cg.template.id,
            'text': self.text,
            'type': cg.template.type,
            'variables': cg.variables,
            'triggers': cg.triggers(),
        }


@receiver(post_save, sender=Board)
def build_board(instance: Board, created: bool, **kwargs):
    if created:
        # If the game code ends with a number, it specifies the game's difficulty.
        if instance.game_code[-1].isdigit():
            instance.difficulty = int(instance.game_code[-1])

        for i in range(25):
            pos = Position(x=(i % 5), y=(i // 5), z=0)
            pos.save()
            Space.objects.create(board=instance, position=pos)

        if instance.seed:
            instance.generate_goals()

        print(f"Created a new board with game code {instance.game_code}")


class PlayerBoard(models.Model):
    """
    A player's set of markings on a particular game board.
    """
    class Marking(models.IntegerChoices):
        UNMARKED = 0
        COMPLETE = 1
        REVERTED = 2
        INVALIDATED = 3
        NOT_INVALIDATED = 4

    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    player_name = models.CharField(blank=False, max_length=128)
    markings = models.ManyToManyField(Space, through='PlayerBoardMarking')
    disconnected_at = models.DateTimeField(null=True)

    def __str__(self):
        return str(self.board) + " : " + self.player_name

    class Meta:
        unique_together = ['board', 'player_name']

    def mark_space(self, space_id: int, to_state: Marking):
        """
        Mark a space on this player's board to a specified state.
        :return: True if the board was changed, False otherwise.
        """
        marking = self.playerboardmarking_set.get(space_id=space_id)
        if marking.color != to_state:
            marking.color = to_state
            marking.save()
            return True
        else:
            return False

    def to_json(self):
        return {
            'player_id': self.pk,
            'player_name': self.player_name,
            'markings': [mark.to_json() for mark in self.playerboardmarking_set.all()],
            'disconnected_at': self.disconnected_at.isoformat() if self.disconnected_at else None,
        }


@receiver(post_save, sender=PlayerBoard)
def build_player_board(instance: PlayerBoard, created: bool, **kwargs):
    if created:
        print(f"Created a new player board with game code {instance.board.game_code}, "
              f"player {instance.player_name}")

        for space in instance.board.space_set.order_by('position').all():
            PlayerBoardMarking.objects.create(
                space=space,
                player_board=instance,
                color=space.initial_state(),
            )


class PlayerBoardMarking(models.Model):
    space = models.ForeignKey(Space, on_delete=models.CASCADE)
    player_board = models.ForeignKey(PlayerBoard, on_delete=models.CASCADE)
    color = models.IntegerField()

    def to_json(self):
        return {
            'space_id': self.space.pk,
            # 'position': self.space.position.to_json(),
            'color': self.color,
        }
