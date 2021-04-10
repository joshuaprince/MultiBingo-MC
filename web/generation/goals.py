import os
from copy import deepcopy
from random import Random
from typing import Dict, List, Optional, TYPE_CHECKING

import yaml

if TYPE_CHECKING:
    from backend.models.space import Space


GOAL_YML = os.path.join(os.path.dirname(__file__), 'goals.yml')
MAX_DIFFICULTY = 10
MAX_NEGATIVES = 3

GOAL_TYPE_NEGATIVE = 'negative'

GOALS: Dict[str, 'GoalTemplate'] = {}


class GoalTemplate:
    """
    An abstract Goal "template" that contains unset variable ranges.
    Each GoalTemplate maps 1:1 with a <Goal> defined in `goals.yml`.
    """
    def __init__(self, id: str):
        self.id = id
        self.difficulty = None
        self.description_template = ""
        self.tooltip_template = ""
        self.type = "default"
        self.weight = 1.0
        self.antisynergy = None
        self.variable_ranges = {}  # type: Dict[str, tuple]

    def __str__(self):
        return f"({self.id}) {self.description_template}"


class ConcreteGoal:
    """
    A goal on a board whose variables are set.
    """
    def __init__(self, template: GoalTemplate, rand: Optional[Random]):
        """
        :param template: Goal that this ConcreteGoal references.
        :param rand: Random element used to set variables. If None, variables will not be
                     automatically set. This should only be used if the variables are being set
                     manually after creation of the ConcreteGoal (such as if being parsed from the
                     database)
        """
        self.template = template
        self.variables = {}

        if rand:
            for k, (mini, maxi) in self.template.variable_ranges.items():
                self.variables[k] = rand.randint(mini, maxi)

    def description(self) -> str:
        return self._replace_vars(self.template.description_template)

    def tooltip(self) -> str:
        return self._replace_vars(self.template.tooltip_template)

    @staticmethod
    def from_space(space: 'Space') -> 'ConcreteGoal':
        # Find goal in GOALS
        the_goal = GOALS.get(space.goal_id)
        if the_goal is None:
            raise RuntimeError(f"Goal ID {space.goal_id} does not exist in the loaded YML.")

        cg = ConcreteGoal(the_goal, None)

        for var in space.setvariable_set.all():
            cg.variables[var.name] = var.value

        return cg

    def _replace_vars(self, inp: str) -> str:
        """
        Get a string with all $variables replaced with their actual values
        """
        for k, v in self.variables.items():
            inp = inp.replace(f'${k}', str(v))

        return inp

    def __str__(self):
        return self.description()


def get_goals(rand: Random, count: int, proportion_easy: float,
              forced_goals: List[str] = None) -> List[ConcreteGoal]:
    # Making a copy of our master Goal (template) list so that we can modify it,
    #  modifying goal weights as we go to ensure no duplicates.
    goals_copy = deepcopy(GOALS).values()

    ret: List[ConcreteGoal] = []
    count_by_difficulty = [0, 0]
    negatives = 0

    def pick(goal_: GoalTemplate):
        nonlocal goals_copy, ret, negatives
        count_by_difficulty[goal_.difficulty] += 1
        ret.append(ConcreteGoal(goal_, rand))
        # Remove the goal and its antisynergies from the template list so none come up again
        if goal_.antisynergy:
            for g in (g for g in goals_copy if g.antisynergy == goal_.antisynergy):
                g.weight = 0
        else:
            goal_.weight = 0
        # Ensure that a limited number of Negative goals can appear on one board
        if goal_.type == GOAL_TYPE_NEGATIVE:
            negatives += 1
            if negatives >= MAX_NEGATIVES:
                for g in (g for g in goals_copy if g.type == GOAL_TYPE_NEGATIVE):
                    g.weight = 0

    # Add forced goals to the list
    for fg_id in (forced_goals or ()):
        try:
            goal = next(g for g in goals_copy if g.id == fg_id)
        except StopIteration:
            print(f"Cannot force unknown goal ID {fg_id}")
            continue
        pick(goal)

    # Randomize the rest of the list
    while len(ret) < count:
        # Decide whether to pick an easy goal next by comparing the current proportion of easy
        #  goals to the targeted proportion
        try:
            curr_proportion_easy = float(count_by_difficulty[0]) / sum(count_by_difficulty)
        except ZeroDivisionError:
            curr_proportion_easy = 0
        difficulty = 0 if curr_proportion_easy < proportion_easy else 1

        goals_this_difficulty = list(g for g in goals_copy if g.difficulty == difficulty)
        if len(goals_this_difficulty) == 0:
            raise IndexError(f"Ran out of goals of difficulty {difficulty}")

        # Pick a random goal at this difficulty
        weights = [goal.weight for goal in goals_this_difficulty]
        goal_idx = rand.choices(range(len(goals_this_difficulty)), weights=weights)[0]
        goal = goals_this_difficulty[goal_idx]
        pick(goal)

    rand.shuffle(ret)

    return ret


def parse_yml(filename=GOAL_YML):
    with open(filename) as yml_file:
        yml = yaml.safe_load(yml_file)

    for goal_id, goal_dict in yml.get('goals').items():
        new_goal = GoalTemplate(goal_id)
        difficulty = goal_dict.get('difficulty')
        if difficulty is None or difficulty > MAX_DIFFICULTY:
            raise yaml.YAMLError(f"Goal {goal_id} does not have a difficulty between "
                                 f"0 and {MAX_DIFFICULTY}")
        new_goal.difficulty = difficulty

        new_goal.description_template = goal_dict.get('text')
        if not new_goal.description_template:
            raise yaml.YAMLError(f"Goal {goal_id} does not have a `text` (description) tag.")

        tooltip = goal_dict.get('tooltip')
        if tooltip:
            new_goal.tooltip_template = tooltip

        weight = goal_dict.get('weight')
        if weight:
            new_goal.weight = weight

        antisynergy = goal_dict.get('antisynergy')
        if antisynergy:
            new_goal.antisynergy = antisynergy

        goal_type = goal_dict.get('type')
        if goal_type:
            new_goal.type = goal_type

        for variable, range_str in \
                ((k, v) for (k, v) in goal_dict.items() if k.startswith('var')):
            try:
                mini, maxi = range_str.split('..')
                new_goal.variable_ranges[variable] = (int(mini), int(maxi))
            except (ValueError, KeyError):
                raise yaml.YAMLError(f"{goal_id}: Invalid variable {variable} = {range_str}.")

        GOALS[new_goal.id] = new_goal


parse_yml(GOAL_YML)
