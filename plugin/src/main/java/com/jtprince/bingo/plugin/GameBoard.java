package com.jtprince.bingo.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GameBoard {
    final BingoGame game;

    protected boolean filled = false;
    protected final Square[] squares = new Square[25];

    public GameBoard(BingoGame game) {
        this.game = game;
    }

    /**
     * Update this board with a new set of squares sent by the server, replacing the existing
     * squares.
     * @param squares The new set of Squares that should be on this board.
     */
    public void setSquares(Square[] squares) {
        if (squares.length != this.squares.length) {
            // TODO this doesn't really NEED to be dependent on 25 squares.
            throw new ArrayIndexOutOfBoundsException("setSquares must be called with 25 Goals");
        }
        System.arraycopy(squares, 0, this.squares, 0, 25);
        this.filled = true;

        Set<Square> autoSquares =
            this.game.autoMarking.registerGoals(Arrays.asList(squares));

        this.game.plugin.getLogger().info("Auto activation on:" + String.join(", ",
            autoSquares.stream().map(sq -> sq.goalId).collect(Collectors.toUnmodifiableList())));
    }

    public List<Square> getSquares() {
        if (!this.filled) {
            return Collections.emptyList();
        }

        return Arrays.asList(squares);
    }

    public Square getSquare(int position) {
        return this.squares[position];
    }
}
