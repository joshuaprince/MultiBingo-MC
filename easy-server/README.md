# Bingo Easy Server Setup Scripts

Easily create a server to use for Bingo on Windows.

## Prerequisites

The following must be installed and accessible from the command line:

- Git for Windows
- Java Development Kit (JDK) 8+

## Initial Setup

1. Clone the repository with Git.
```commandline
git clone https://github.com/joshuaprince/MCBingo.git
```
2. Run the `INSTALL.sh` script in the easy-server directory.

## Playing the Game

1. In the `easy-server` directory, run the `RUN.bat` script to start the server.
2. Open Minecraft and navigate to Multiplayer. Connect to the server with the
   IP address `localhost`.
3. When all players are in the server, prepare a Bingo game with one the 
   following example commands in Minecraft:
```
/bingo prepare square       - Prepares a square game board.
/bingo prepare hexagon      - Prepares a hexagonal game board.
/bingo prepare ABCDEF       - Connects to an existing Bingo game by game code.
```
4. When the game is complete, use `/bingo end` to close the Bingo worlds. You
   may return to step 3 to play again.
5. Run `stop` in the server console to shut down the server.

## Updating

To update the Bingo plugin and server to their latest versions, simply re-run
the `INSTALL.sh` script.
