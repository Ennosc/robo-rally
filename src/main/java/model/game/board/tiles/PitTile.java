package model.game.board.tiles;

import model.game.board.robots.Robot;

/**
 * Represents a pit tile.
 */
public class PitTile extends Tile {
    public PitTile(String isOnBoard) {
        super("Pit",isOnBoard);
    }

    @Override
    public void activate(Robot robot) {
    }
}
