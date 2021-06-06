# Item Trigger Specification Format

Automated item triggers allow the Bukkit plugin to automatically determine 
whether a player has item(s) in their inventory that satisfy a space on the 
board. Item collection goals can take many forms. Therefore, it is important 
that the format for specifying exactly what the player needs is flexible.

## The item_triggers.yml file

All item triggers are defined in the item_triggers.yml file, located at
```
kotlinplugin/src/main/resources/item_triggers.yml
```

At the file's root is a dictionary called `item_triggers`. Keys in this
dictionary match goal IDs. The value of each key describes what items a 
player needs to satisfy that goal.

## Format by Example

98% of item collection goals should resemble one of the following formats.

### One item

The simplest possible item collection, the player needs one single item in 
their inventory. The `name` key must match a Minecraft namespaced item ID.

```yaml
  jtp_book_quill:
    # Goal: "Book and Quill"
    name: minecraft:writable_book
```

### One item of multiple possible types

The player has choices as to exactly which item they collect.

```yaml
  jtp_chicken:
    # Goal: "Bucket of Fish"
    name:
      - minecraft:cod_bucket
      - minecraft:salmon_bucket
      - minecraft:pufferfish_bucket
      - minecraft:tropical_fish_bucket
```

### Multiple items of the same type

The player must collect a specific number (more than 1) of a single item.

```yaml
  jtp_cobblestone_stack:
    # Goal: "64 Cobblestone"
    total: 64
    name: minecraft:cobblestone
```

This works with multiple possible types as well. In the below example, 8 
regular and 2 sticky pistons will satisfy the requirement. 

```yaml
  jtp_pistons:
    # Goal: "10 Pistons"
    total: 10
    name: 
      - minecraft:piston
      - minecraft:sticky_piston
```

### Multiple items of different types

The player must collect many unique items, one of each. If the player 
collects more than 1 of a type of item, it only counts once.

```yaml
  jtp_diamond_armor:
    # Goal: "Full Diamond Armor"
    unique: 4
    name:
      - minecraft:diamond_helmet
      - minecraft:diamond_chestplate
      - minecraft:diamond_leggings
      - minecraft:diamond_boots
```

In the above example, the player cannot collect 3 pairs of boots and a 
helmet, or any other combination. There must be one of each piece of armor 
in their inventory.

### Variable numbers of items

Variables may be used for any value that accepts a number. The variable must 
be defined and set in the backend with matching `var` tags.

```yaml
  jtp_ender_pearls:
    # Goal: "$var Ender Pearls"
    total: $var
    name: minecraft:ender_pearl
```

### Regular Expression names

Often, the namespaced IDs of similar items will follow a pattern. The `name`
field in item triggers allows for regular expression matching. Different item
names that match the regular expression count once **each** towards `unique` 
requirements.

```yaml
  jtp_saplings:
    # Goal: "$var Different Saplings"
    unique: $var
    name: minecraft:.*_sapling
```

### Item Groups

Sometimes we want a more complex relationship that involves multiple items of
different types. For instance, if we want the player to have to collect "5
unique foods", they should not be able to count "cooked chicken" and "raw
chicken" as 2 foods. Item groups are an advanced mechanism for implementing 
such a goal.

```yaml
  jtp_different_foods:
    # Goal: "5 Unique Foods"
    unique: 5
    name:
      - minecraft:apple
      - minecraft:beetroot
      # ... all the rest of the foods ...
      - minecraft:tropical_fish
    groups:
      - name:
          - minecraft:baked_potato
          - minecraft:potato
      - name:
          - minecraft:cooked_beef
          - minecraft:beef
      - name:
          - minecraft:chicken
          - minecraft:cooked_chicken
      # ... all the rest of the cooked/raw food pairs ...
```

With the above configuration, any item that is listed under the first `name` 
key will count once towards the requirement of 5 foods, regardless of how 
many of that food the player has. Under the `groups` key, any set of items 
under a single `name` key can only count once towards those 5. For example, 
a player that has an apple, beetroot, tropical fish, potato, and baked 
potato will not activate the goal. If the player drops the potato and picks 
up a chicken, the goal will activate.

### More than one each of multiple items

The [Multiple items of different types](#Multiple items of different types) tag
only describes how to add items where only 1 each is needed. Item groups can 
allow us to expand this. This takes advantage of the way the `unique` and 
`total` tags work together - within an item group, if a `total` key is 
specified, that item group will only count towards the outer `unique` if the 
player has `total` items.

```yaml
  jm_roses_dandelions:
    # Goal: "10 Roses and 15 Dandelions"
    unique: 2
    groups:
      - name: minecraft:rose
        total: 10
      - name: minecraft:dandelion
        total: 15
```

### Item attributes (enchantments, potion effects, durability)

No support yet - it's coming.

## Internal Mechanism

If none of the above examples allow you to implement the item trigger you 
are looking for, it may help to understand exactly how inventories are 
scanned when given an item trigger definition.

### Item group tree

The internal representation of every item trigger is a tree of MatchGroup 
objects. A MatchGroup object consists of the following fields:

```
    +---------------+
    | name: Regex[] |
    | unique: int   |
    | total: int    |
    | children: []  |
    +---------------+
```

Every item trigger specified in the YAML has a "root" ItemMatchGroup node that
is derived from the keys directly under the goal ID key. When `groups` is
specified, each value in the `groups` list corresponds to another ItemMatchGroup
that is placed in the enclosing key's `children` list.

All keys are optional in each ItemMatchGroup. When not specified, `name`
and `children` (`groups`) default to an empty list, `unique` and `total` 
default to 1.

The satisfaction of an item trigger is determined on the basis of an entire
inventory. Each ItemMatchGroup maintains counters `u` and `t`. At their
simplest, `u` reflects how many *unique* items in the inventory match this match
group, and `t` reflects the *total* number of items that match this match 
group.

When a Match Group has children, its `u` and `t` counters can be incremented 
by these children. A Match Group finds its "effective U and T" by inspecting 
its children:

1. A child Match Group's `t` value increments the parent's `t` one-to-one up to
   the child's `total`.
2. A child Match Group's `u` value increments the parent's `u` one-to-one up to
   the child's `unique` **only if the child's `t` has reached the child's
   `total`**.
3. A root Match Group is satisfied when its effective `u >= unique` and
   `t >= total`.

### Example

Let's look at the roses and dandelions example first presented above:

```yaml
  jm_roses_dandelions:
    # Goal: "10 Roses and 15 Dandelions"
    unique: 2
    groups:
      - name: minecraft:rose
        total: 10
      - name: minecraft:dandelion
        total: 15
```

This YAML will create a Match Group tree that looks like this:

```
                  +---------------+
                  | name: []      |
                  | unique: 2     |
                  | total: 1      |
                  | children: (2) |
                  +-+-----------+-+
                    |           |
                    v           v
    +-----------------+       +-----------------+
    | name: "mc:rose" |       | name: "mc:ddln" |
    | unique: 1       |       | unique: 1       |
    | total: 10       |       | total: 15       |
    | children: (0)   |       | children: (0)   |
    +-----------------+       +-----------------+
```

First, note that the root Match Group has no `name` key. Therefore, neither 
`u` nor `t` will be incremented by an item internally in that Match Group.

The root Match Group then turns to its children to find its "effective U and T".
Let's say that a player has 12 roses, and 8 dandelions in their inventory - not
enough to satisfy this Item Trigger. The Rose Match Group will only pay
attention to the 12 roses, and its `u` will be calculated to be 1, as there is
only 1 unique item that matches this Match Group. Its `t` value will be
calculated to 10, because while the player has 12 roses, `t` cannot exceed
`total` for a match group. The Rose Match Group will contribute 10 toward the
root's `t`, and 1 toward the root's `u`.

Now let's look at the Dandelion match group. The player has 1 unique item 
that matches, so `u` becomes 1, and 8 this item, so `t` becomes 8. The 
Dandelion match group will contribute 8 toward the root's `t`. But **the 
Dandelion match group will contribute 0 to the root's `u`** - because this 
child's `t` has not reached its `total`.

The root match group's effective `t` is therefore 18, which is greater than 
its requirement of 1. But the root's effective `u` is only 1. Therefore, the 
item trigger is not satisfied.
