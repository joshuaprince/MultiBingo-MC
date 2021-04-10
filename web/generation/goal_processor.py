#!/usr/bin/env python3

"""
This script reads in a goals.yml file and outputs a list of goals in an easier-to-read format.
"""
import argparse
import pathlib

from goals import GOALS


def _has_plugin_trigger(goal):
    base_plugin_src = (
        pathlib.Path(__file__).parent.parent.parent
        / 'plugin' / 'src' / 'main' / 'java' / 'com' / 'jtprince' / 'bingo' / 'plugin'
    )

    # TODO: Finish this
    return False


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Read in a goals.Yml file and output them.')
    parser.add_argument('--not-auto', help='Only show goals that have not been automated',
                        action='store_true')
    args = parser.parse_args()

    if args.not_auto:
        # TODO
        pass

    for g in GOALS.values():
        desc_template = g.description_template
        weight = f" {{{g.weight}}}" if g.weight != 1 else ''
        for varname, (mini, maxi) in g.variable_ranges.items():
            desc_template = desc_template.replace(f'${varname}', f'({mini}-{maxi})')
        print(f'[{g.difficulty}]\t{desc_template}{weight}')
