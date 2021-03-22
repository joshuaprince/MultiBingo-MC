package com.jtprince.bingo.plugin.player;

import com.jtprince.bingo.plugin.BingoGame;
import com.jtprince.bingo.plugin.MCBingoPlugin;
import com.jtprince.bingo.plugin.Space;
import io.github.Skepter.Utils.FireworkUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private final BingoGame game;
    private final BingoPlayer player;

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

    private boolean announcedVictory = false;

    /**
     * List of spaces on this player's board that this plugin has sent to the webserver, but that
     * we haven't gotten a change back yet for. Used to determine if an incoming change was remote
     * or from this plugin.
     */
    private final Set<Integer> spacesChangingInFlight = new HashSet<>();

    /**
     * List of spaces on this player's board that were changed by something outside of this plugin,
     * such as clicking on the web UI, meaning that we should no longer try to modify it.
     */
    private final Set<Integer> remoteChangedSpaceIds = new HashSet<>();

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
        if (!isSpaceAutomarkedForPlayer(space.spaceId)) {
            return;
        }

        int toState = space.goalType.equals("negative") ? INVALIDATED : COMPLETE;
        if (markings.get(space.spaceId) != toState) {
            spacesChangingInFlight.add(space.spaceId);
            this.game.wsClient.sendMarkSpace(player.getName(), space.spaceId, toState);
        }
    }

    /**
     * Marks a space as "unmarked" (blue), only if it was previously marked.
     * @param space Space to unmark.
     */
    public synchronized void autoRevert(@NotNull Space space) {
        if (!isSpaceAutomarkedForPlayer(space.spaceId)) {
            return;
        }

        if (markings.get(space.spaceId) == COMPLETE) {
            spacesChangingInFlight.add(space.spaceId);
            this.game.wsClient.sendMarkSpace(player.getName(), space.spaceId, REVERTED);
        }
    }

    /**
     * Update the latest known markings to the provided list of markings.
     * @param markings A board marking JSON object directly from the websocket.
     */
    public synchronized void updateMarkings(JSONArray markings) {
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
                this.onChange(spaceId, this.markings.get(spaceId), toState);
                this.markings.put(spaceId, toState);
            }
        }
    }

    /**
     * Update the latest known list of winning-spaces (indicating that this player has won) and
     * potentially announce the victory.
     * @param win A "win" JSON object directly from the websocket.
     */
    public synchronized void updateWin(@Nullable JSONArray win) {
        if (win == null || win.size() == 0) {
            return;
        }

        considerAnnounceVictory();
    }

    public boolean isSpaceAutomarkedForPlayer(int spaceId) {
        Space spc = game.gameBoard.getSpaces().get(spaceId);
        return (spc != null && spc.isAutoMarked() && !remoteChangedSpaceIds.contains(spaceId));
    }

    private void onChange(int spaceId, int fromState, int toState) {
        if (fromState == UNMARKED && toState == NOT_INVALIDATED) {
            // Initial board markings for Negative spaces - don't do anything special with them
            return;
        }

        if (spacesChangingInFlight.contains(spaceId)) {
            spacesChangingInFlight.remove(spaceId);
        } else {
            MCBingoPlugin.logger().info("Got remote change to " + this.player.getName() +
                " " + game.gameBoard.getSpaces().get(spaceId).goalId + ". No longer tracking.");
            remoteChangedSpaceIds.add(spaceId);
            game.gameBoard.updateAutoMarkedSpaces();
        }
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

    private void considerAnnounceVictory() {
        if (announcedVictory) {
            return;
        }

        for (Player p : player.getBukkitPlayers()) {
            FireworkUtils.spawnSeveralFireworks(game.plugin, p);
        }

        this.game.messages.announcePlayerVictory(player);
        announcedVictory = true;
    }
}
