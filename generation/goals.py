from copy import deepcopy
from random import Random
from typing import Dict, List
from xml.etree import ElementTree

from .difficulty import Difficulty


GOAL_XML = './goals.xml'
GOALS = {dif.value: [] for dif in Difficulty}


class Goal:
    """
    An abstract Goal "template" that contains variable ranges.
    """
    def __init__(self):
        self.description_template = ""
        self.tooltip_template = ""
        self.variable_ranges = {}  # type: Dict[str, tuple]

    def __str__(self):
        return self.description_template


class ConcreteGoal:
    """
    A goal on a board whose variables are set.
    """
    def __init__(self, goal: Goal, rand: Random):
        self.goal = goal
        self.variables = {}

        for k, (mini, maxi) in self.goal.variable_ranges.items():
            self.variables[k] = rand.randint(mini, maxi)

    def description(self) -> str:
        return self._replace_vars(self.goal.description_template)

    def tooltip(self) -> str:
        return self._replace_vars(self.goal.tooltip_template)

    def _replace_vars(self, inp: str) -> str:
        """
        Get a string with all $variables replaced with their actual values
        """
        for k, v in self.variables.items():
            inp = inp.replace(f'${k}', str(v))

        return inp

    def __str__(self):
        return self.description()


def get_goals(rand: Random, difficulty_counts: tuple) -> List[ConcreteGoal]:
    # Making a copy of our master Goal (template) list so that we can modify it,
    #  removing goals as we go to ensure no duplicates.
    goals_copy = deepcopy(GOALS)
    ret: List[ConcreteGoal] = []

    for diff, count in enumerate(difficulty_counts):
        for _ in range(count):
            # Pick a random goal at this difficulty
            this_difficulty_goals_list: List = goals_copy[diff]
            if len(this_difficulty_goals_list) == 0:
                raise IndexError(f"Ran out of goals of difficulty {diff}")
            goal_idx = rand.randint(0, len(this_difficulty_goals_list) - 1)

            # Append it to our returned list
            ret.append(ConcreteGoal(this_difficulty_goals_list[goal_idx], rand))

            # Remove the goal from the template list so it does not get duplicated
            this_difficulty_goals_list.pop(goal_idx)

    rand.shuffle(ret)

    return ret


def parse_xml(filename=GOAL_XML):
    etree = ElementTree.parse(filename)
    for e_goal in etree.getroot():
        new_goal = Goal()
        if e_goal.find('Description') is not None:
            new_goal.description_template = e_goal.find('Description').text
        if e_goal.find('Tooltip') is not None:
            new_goal.tooltip_template = e_goal.find('Tooltip').text

        for e_var in e_goal.findall('Variable'):
            name = e_var.get('name') or 'var'
            mini = int(e_var.get('min'))
            maxi = int(e_var.get('max'))
            new_goal.variable_ranges[name] = (mini, maxi)

        difficulty = int(e_goal.get('difficulty'))
        GOALS[difficulty].append(new_goal)
