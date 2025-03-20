package model.game.board.tiles;

import model.game.board.Direction;
import model.game.board.robots.Robot;

/**
 * Represents a laser tile.
 */
public class LaserTile extends Tile {
    private final int damage;
    private final Direction direction;
    private boolean startingLaser;

    /**
     * Constructs a LaserTile with the specified damage and direction.
     *
     * @param isOnBoard a flag indicating whether the tile is on the board.
     * @param direction the direction in which the laser fires.
     * @param damage    the amount of damage dealt.
     */
    public LaserTile(String isOnBoard, Direction direction, int damage) {
        super("Laser", isOnBoard);
        this.direction = direction;
        this.damage = damage;
        this.startingLaser = false;
    }

    @Override
    public void activate(Robot robot) {
    }

    public int getDamage(){
        return damage;
    }

    public Direction getDirection(){
        return direction;
    }

    public boolean isStartingLaser() {
        return startingLaser;
    }

    public void setStartingLaser(boolean startingLaser) {
        this.startingLaser = startingLaser;
    }
}
