import os
from copy import deepcopy
from random import Random
from typing import Dict, List, Optional, OrderedDict
from xml.etree import ElementTree
from xml.etree.ElementTree import Element

import xmltodict

GOAL_XML = os.path.join(os.path.dirname(__file__), 'goals.xml')
MAX_DIFFICULTY = 10
MAX_NEGATIVES = 3

GOAL_TYPE_NEGATIVE = 'negative'

GOALS: List['GoalTemplate'] = []


class GoalTemplate:
    """
    An abstract Goal "template" that contains unset variable ranges.
    Each GoalTemplate maps 1:1 with a <Goal> defined in `goals.xml`.
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
        self.triggers_xml = []  # type: List[Element]

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
                     manually after creation of the ConcreteGoal (such as if being parsed from an
                     XML ID)
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

    def xml_id(self) -> str:
        """
        XML ID contains the ID of the goal and serialized versions of any variables.
        Example: "stairs:::needed:3::othervar:5"
        """
        goal_id = self.template.id
        vars_str = '::'.join(':'.join([k, str(v)]) for k, v in self.variables.items())
        return ':::'.join([goal_id, vars_str])

    def triggers(self) -> List[dict]:
        triggers_list = []
        for goal_trigger_element in self.template.triggers_xml:
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
        for goal in GOALS:
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
        for goal_var, _ in cg.template.variable_ranges.items():
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


def get_goals(rand: Random, count: int, proportion_easy: float,
              forced_goals: List[str] = None) -> List[ConcreteGoal]:
    # Making a copy of our master Goal (template) list so that we can modify it,
    #  modifying goal weights as we go to ensure no duplicates.
    goals_copy = deepcopy(GOALS)

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

        new_goal = GoalTemplate(gid)

        difficulty = int(e_goal.get('difficulty'))
        if difficulty is None or difficulty > MAX_DIFFICULTY:
            raise ValueError(f"Goal {gid} does not have a difficulty between "
                             f"0 and {MAX_DIFFICULTY}")
        new_goal.difficulty = difficulty

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

        goal_type = e_goal.get('type')
        if goal_type:
            new_goal.type = goal_type

        GOALS.append(new_goal)


parse_xml(GOAL_XML)
