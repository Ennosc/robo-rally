package model.game.board.tiles;

import model.game.board.Direction;
import model.game.board.robots.Robot;

/**
 * Represents an antenna tile that may indicate a specific direction.
 */
public class AntennaTile extends Tile {

    public Direction direction;

    /**
     * Constructs an AntennaTile with the specified direction.
     *
     * @param isOnBoard a flag indicating whether the tile is on the board.
     * @param direction the direction indicated by the antenna.
     */
    public AntennaTile(String isOnBoard, Direction direction) {
        super("Antenna", isOnBoard);
        this.direction = direction;
    }

    @Override
    public void activate(Robot robot) {
    }

    public Direction getDirection() {
        return direction;
    }
}
