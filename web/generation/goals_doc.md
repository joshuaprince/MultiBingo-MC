# Bingo Goals Format

Goals are used to generate Bingo boards. Each square on the board consists 
of a single Goal.

Goal Templates are stored in `goals.xml`. An example Goals XML:

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

Each XML `<Goal>` node defines a Goal Template. When a board is generated, Goal
Templates are pulled from the XML and placed on a board as concrete Goals. A
Goal Template may have variables (described below) which are fixed when the 
template is placed on a board as a concrete Goal.

#### `difficulty` (Required)
Ranges from 0 to 4. Describes the difficulty of this goal with 0 being "very
easy" and 4 being "very hard". Boards will be generated with select values of
each difficulty for balancing purposes.

#### `id`
A unique identifier for this goal. This is used for automatic activation only.

#### `type`
An optional "Goal Type" that implies that it may behave in a certain way.
Current goal types are:
- `default` - The default, regular goal to achieve.
- `negative` - A goal of the type "Never X" that, if marked, blocks a bingo 
  in its wake instead of counting towards one.

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

#### `<ItemTrigger>`
Defines that this goal can be automatically activated by a Plugin backend by 
obtaining certain items in the player's inventory. See below for more details.

#### `<Auto/>`
This tag does nothing. It simply serves as a flag to indicate that automatic 
activation has been manually implemented for this goal.

## Item Triggers
Item triggers specify that a goal should be satisfied by acquiring 
certain items in the player's inventory. Examples:
```xml
<!-- Basic single item -->
<ItemTrigger>
  <Name>minecraft:diamond</Name>
</ItemTrigger>

<!-- Basic item stack of greater quantity than 1 -->
<Variable min="32" max="64"/>
<ItemTrigger>
  <Name>minecraft:cobblestone</Name>
  <Quantity>$var</Quantity>
</ItemTrigger>

<!-- Multiple different items -->
<ItemTrigger needed="3">
  <Name>minecraft:water_bucket</Name>
  <Name>minecraft:lava_bucket</Name>
  <Name>minecraft:milk_bucket</Name>
</ItemTrigger>

<!-- Variable number of different items -->
<Variable min="2" max="4"/>
<ItemTrigger needed="$var">
  <Name>minecraft:diamond_helmet</Name>
  <Name>minecraft:diamond_chestplate</Name>
  <Name>minecraft:diamond_leggings</Name>
  <Name>minecraft:diamond_boots</Name>
</ItemTrigger>

<!-- Regular expression of multiple unique items -->
<Variable min="2" max="4"/>
<ItemTrigger needed="$var">
  <Name>minecraft:.*_stairs</Name>
</ItemTrigger>
```

#### `<ItemTrigger>`
The root ItemTrigger node defines a set of items that must be acquired to 
trigger this goal. It has an optional `needed` attribute; this specifies the 
number of Item Matches needed to trigger this goal. If unspecified, `needed` 
behaves as if it were set to 1 (any item match triggers the goal). `needed` 
may reference a variable in the Goal definition.

#### `<Name>`
The Name tag is used to match an item by namespaced identifier. It is 
applied as a regular expression against the item stacks in a player's 
inventory, so any regex is allowed.

#### `<Quantity>`
The Quantity tag is optional, and specifies that only an item stack with 
this many or greater items in it can satisfy the goal.

### Item Match Groups
An Item Match Group allows for more complex item requirements. The `<Name>` 
and `<Quantity>` tags are valid within an item match group, but the group 
may limit how many unique item names can count towards a goal.

```xml
<!-- Unique items where different items count as 1 -->
<ItemTrigger needed="2">
  <ItemMatchGroup max-matches="1">
    <Name>minecraft:cod</Name>
    <Name>minecraft:cooked_cod</Name>
    <Name>minecraft:cod_bucket</Name>
  </ItemMatchGroup>
  <ItemMatchGroup max-matches="1">
    <Name>minecraft:salmon</Name>
    <Name>minecraft:cooked_salmon</Name>
    <Name>minecraft:salmon_bucket</Name>
  </ItemMatchGroup>
</ItemTrigger>
```

In the above example, we want the player to have to collect a cod and a 
salmon. But we also want to allow these to be in any form - raw, cooked, or 
bucket. If we just specify the 6 item names, the player could collect 2 cod, 
cook one, and satisfy the goal. By adding `max-matches="1"` to two separate 
item groups, only 1 item in each group can count toward the needed 2.

```xml
<!-- Different items with different quantities -->
<Variable name="quantpoppy" min="5" max="25"/>
<Variable name="quantdand" min="26" max="40"/>
<ItemTrigger needed="2">
  <ItemMatchGroup>
    <Name>minecraft:poppy</Name>
    <Quantity>$quantpoppy</Quantity>
  </ItemMatchGroup>
  <ItemMatchGroup>
    <Name>minecraft:dandelion</Name>
    <Quantity>$quantdand</Quantity>
  </ItemMatchGroup>
</ItemTrigger>
```

Item match groups can also be used when different quantities of different 
items are needed. Here, the player will need to collect somewhere between 5 
and 25 poppies, and somewhere between 26 and 40 dandelions.
