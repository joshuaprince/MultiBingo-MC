package com.jtprince.bingo.plugin;

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

        // Register spaces with auto marking
        Set<Space> autoSpaces = this.game.autoMarking.registerGoals(spaces);
        MCBingoPlugin.logger().info("Auto activation on:" + String.join(", ",
            autoSpaces.stream().map(spc -> spc.goalId).collect(Collectors.toUnmodifiableList())));

        this.game.transitionToReady();
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
}
