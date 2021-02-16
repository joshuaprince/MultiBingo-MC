from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver

from backend.models.position import Position
from backend.models.space import Space
from generation import BoardGenerator


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


@receiver(post_save, sender=Board)
def build_board(instance: Board, created: bool, **kwargs):
    if created:
        # If the game code ends with a number, it specifies the game's difficulty.
        if instance.game_code[-1].isdigit():
            instance.difficulty = int(instance.game_code[-1])

        for i in range(25):
            pos = Position(x=(i % 5), y=(i // 5))
            pos.save()
            Space.objects.create(board=instance, position=pos)

        if instance.seed:
            instance.generate_goals()

        print(f"Created a new board with game code {instance.game_code}")
