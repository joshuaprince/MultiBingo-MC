package com.jtprince.bingo.plugin;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to maintain the Plugin-side latest known markings on a single Player's board.
 *
 * The data maintained here is not authoritative; the web backend maintains the authoritative
 * version.
 */
public class PlayerBoard {
    final BingoGame game;
    final Player player;
    protected final int[] knownMarkings = new int[25];
    protected Set<Integer> announcedPositions = new HashSet<>();

    /* Board marking states */
    protected final int UNMARKED = 0;
    protected final int COMPLETE = 1;
    protected final int REVERTED = 2;
    protected final int INVALIDATED = 3;

    public PlayerBoard(Player player, BingoGame game) {
        this.player = player;
        this.game = game;
    }

    /**
     * Update the latest known markings to the provided list of markings.
     * @param jsonBoardStr A board marking string directly from the websocket, such as
     *                     "0000000000001000000000000" for a board with only the middle square
     *                     marked.
     */
    public void update(String jsonBoardStr) {
        if (jsonBoardStr.length() != 25) {
            throw new ArrayIndexOutOfBoundsException(
                "Received board markings that are not 25 characters: " + jsonBoardStr);
        }

        for (int i = 0; i < 25; i++) {
            int toState = Character.getNumericValue(jsonBoardStr.charAt(i));
            if (knownMarkings[i] != toState) {
                this.onChange(i, toState);
            }
            this.knownMarkings[i] = toState;
        }
    }

    protected void onChange(int position, int toState) {
        this.considerAnnounceChange(position, toState);
    }

    protected void considerAnnounceChange(int position, int toState) {
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
            ConcreteGoal square = this.game.gameBoard.getSquare(position);
            this.game.messages.announcePlayerMarking(this.player, square, invalidated);
        }
    }
}
