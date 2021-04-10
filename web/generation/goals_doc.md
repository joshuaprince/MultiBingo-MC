# Bingo Goals Format

Goals are used to generate Bingo boards. Each space on the board consists 
of a single Goal.

Goal Templates are stored in `goals.yml`. An example Goals YAML:

```yaml
goals:
  g_cobblestone:
    difficulty: 0
    var: 32..64
    text: $var Cobblestone

  g_never_die:
    difficulty: 1
    type: negative
    text: Never Die
    tooltip: Any death unmarks this square!

  g_colors_terracotta:
    difficulty: 1
    weight: 0.25
    varCustomNamed: 4..7
    text: $varCustomNamed Colors of Terracotta
    antisynergy: TerracottaColor
```

The root `goals` object contains a list of goal objects, with the keys each
corresponding to a unique goal ID.

### Goal Objects

Each `goal` object defines a Goal Template. When a board is generated, Goal
Templates are pulled from the YML and placed on a board as concrete Goals. A
Goal Template may have variables (described below) which are fixed when the 
template is placed on a board as a concrete Goal.

#### difficulty (Required)
Ranges from 0 to 1. Describes the difficulty of this goal with 0 being "easier"
and 1 being "harder". Boards will be generated with pre-set values of each
difficulty to reduce the chances of any board being too easy or too hard.

#### type
An optional "Goal Type" that implies that it may behave in a certain way.
Current goal types are:
- `default` - The default, regular goal to achieve.
- `negative` - A goal of the type "Never X" that, if marked, blocks a bingo 
  in its wake instead of counting towards one. These goals are marked at the 
  start of a game until unmarked otherwise.

#### Variables
Some goals have numbers that should be randomized every time the goal is 
selected. For example, the player might have to collect anywhere from 32 to 
64 cobblestone - each board will have a different value in this range.

A goal may have any number of variable ranges defined. Variables are 
distinguished in the goal's specification by starting with the letters `var`.
`var` itself is a valid variable name, and the recommended name for most 
variables.

Variable ranges take on the format of `(min)..(max)`. For example, `32..64`
will allow the variable to be uniformly distributed between 32 and 64.

Variables may be referenced in the `text` or `tooltip` tag by adding a `$` to 
the variable name, as seen in the examples.

#### text (Required)
A short description of this goal. This is the text that will be printed on the
space on the actual board. Goal variables may be referenced with a `$`, such
as `$var`.

#### tooltip
If present, bingo spaces will have a '?' in the corner. When hovered over, this
tooltip will show up. Used to give further details about the goal that would be
too verbose to include in the space itself.

#### weight
Goals may be weighted to appear more or less frequently on the board. If not 
specified, goals have a weight of 1.0. A goal with weight 0.25, for example, 
should appear roughly one quarter as often as a goal with weight of 1.0.

#### antisynergy
No two goals with the same antisynergy will appear on the same board. This is
useful for goals with similar requirements, so that boards are a bit more
varied.
