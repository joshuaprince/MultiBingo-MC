package com.jtprince.bingo.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameBoard {
    protected boolean filled = false;
    protected final ConcreteGoal[] squares = new ConcreteGoal[25];

    public void setSquares(ConcreteGoal[] squares) {
        if (squares.length != this.squares.length) {
            throw new ArrayIndexOutOfBoundsException("setSquares must be called with 25 Goals");
        }
        System.arraycopy(squares, 0, this.squares, 0, 25);
        this.filled = true;
    }

    public List<ConcreteGoal> getSquares() {
        if (!this.filled) {
            return Collections.emptyList();
        }

        return Arrays.asList(squares);
    }

    public ConcreteGoal getSquare(int position) {
        return this.squares[position];
    }
}
