package model.game.board.tiles;

import model.game.board.Direction;
import model.game.board.robots.Robot;

/**
 * Represents a reboot token tile.
 */
public class RebootTokenTile extends Tile {
    private Direction direction;

    /**
     * Constructs a RebootTokenTile with the specified direction.
     *
     * @param isOnBoard a flag indicating whether the tile is on the board.
     * @param direction the direction to set for the robot upon reboot.
     */
    public RebootTokenTile(String isOnBoard, Direction direction) {
        super("RestartPoint", isOnBoard);
        this.direction = direction;
    }

    @Override
    public void activate(Robot robot) {
    }

    public Direction getDirection() {
        return direction;
    }
}
