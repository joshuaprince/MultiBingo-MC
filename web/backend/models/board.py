from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver

from backend.models.position import Position
from backend.models.space import Space
from generation import BoardGenerator


class Board(models.Model):
    class Shape(models.TextChoices):
        SQUARE = 'square'
        HEXAGON = 'hexagon'

    game_code = models.SlugField(unique=True, max_length=128)
    shape = models.CharField(max_length=16, choices=Shape.choices)

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


@receiver(post_save, sender=Board)
def build_board(instance: Board, created: bool, **kwargs):
    if created:
        # If the game code ends with a number, it specifies the game's difficulty.
        if instance.game_code[-1].isdigit():
            instance.difficulty = int(instance.game_code[-1])

        if instance.shape == Board.Shape.HEXAGON:  # TODO
            spaces = [
                (1,0), (2,0), (3,0),
                (0,1), (1,1), (2,1), (3,1),
                (-1,2), (0,2), (1,2), (2,2), (3,2),
                (-1,3), (0,3), (1,3), (2,3),
                (-1,4), (0,4), (1,4)
            ]
            for sp in spaces:
                pos = Position(x=sp[0], y=sp[1])
                pos.save()
                Space.objects.create(board=instance, position=pos)
        else:
            for i in range(25):
                pos = Position(x=(i % 5), y=(i // 5))
                pos.save()
                Space.objects.create(board=instance, position=pos)

        if instance.seed:
            instance.generate_goals()

        print(f"Created a new board with game code {instance.game_code}")
