package model.game.board.tiles;

import model.game.board.robots.Robot;

/**
 * Abstract base class for all board tiles.
 */
public abstract class Tile {
    private String name;
    public String isOnBoard;

    /**
     * Constructs a Tile with the specified name and board status.
     *
     * @param name      the name of the tile.
     * @param isOnBoard a flag indicating whether the tile is on the board.
     */
    public Tile(String name, String isOnBoard) {
        this.name = name;
        this.isOnBoard = isOnBoard;
    }

    public String getName() {
        return name;
    }

    public String getIsOnBoard() {
        return isOnBoard;
    }

    /**
     * Activates the tile's effect on a robot.
     *
     * @param robot the robot that activates the tile.
     */
    public abstract void activate(Robot robot);
}

