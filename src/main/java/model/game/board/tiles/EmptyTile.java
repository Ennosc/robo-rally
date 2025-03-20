package model.game.board.tiles;

import model.game.board.robots.Robot;

/**
 * Represents an empty tile on the game board. An {@code EmptyTile} does not trigger
 * any special effects when activated.
 */
public class EmptyTile extends Tile {

    /**
     * Constructs a new {@code EmptyTile} instance.
     *
     * @param isOnBoard a string indicating whether the tile is on the board
     */
    public EmptyTile(String isOnBoard) {
        super("Empty",isOnBoard);
    }

    @Override
    public void activate(Robot robot) {
    }
}
