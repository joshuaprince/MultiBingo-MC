package com.jtprince.bingo.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class to maintain the Plugin-side latest known markings on a single Player's board.
 *
 * The data maintained here is not authoritative; the web backend maintains the authoritative
 * version.
 */
public class PlayerBoard {
    final BingoGame game;
    final BingoPlayer player;

    /**
     * The latest known markings on this board.
     * Not authoritative; the webserver holds the authoritative copy.
     */
    private final ArrayList<Integer> markings;

    /**
     * List of positions on this player's board that have been announced as marked, so that no
     * square is announced multiple times (i.e. if the player manually changes it).
     */
    private final Set<Integer> announcedPositions = new HashSet<>();

    /* Board marking states */
    final static int UNMARKED = 0;
    final static int COMPLETE = 1;
    final static int REVERTED = 2;
    final static int INVALIDATED = 3;
    final static int NOT_INVALIDATED = 4;

    public PlayerBoard(BingoPlayer player, BingoGame game) {
        this.player = player;
        this.game = game;

        int numSquares = game.gameBoard.getSquares().size();
        this.markings = new ArrayList<>(Collections.nCopies(numSquares, UNMARKED));
    }

    /**
     * Mark a square on a player's board if it is not currently marked.
     * @param square Square to mark.
     */
    public synchronized void autoMark(@NotNull Square square) {
        int toState = square.goalType.equals("negative") ? 3 : 1;
        if (markings.get(square.position) != toState) {
            this.game.wsClient.sendMarkSquare(player.getName(), square.position, toState);
        }
    }

    /**
     * Update the latest known markings to the provided list of markings.
     * @param jsonBoardStr A board marking string directly from the websocket, such as
     *                     "0000000000001000000000000" for a board with only the middle square
     *                     marked.
     */
    public synchronized void update(String jsonBoardStr) {
        if (jsonBoardStr.length() != this.markings.size()) {
            throw new ArrayIndexOutOfBoundsException(
                "Received board marking string of " + jsonBoardStr.length() + " squares; expected: "
                    + this.markings.size());
        }

        for (int i = 0; i < this.markings.size(); i++) {
            int toState = Character.getNumericValue(jsonBoardStr.charAt(i));
            if (this.markings.get(i) != toState) {
                this.onChange(i, toState);
            }
            this.markings.set(i, toState);
        }
    }

    private void onChange(int position, int toState) {
        this.considerAnnounceChange(position, toState);
    }

    private void considerAnnounceChange(int position, int toState) {
        if (this.announcedPositions.contains(position)) {
            return;
        }

        Boolean invalidated = null;  // null => no announcement
        if (toState == COMPLETE) {
            invalidated = false;
        }

        if (toState == INVALIDATED) {
            invalidated = true;
        }

        if (invalidated != null) {
            this.announcedPositions.add(position);
            Square square = this.game.gameBoard.getSquares().get(position);
            this.game.messages.announcePlayerMarking(this.player, square, invalidated);
        }
    }
}
