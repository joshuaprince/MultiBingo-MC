# Bingo Goals Format

Goals are used to generate Bingo boards. A goal is used to create a single
square on the board.

Goals are stored in `goals.xml`. An example Goals XML:

```xml
<MCBingoGoals version="0">
  <Goal difficulty="0" id="cobblestone">
    <Description>$var Cobblestone</Description>
    <Tooltip>Gather cobblestone in your inventory.</Tooltip>
    <Variable min="32" max="64"/>
  </Goal>
  <Goal difficulty="0" id="poppies_dandelions">
    <Description>$var1 Poppies &amp; $var2 Dandelions</Description>
    <Variable name="var1" min="5" max="25"/>
    <Variable name="var2" min="5" max="25"/>
    <Weight>0.25</Weight>
    <Antisynergy>Flowers</Antisynergy>
  </Goal>
```

The root `<MCBingoGoals>` node contains any number of `<Goal>` nodes.

### Goal Nodes

Each goal has the following attributes.
 
#### `difficulty` (Required)
Ranges from 0 to 4. Describes the difficulty of this goal with 0 being "very
easy" and 4 being "very hard". Boards will be generated with select values of
each difficulty for balancing purposes.

#### `id`
A unique identifier for this goal. This is used for automatic activation only.

### Goal Sub-Nodes

Each goal may have the following sub-nodes:

#### `<Description>` (Required)
Inner text should be a short description of this goal. This is the text that 
will be printed on the square on the actual board. Goal variables may be 
referenced with a `$`, such as `$var`.

#### `<Tooltip>`
If present, bingo squares will have a '?' in the upper right corner. When 
hovered over, this tooltip will show up. Used to give further details about 
the goal that would be too verbose to include in the square itself.

#### `<Variable>`
Some goals have numbers that should be randomized every time the goal is 
selected. For example, the player might have to collect anywhere from 32 to 
64 cobblestone - each board will have a different value in this range.

Variables must have attributes `min` and `max` that describe the range that 
this variable may take when the goal is put on a board.

Variables may optionally have a `name` attribute. If unspecified, the 
variable will take name `var`. Variables should only be named if 2 or more 
are being created for a single goal.

Variables may be referenced in the Description tag by adding a `$` to the 
variable name, as seen in both examples.

#### `<Weight>`
Goals may be weighted to appear more or less frequently on the board. If not 
specified, goals have a weight of 1.0. A goal with weight 0.25, for example, 
should appear roughly one quarter as often as a goal with weight of 1.0.s

#### `<Antisynergy>`
No two goals with the same antisynergy will appear on the same board. This is
useful for goals with similar requirements, so that boards are a bit more
varied.
