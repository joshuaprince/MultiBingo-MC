#!/usr/bin/env python3

"""
This script reads in a goals.xml file and outputs a list of goals in an easier-to-read format.
"""
import argparse

from goals import GOALS

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Read in a goals.xml file and output them.')
    parser.add_argument('--not-auto', help='Only show goals that have not been automated',
                        action='store_true')
    args = parser.parse_args()

    if args.not_auto:
        GOALS = [g for g in GOALS if not g.is_autoactivated]

    for g in GOALS:
        desc_template = g.description_template
        for varname, (mini, maxi) in g.variable_ranges.items():
            desc_template = desc_template.replace(f'${varname}', f'({mini}-{maxi})')
        print(f'[{g.difficulty}]\t{desc_template}')
