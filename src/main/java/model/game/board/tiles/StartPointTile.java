package model.game.board.tiles;

import model.game.board.robots.Robot;

/**
 * Represents a starting point tile.
 */
public class StartPointTile extends Tile{
    /**
     * Constructs a StartPointTile.
     *
     * @param isOnBoard a flag indicating whether the tile is on the board.
     */
    public StartPointTile(String isOnBoard) {
        super("StartPoint", isOnBoard);
    }
    @Override
    public void activate(Robot robot) {
    }
}
