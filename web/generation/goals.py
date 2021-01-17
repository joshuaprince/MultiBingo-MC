import os
from copy import deepcopy
from random import Random
from typing import Dict, List, Optional, OrderedDict
from xml.etree import ElementTree
from xml.etree.ElementTree import Element

import xmltodict

GOAL_XML = os.path.join(os.path.dirname(__file__), 'goals.xml')
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
        self.type = "default"
        self.weight = 1.0
        self.antisynergy = None
        self.variable_ranges = {}  # type: Dict[str, tuple]
        self.is_autoactivated = False
        self.triggers_xml = []  # type: List[Element]

    def __str__(self):
        return f"({self.id}) {self.description_template}"


class ConcreteGoal:
    """
    A goal on a board whose variables are set.
    """
    def __init__(self, goal: Goal, rand: Optional[Random]):
        """
        :param goal: Goal that this ConcreteGoal references.
        :param rand: Random element used to set variables. If None, variables will not be
                     automatically set. This should only be used if the variables are being set
                     manually after creation of the ConcreteGoal (such as if being parsed from an
                     XML ID)
        """
        self.goal = goal
        self.variables = {}

        if rand:
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

    def triggers(self) -> List[dict]:
        triggers_list = []
        for goal_trigger_element in self.goal.triggers_xml:
            xml_str = ElementTree.tostring(goal_trigger_element, encoding='unicode')
            xml_str_derefd = self._replace_vars(xml_str)
            xml_dict = xmltodict.parse(xml_str_derefd, force_list=True)  # type: OrderedDict

            # Slight mangling to handle the fact that force_list above forces the root to be a list
            #  (even though there is only ever 1 root)
            root_key = list(xml_dict)[0]
            root_val = xml_dict[root_key][0]
            triggers_list.append({root_key: root_val})
        return triggers_list

    @staticmethod
    def from_xml_id(xml_id: str) -> 'ConcreteGoal':
        goal_id, vars_str = xml_id.split(':::')

        # Find goal in GOALS
        the_goal = None
        for dif in range(NUM_DIFFICULTIES):
            for goal in GOALS[dif]:
                if goal.id == goal_id:
                    the_goal = goal
        if the_goal is None:
            raise RuntimeError(f"Goal ID {goal_id} does not exist in the loaded XML.")

        cg = ConcreteGoal(the_goal, None)

        vars_from_xml = {}  # Map of (variable, value) in this XML definition
        if vars_str:
            for var_str in vars_str.split('::'):
                k, v = var_str.split(':')
                vars_from_xml[k] = v

        # By iterating twice, ensure that only variables associated with this Goal are in this CG
        for goal_var, _ in cg.goal.variable_ranges.items():
            cg.variables[goal_var] = vars_from_xml[goal_var]

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
    goal_ids = set()  # just used for ID duplication checking
    for e_goal in etree.getroot():
        gid = e_goal.get('id')
        if gid:
            if gid in goal_ids:
                raise ValueError(f"Duplicated goal ID {gid} in goal XML definition")
            else:
                goal_ids.add(gid)

        new_goal = Goal(gid)

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

        new_goal.triggers_xml = e_goal.findall('ItemTrigger')

        if len(e_goal.findall('ItemTrigger')) > 0 or len(e_goal.findall('Auto')) > 0:
            new_goal.is_autoactivated = True

        goal_type = e_goal.get('type')
        if goal_type:
            new_goal.type = goal_type

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
