#!/usr/bin/env python3

"""
This script reads in a goals.yml file and outputs a list of goals in an easier-to-read format.
"""
import argparse
import pathlib
from typing import List, Callable

from generation.goals import GOALS

_PLUGIN_TRIGGERED_GOAL_IDS = set()


def _load_plugin_triggers():
    # If cached already, do nothing
    if _PLUGIN_TRIGGERED_GOAL_IDS:
        return

    auto_goals_txt = pathlib.Path(__file__).parent.parent / 'kotlinplugin' / 'automated_goals.txt'

    if not auto_goals_txt.exists():
        raise FileNotFoundError("Assemble the plugin to generate automated_goals.txt")

    with open(auto_goals_txt) as f:
        for goal in f.readlines():
            _PLUGIN_TRIGGERED_GOAL_IDS.add(goal.strip())


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Read in a goals.Yml file and output them.')
    parser.add_argument('--not-auto', help='Only show goals that have not been automated',
                        action='store_true')
    parser.add_argument('--tooltip', help='Include tooltips', action='store_true')
    args = parser.parse_args()

    conditions = []  # type: List[Callable]

    try:
        _load_plugin_triggers()
    except FileNotFoundError as e:
        print(str(e))
        exit(1)

    if args.not_auto:
        conditions.append(lambda goal: goal.id not in _PLUGIN_TRIGGERED_GOAL_IDS)

    for g in GOALS.values():
        if any(not cond(g) for cond in conditions):
            continue

        desc_template = g.description_template
        weight = f" {{{g.weight}}}" if g.weight != 1 else ''
        auto = '[A]' if g.id in _PLUGIN_TRIGGERED_GOAL_IDS else '[ ]'
        tooltip = f'  ("{g.tooltip_template}")' if args.tooltip and g.tooltip_template else ''
        for varname, (mini, maxi) in g.variable_ranges.items():
            desc_template = desc_template.replace(f'${varname}', f'({mini}-{maxi})')

        print(f'[{g.difficulty}]{auto}\t{desc_template}{weight}{tooltip}')

    print(f"{len(GOALS)} total goals, {len(_PLUGIN_TRIGGERED_GOAL_IDS)} automated")
