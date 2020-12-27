from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver

from generation import BoardGenerator


class Board(models.Model):
    game_code = models.SlugField(unique=True, max_length=128)

    seed = models.SlugField(max_length=128)
    """If blank, goals will not be auto-generated on this board."""

    difficulty = models.IntegerField()

    obscured = models.BooleanField(default=True)

    def __str__(self):
        return self.game_code

    def generate_goals(self):
        """
        Populate all squares with goals based on the seed.
        :return:
        """
        gen = BoardGenerator(self.difficulty, self.seed or None)
        goals = gen.generate()

        for pos, goal in enumerate(goals):
            sq = self.square_set.get(position=pos)  # type: Square
            sq.text = goal.description()
            sq.tooltip = goal.tooltip()
            sq.xml_id = goal.xml_id()
            sq.save()


class Square(models.Model):
    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    position = models.IntegerField()

    text = models.CharField(max_length=256)
    tooltip = models.CharField(max_length=512)

    xml_id = models.SlugField(max_length=256)
    """`id` field of the goal from XML. Should only be used to pull trigger data."""

    def __str__(self):
        return str(self.board) + " #" + str(self.position)

    class Meta:
        unique_together = ['board', 'position']


@receiver(post_save, sender=Board)
def build_board(instance: Board, created: bool, **kwargs):
    if created:
        for i in range(25):
            Square.objects.create(board=instance, position=i)

        if instance.seed:
            instance.generate_goals()


class PlayerBoard(models.Model):
    """
    A player's set of markings on a particular game board.
    """
    class Marking(models.IntegerChoices):
        UNMARKED = 0
        MARKED_GREEN = 1
        MARKED_BLUE = 2
        MARKED_RED = 3

    board = models.ForeignKey(Board, on_delete=models.CASCADE)
    player_name = models.CharField(blank=False, max_length=128)
    squares = models.CharField(max_length=25, default=('0' * 25))
    disconnected_at = models.DateTimeField(null=True)

    def __str__(self):
        return str(self.board) + " : " + self.player_name

    class Meta:
        unique_together = ['board', 'player_name']

    def mark_square(self, pos, to_state: Marking):
        self.squares = \
            self.squares[:pos] + \
            str(to_state) + \
            self.squares[pos+1:]

    def to_json(self):
        return {
            'player_id': self.pk,
            'player_name': self.player_name,
            'board': self.squares,
            'disconnected_at': self.disconnected_at.isoformat() if self.disconnected_at else None,
        }
