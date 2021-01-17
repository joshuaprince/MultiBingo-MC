package com.jtprince.bingo.plugin;

import java.util.*;
import java.util.stream.Collectors;

public class GameBoard {
    final BingoGame game;

    private ArrayList<Square> squares;

    public GameBoard(BingoGame game) {
        this.game = game;
    }

    /**
     * Update this board with a new set of squares sent by the server, replacing the existing
     * squares.
     * @param squares The new set of Squares that should be on this board.
     */
    public void setSquares(Collection<Square> squares) {
        this.squares = new ArrayList<>(squares);

        // Register squares with auto marking
        Set<Square> autoSquares = this.game.autoMarking.registerGoals(squares);
        this.game.plugin.getLogger().info("Auto activation on:" + String.join(", ",
            autoSquares.stream().map(sq -> sq.goalId).collect(Collectors.toUnmodifiableList())));
    }

    public ArrayList<Square> getSquares() {
        return this.squares;
    }
}
