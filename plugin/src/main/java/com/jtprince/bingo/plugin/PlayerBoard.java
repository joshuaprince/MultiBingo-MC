package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
     * Maps Space ID to the marking (color) code.
     * Not authoritative; the webserver holds the authoritative copy.
     */
    private final Map<Integer, Integer> markings;

    /**
     * List of spaces on this player's board that have been announced as marked, so that no
     * space is announced multiple times (i.e. if the player manually changes it).
     */
    private final Set<Integer> announcedSpaceIds = new HashSet<>();

    /* Board marking states */
    final static int UNMARKED = 0;
    final static int COMPLETE = 1;
    final static int REVERTED = 2;
    final static int INVALIDATED = 3;
    final static int NOT_INVALIDATED = 4;

    public PlayerBoard(BingoPlayer player, BingoGame game) {
        this.player = player;
        this.game = game;

        this.markings = new HashMap<>();
        for (int spaceId : game.gameBoard.getSpaces().keySet()) {
            this.markings.put(spaceId, UNMARKED);
        }
    }

    /**
     * Mark a space on a player's board if it is not currently marked.
     * @param space Space to mark.
     */
    public synchronized void autoMark(@NotNull Space space) {
        int toState = space.goalType.equals("negative") ? 3 : 1;
        if (markings.get(space.spaceId) != toState) {
            this.game.wsClient.sendMarkSpace(player.getName(), space.spaceId, toState);
        }
    }

    /**
     * Update the latest known markings to the provided list of markings.
     * @param markings A board marking JSON object directly from the websocket.
     */
    public synchronized void update(JSONArray markings) {
        if (markings.size() != this.markings.size()) {
            throw new ArrayIndexOutOfBoundsException(
                "Received board marking of " + markings.size() + " spaces; expected: "
                    + this.markings.size());
        }

        for (Object marking : markings) {
            JSONObject markObj = (JSONObject) marking;
            int spaceId = ((Long) markObj.get("space_id")).intValue();
            int toState = ((Long) markObj.get("color")).intValue();
            if (!this.markings.containsKey(spaceId)) {
                throw new ArrayIndexOutOfBoundsException("Received board marking with space ID " +
                    spaceId + " that does not exist on the board.");
            }
            if (this.markings.get(spaceId) != toState) {
                this.onChange(spaceId, toState);
                this.markings.put(spaceId, toState);
            }
        }
    }

    private void onChange(int spaceId, int toState) {
        this.considerAnnounceChange(spaceId, toState);
    }

    private void considerAnnounceChange(int spaceId, int toState) {
        if (this.announcedSpaceIds.contains(spaceId)) {
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
            this.announcedSpaceIds.add(spaceId);
            Space space = this.game.gameBoard.getSpaces().get(spaceId);
            this.game.messages.announcePlayerMarking(this.player, space, invalidated);
        }
    }
}
