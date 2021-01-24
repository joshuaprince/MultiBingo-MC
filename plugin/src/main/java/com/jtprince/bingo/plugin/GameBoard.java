package com.jtprince.bingo.plugin;

import java.util.*;
import java.util.stream.Collectors;

public class GameBoard {
    public final BingoGame game;

    private ArrayList<Square> squares;

    public GameBoard(BingoGame game) {
        this.game = game;
    }

    /**
     * Update this board with a new set of squares sent by the server, replacing the existing
     * squares.
     * @param squares The new set of Squares that should be on this board.
     */
    public synchronized void setSquares(Collection<Square> squares) {
        this.squares = new ArrayList<>(squares);

        // Register squares with auto marking
        Set<Square> autoSquares = this.game.autoMarking.registerGoals(squares);
        MCBingoPlugin.logger().info("Auto activation on:" + String.join(", ",
            autoSquares.stream().map(sq -> sq.goalId).collect(Collectors.toUnmodifiableList())));

        this.game.transitionToReady();
    }

    public synchronized ArrayList<Square> getSquares() {
        return this.squares;
    }

    public synchronized boolean isReady() {
        return this.squares != null && !this.squares.isEmpty();
    }
}
