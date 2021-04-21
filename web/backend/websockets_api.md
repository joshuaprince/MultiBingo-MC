# Bingo WebSocket API

Bingo players and backends can connect to a WebSocket to perform the following
actions:
- Update board markings for players.
- Subscribe to updates of players' boards.
- Reveal or hide the board.

## Endpoints

### `/ws/board/<gamecode>/` (Spectator)
Spectator view of the game <gamecode>. Receives board updates and can reveal or
hide the board.

### `/ws/board/<gamecode>/<player>/` (Player)
Active player view of the game <gamecode> for the player with name <player>.
Receives board updates, can update the Player Board associated with themselves,
and can reveal or hide the board.

### `/ws/board-plugin/<gamecode>/` (Plugin)
Administrative websocket that should only be used by a Minecraft server that is
hosting a game. This socket will receive board updates, can update any player's
board markings, and can reveal or hide the board.

## Client to Server API

Packets sent over the websocket can have the following formats:

### board_mark
Example:
```json
{
  "action": "board_mark",
  "space_id": 15,
  "to_state": 1
}
```

Availability: Player socket only.  

Marks a space on the board belonging to the player who opened this socket.
`space_id` is an integer representing which space (sent as part of the 
`Board` server-to-client API) should be marked. `to_state` is an integer 
corresponding to how the space should be marked:

- 0: Unmarked space (white)
- 1: *Marked space (green)
- 2: Previously marked space (blue)
  - Space was marked at some point, but is not right now.
- 3: Invalidated space (red)
  - Goal is of the negative type, and the player did the action they were 
    not supposed to do.
- 4: *Pre-marked space (pale green)
  - Goal is of the negative type, and the player has not yet done the action 
    they are not supposed to do.

### board_mark_admin
Example:
```json
{
  "action": "board_mark_admin",
  "player": "Chips",
  "space_id": 15,
  "to_state": 1
}
```

Availability: Plugin socket only.  

Marks a space on the board for the specified player. Identical to `board_mark`
but with the added `player` argument, which should have a string matching
the player whose name to mark.

### game_state
Example:
```json
{
  "action": "game_state",
  "to_state": "start"
}
```

Availability: Plugin socket only.  

Signals the server to change its game state. This means starting or ending a 
game. `to_state` may be either `start` or `end`.

### reveal_board
Example:
```json
{
  "action": "reveal_board"
}
```

Availability: All sockets.  

Signals the server to modify the "obscured" flag on a game, revealing the
board to all players.


### set_automarks
Example:
```json
{
  "action": "set_automarks",
  "players": {
    "Alice": [1, 2, 3],
    "Bob": [2, 3]
  }
}
```

Availability: Plugin socket only.

Indicates to the server that this client is tracking and automatically marking
the board for these space IDs.


### message_relay
Example:
```json
{
  "action": "message_relay",
  "json": "{\"text\": \"a\"}"
}
```

Availability: Plugin socket only.

Sends a message that will be relayed to all connected clients, intended to be
used to broadcast server events such as chat, player advancements, deaths, and
so on in Minecraft. The `message.minecraft` field must contain the message
formatted
as [Raw Minecraft JSON](https://minecraft.fandom.com/wiki/Raw_JSON_text_format).

## Server to Client API

### Board (Player)

This packet is sent as soon as a websocket is opened by a Player. It contains
information about the board, such as the spaces on it. Example:

```json
{
  "board": {
    "obscured": true,
    "shape": "square",
    "spaces": [
      {
        "space_id": 123,
        "position": {
          "x": 0,
          "y": 1
        },
        "text": "3 Cobblestone",
        "tooltip": "",
        "auto": true
      }
    ]
  }
}
```

`obscured` is a flag that specifies whether the client should hide board
goals. If true, the text and tooltip data in `spaces` is not guaranteed to be 
accurate.

`shape` is a string, currently either "square" or "hexagon".

`spaces` is a list of objects, each corresponding to a space on the 
board. Each Goal object consists of:
- A `space_id` integer, which uniquely identifies this space and must be used 
  when the client wants to mark this space.
- A `position` object, containing integer keys `x` and `y`.
  - If the board shape is "square", this indicates the position on the grid, 
    where (0, 0) is the upper left corner and all other positions are 
    nonnegative.
  - If the board shape is "hexagon", this indicates the position of the space
    in [axial coordinates](https://www.redblobgames.com/grids/hexagons/#coordinates-axial).
    The upper-left corner corresponds to (0, 0). Due to the way that axial
    coordinates work, it is possible for valid coordinates to have negative
    values. All valid coordinates must map to being no further above or to the
    left of the position defined by (0, 0).
- A `text` string, with the text that should be displayed on the space.
- An optional `tooltip` string, which specifies additional information about 
  this goal that can be seen on hover.
- An optional `auto` boolean, which if true, indicates that this space will be
  automatically completed and should be represented as such to the user.

### Board (Plugin)

This packet is sent as soon as a websocket is opened by a Plugin. It is
analagous to Board (Player) above, but contains different information. For
example, it describes all goals that the plugin should listen for and report
back to the server when accomplished.

```json
{
  "board": {
    "spaces": [
      {
        "space_id": 0,
        "goal_id": "jm_cobblestone",
        "text": "32 Cobblestone",
        "type": "default",
        "variables": {
          "var": 32
        }
      },
      {
        "space_id": 1,
        "goal_id": "jm_never_die",
        "text": "Never Die",
        "type": "negative",
        "variables": {}
      }
    ]
  }
}
```

`spaces` is a list of 25 Goal objects, each corresponding to a space on the 
board. Each Goal object consists of:
- A `space_id` field, which uniquely identifies this space and must be used 
  when the plugin wants to mark this space.
- A `goal_id` field, which corresponds to the goal ID in goals.yml.
- A `text` field, which contains the text as it is shown to players 
  (variables replaced).
- A `type` field, which contains the goal type (default or negative).
- An optional `variables` object, which lists any variables present on this 
  goal.
  
The web backend may send multiple copies of this packet to a Plugin during the
course of a game. Any packets after the first may be ignored. This means that
a board may not be modified after a Plugin websocket is opened.

### Player Boards

This packet is sent to all connected clients (both players and plugins) when
anything in the game's state changes, or as a response to any incoming message
on a socket. Example:

```json
{
  "pboards": [
    {
      "player_id": 123,
      "player_name": "Chips",
      "markings": [
        {
          "space_id": 0,
          "color": 1
        }
      ],
      "disconnected_at": "2020-12-25T08:15:30-08:00"
    },
    {
      "player_id": 456,
      "player_name": "Bob",
      "markings": [
        {
          "space_id": 0,
          "color": 1
        }
      ],
      "disconnected_at": null
    }
  ]
}
```

`pboards` is a list of objects corresponding to each player in the current
game. `player_id` is a unique identifier for this player. `markings` is a list
of all spaces on this board and the current marking that this player has on that
square. `disconnected_at` is an ISO datetime if the player disconnected from the
game, or null if the plyer is still connected.

### Game State Change

This packet signals game state changes (start, end) and board markings.
There are multiple types of `game_state` packets:

#### Game Start

```json
{
  "game_state": {
    "state": "start"
  }
}
```

Signals to all clients that the game is starting in 7 seconds.

#### Marking

```json
{
  "game_state": {
    "state": "marking",
    "marking_type": "complete",
    "player": "Alice",
    "goal": "64 Cobblestone"
  }
}
```

Indicates that a player has marked a space *for the first time*. This packet
must only be used to display a message to the player that something has been
marked; for an authoritative callback for marking changes, use [Player 
Boards](#player-boards) instead.

`marking_type` is either "complete" or "invalidate".

#### Game End

```json
{
  "game_state": {
    "state": "end",
    "winner": "Alice"
  }
}
```

Indicates that the game has ended, along with the winner if there is one.
`winner` may be null.

### Minecraft Message Relay

This packet relays a message from one Minecraft server to another, such as a 
chat or player death message. Example:

```json
{
  "message_relay": {
    "sender": "KotlinPlugin1234:Alice,Bob",
    "json": "{\"text\": \"a\"}"
  }
}
```

`sender` is the client ID of the Bingo Plugin that sent this message. To 
avoid doubled messages, a client should not print a message whose sender is its
own client ID.


