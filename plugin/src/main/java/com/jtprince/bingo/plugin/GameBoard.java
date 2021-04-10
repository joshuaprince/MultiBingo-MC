package com.jtprince.bingo.plugin;

import com.jtprince.bingo.plugin.player.BingoPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class GameBoard {
    public final BingoGame game;

    private Map<Integer, Space> spaces;

    public GameBoard(BingoGame game) {
        this.game = game;
    }

    /**
     * Update this board with a new set of spaces sent by the server, replacing the existing
     * spaces.
     * @param spaces The new set of Spaces that should be on this board.
     */
    public synchronized void setSpaces(Collection<Space> spaces) {
        // Destroy existing Space objects
        if (this.spaces != null) {
            for (Space sp : this.spaces.values()) {
                sp.destroy();
            }
        }

        this.spaces = new HashMap<>(
            spaces.stream().collect(Collectors.toMap(space -> space.spaceId, space -> space))
        );

        Set<Space> autoSpaces = this.updateAutoMarkedSpaces();
        MCBingoPlugin.logger().info("Auto activation on: " +
            autoSpaces.stream().map(spc -> spc.goalId).collect(Collectors.joining(", ")));

        this.game.transitionToReady();
    }

    public void destroy() {
        if (this.spaces != null) {
            for (Space sp : this.spaces.values()) {
                sp.destroy();
            }
        }
    }

    /**
     * Get a map of all spaces on this Board, where keys are Space IDs and values are Space objects.
     */
    public synchronized Map<Integer, Space> getSpaces() {
        return this.spaces;
    }

    public synchronized boolean isReady() {
        return this.spaces != null && !this.spaces.isEmpty();
    }

    /**
     * Send the web server information about which Players and Spaces are being auto-marked by
     * this plugin.
     */
    public Set<Space> updateAutoMarkedSpaces() {
        Map<String, Collection<Integer>> playerSpaceIdsMap = new HashMap<>();

        Set<Space> autoSpaces = spaces.values().stream()
            .filter(Space::isAutoMarked).collect(Collectors.toUnmodifiableSet());

        for (BingoPlayer p : game.playerManager.getLocalPlayers()) {
            Set<Integer> autoSpaceIds = autoSpaces.stream()
                .map(space -> space.spaceId)
                .filter(id -> game.playerManager.getPlayerBoard(p).isSpaceAutomarkedForPlayer(id))
                .collect(Collectors.toUnmodifiableSet());
            playerSpaceIdsMap.put(p.getName(), autoSpaceIds);
        }

        game.wsClient.sendAutoMarks(playerSpaceIdsMap);

        return autoSpaces;
    }
}
