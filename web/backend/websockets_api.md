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
  "position": 15,
  "to_state": 1
}
```

Availability: Player socket only.  

Marks a square on the board belonging to the player who opened this socket.
`position` is an integer from 0 to 24. `to_state` is an integer corresponding
to how the square should be marked:
- 0: White square (unmarked)
- 1: Green square (marked)
- 2: Blue square
- 3: Red square

### board_mark_admin
Example:
```json
{
  "action": "board_mark_admin",
  "player": "Chips",
  "position": 15,
  "to_state": 1
}
```

Availability: Plugin socket only.  

Marks a square on the board for the specified player. Identical to `board_mark`
but with the added `player` argument, which should have a string matching
the player whose name to mark.

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


## Server to Client API

The server only sends packets with one format to the Client. This packet is
sent when anything in the game's state changes, or as a response to any 
incoming message on a socket. Example:
```json
{
  "obscured": true,
  "pboards": [
    {
      "player_id": 123,
      "player_name": "Chips",
      "board": "0000000000001000000000000",
      "disconnected_at": "2020-12-25T08:15:30-08:00"
    },
    {
      "player_id": 456,
      "player_name": "Bob",
      "board": "1111100000001000000000000",
      "disconnected_at": null
    }
  ]
}
```

`obscured` is a flag that specifies whether the client should hide board
goals.

`pboards` is a list of objects corresponding to each player in the current
game. `player_id` is a unique identifier for this player. `board` is the
representation of the squares the player has marked, with the index in the
returned string representing the position of each square and its value the
marking state. `disconnected_at` is an ISO datetime if the player disconnected
from the game, or null if the plyer is still connected.