#!/usr/bin/env python3

"""
This script converts a goal listing file from JSON (formatted for Joshimuz's mcbingo,
https://github.com/Joshimuz/mcbingo) to the XML goal file that is parsed in this Bingo game.
"""
import json
import re
import sys
from random import Random
from typing import Dict
from xml.dom import minidom
from xml.etree.ElementTree import Element, tostring, SubElement


def json_to_xml(inf, outf):
    in_json = json.load(inf)

    e_root = Element('MCBingoGoals')
    e_root.set('version', '0')

    for dif, goals in enumerate(in_json):
        for goal in goals:  # for each goal of this difficulty
            e_goal = SubElement(e_root, 'Goal')
            e_goal.set('difficulty', str(dif))

            desc, variables = _parse_description(goal['name'])
            e_desc = SubElement(e_goal, 'Description')
            e_desc.text = desc

            e_goal.set('id', _generate_id(desc, dif))

            if goal.get('tooltiptext'):
                e_tooltip = SubElement(e_goal, 'Tooltip')
                e_tooltip.text = goal['tooltiptext']

            if goal.get('frequency'):
                e_tooltip = SubElement(e_goal, 'Weight')
                e_tooltip.text = str(round(1 / goal['frequency'], 2))

            if goal.get('antisynergy'):
                e_tooltip = SubElement(e_goal, 'Antisynergy')
                e_tooltip.text = goal['antisynergy']

            for varname, (mini, maxi) in variables.items():
                e_var = SubElement(e_goal, 'Variable')
                if varname != 'var':
                    e_var.set('name', varname)
                e_var.set('min', mini)
                e_var.set('max', maxi)

    pretty_xml = minidom.parseString(tostring(e_root)).toprettyxml(indent='  ')
    outf.write(pretty_xml)


def _parse_description(name: str) -> (str, Dict[str, tuple]):
    """
    Example returns:
    "(1-3) items" -> ("$var items", {"var": (1, 3)})
    "(1-3) (4-5) items" -> ("$var1 $var2 items", {"var1": (1, 3), "var2": (4, 5)})
    """
    range_re = re.compile(r'\((\d+)-(\d+)\)')

    # If there is only one number range, its name should be $var.
    # If there are more, their names should be $var1, $var2, ...
    are_vars_numbered = (len(range_re.findall(name)) > 1)

    ranges = {}
    for matchnum, match in enumerate(range_re.findall(name)):
        varname = f'var{matchnum+1}' if are_vars_numbered else 'var'
        ranges[varname] = (match[0], match[1])

    num_replaced = 1
    while range_re.search(name):
        if are_vars_numbered:
            name = range_re.sub(f'$var{num_replaced}', name, count=1)
            num_replaced += 1
        else:
            name = range_re.sub(f'$var', name, count=1)

    return name, ranges


def _generate_id(description: str, difficulty: int):
    """
    Repeatably random method for generating a unique ID for each goal
    """
    description = description.replace(' ', '_').lower()
    description = re.sub(r'\$[0-9a-zA-Z]+_?', '', description)  # eliminate $var strings

    rand = Random(description + str(difficulty))
    uniq = str(rand.randint(10000, 99999))

    if len(description) <= 10:
        return 'jm_' + description + '_' + uniq
    else:
        return 'jm_' + description[:5] + '_' + description[-5:] + uniq


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} [infile.json] [outfile.xml]")
        exit(1)

    with open(sys.argv[1], 'r') as infile:
        with open(sys.argv[2], 'w') as outfile:
            json_to_xml(infile, outfile)
