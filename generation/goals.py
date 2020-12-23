from copy import deepcopy
from random import Random
from typing import Dict, List
from xml.etree import ElementTree

GOAL_XML = 'generation/goals.xml'
NUM_DIFFICULTIES = 5
GOALS: Dict[int, List['Goal']] = {i: [] for i in range(NUM_DIFFICULTIES)}


class Goal:
    """
    An abstract Goal "template" that contains variable ranges.
    """
    def __init__(self, id: str):
        self.id = id
        self.description_template = ""
        self.tooltip_template = ""
        self.weight = 1.0
        self.antisynergy = None
        self.variable_ranges = {}  # type: Dict[str, tuple]

    def __str__(self):
        return f"({self.id}) {self.description_template}"


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

    def xml_id(self) -> str:
        """
        XML ID contains the ID of the goal and serialized versions of any variables.
        Example: "stairs:::needed:3::othervar:5"
        """
        goal_id = self.goal.id
        vars_str = '::'.join(':'.join([k, str(v)]) for k, v in self.variables.items())
        return ':::'.join([goal_id, vars_str])

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
            this_difficulty_goals_list: List = goals_copy[diff]
            if len(this_difficulty_goals_list) == 0:
                raise IndexError(f"Ran out of goals of difficulty {diff}")

            # Pick a random goal at this difficulty
            weights = [goal.weight for goal in this_difficulty_goals_list]
            goal_idx = rand.choices(range(len(this_difficulty_goals_list)), weights=weights)[0]
            goal = this_difficulty_goals_list[goal_idx]

            # Append it to our returned list
            ret.append(ConcreteGoal(goal, rand))

            # Remove the goal and its antisynergies from the template list so none come up again
            if goal.antisynergy:
                for df in goals_copy.keys():
                    goals_copy[df] = [g for g in goals_copy[df] if g.antisynergy != goal.antisynergy]
            else:
                this_difficulty_goals_list.pop(goal_idx)

    rand.shuffle(ret)

    return ret


def parse_xml(filename=GOAL_XML):
    etree = ElementTree.parse(filename)
    for e_goal in etree.getroot():
        new_goal = Goal(e_goal.get('id'))
        if e_goal.find('Description') is not None:
            new_goal.description_template = e_goal.find('Description').text
        if e_goal.find('Tooltip') is not None:
            new_goal.tooltip_template = e_goal.find('Tooltip').text
        if e_goal.find('Weight') is not None:
            new_goal.weight = float(e_goal.find('Weight').text)
        if e_goal.find('Antisynergy') is not None:
            new_goal.antisynergy = e_goal.find('Antisynergy').text

        for e_var in e_goal.findall('Variable'):
            name = e_var.get('name') or 'var'
            mini = int(e_var.get('min'))
            maxi = int(e_var.get('max'))
            new_goal.variable_ranges[name] = (mini, maxi)

        difficulty = int(e_goal.get('difficulty'))
        GOALS[difficulty].append(new_goal)

    # _add_placeholder_goals()  # TODO remove


def _add_placeholder_goals():
    """
    Add goals that will placehold until there are 25 of each difficulty
    """
    for dif in range(NUM_DIFFICULTIES):
        for i in range(25):
            new_goal = Goal(f'placeholder_dif{dif}_{i}')
            new_goal.description_template = f'D{dif} Placeholder goal {i}'
            GOALS[dif].append(new_goal)


parse_xml(GOAL_XML)
