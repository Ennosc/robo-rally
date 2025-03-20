package model.game.board.tiles;

import model.game.board.Direction;
import model.game.board.robots.Robot;

import java.util.List;

/**
 * Represents a wall tile.
 */
public class WallTile extends Tile {
    private final List<Boolean> directions;

    /**
     * Constructs a WallTile with the specified wall configuration.
     *
     * @param isOnBoard  a flag indicating whether the tile is on the board.
     * @param directions a list of booleans indicating the presence of walls on each side
     *                   (top, right, bottom, left).
     */
    public WallTile(String isOnBoard, List<Boolean> directions) {
        super("Wall", isOnBoard);
        this.directions = directions;
    }

    @Override
    public void activate(Robot robot) {
    }

    /**
     * Determines whether there is a wall in the specified direction.
     *
     * @param direction the direction to check.
     * @return {@code true} if there is a wall in that direction; {@code false} otherwise.
     */
    public boolean getWall(Direction direction) {
        switch (direction) {
            case TOP:
                return directions.get(0);
            case RIGHT:
                return directions.get(1);
            case BOTTOM:
                return directions.get(2);
            case LEFT:
                return directions.get(3);
            default:
                return false;
        }
    }

    public List<Boolean> getDirections() {
        return directions;
    }
}

