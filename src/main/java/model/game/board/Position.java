package model.game.board;


/**
 * Represents a coordinate on the game board.
 */
public record Position(int x, int y) {

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}


