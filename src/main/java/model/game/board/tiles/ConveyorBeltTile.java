package model.game.board.tiles;

import model.game.board.Direction;
import model.game.board.robots.Robot;
import java.util.*;

/**
 * Represents a conveyor belt tile.
 */
public class ConveyorBeltTile extends Tile {
    private Direction outflowDirection;
    private List<Direction> inflowDirections;
    private int speed;

    /**
     * Constructs a ConveyorBeltTile with the specified outflow direction, inflow directions, and speed.
     *
     * @param isOnBoard       a flag indicating whether the tile is on the board.
     * @param outflowDirection the direction in which the conveyor belt moves robots.
     * @param inflowDirections the directions from which robots can enter.
     * @param speed            the speed of the conveyor belt.
     */
    public ConveyorBeltTile(String isOnBoard, Direction outflowDirection, List<Direction> inflowDirections, int speed) {
        super("ConveyorBelt", isOnBoard);
        this.outflowDirection = outflowDirection;
        this.inflowDirections = inflowDirections;
        this.speed = speed;
    }

    public Direction getOutflowDirection() {
        return outflowDirection;
    }

    public List<Direction> getInflowDirections() {
        return inflowDirections;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public void activate(Robot robot) {
    }

    /**
     * Determines whether a robot can enter the tile from the given direction.
     *
     * @param direction the direction from which the robot is entering.
     * @return {@code true} if entry is allowed; {@code false} otherwise.
     */
    public boolean canEnterFrom(Direction direction) {
        return inflowDirections.contains(direction);
    }
}

