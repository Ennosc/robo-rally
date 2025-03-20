package model.game.board.tiles;

import model.game.Game;
import model.game.board.robots.Robot;

/**
 * Represents a gear tile.
 */
public class GearTile extends Tile {
    private String direction;

    /**
     * Constructs a GearTile with the specified rotation direction.
     *
     * @param isOnBoard a flag indicating whether the tile is on the board.
     * @param direction the rotation direction for the gear.
     */
    public GearTile(String isOnBoard, String direction) {
        super("Gear", isOnBoard);
        this.direction = direction;
    }

    /**
     * Activates the gear tile.
     * <p>
     * The tile rotates the robot according to its defined logic and notifies the game of the gear animation.
     * </p>
     *
     * @param robot the robot activating the tile.
     */
    @Override
    public void activate(Robot robot) {
        robot.rotateRobot(direction);
        if(robot.getPlayer()!=null) {
            Game.getInstance().notifyAnimation("Gear");
        }
    }

    public String getDirection() {
        return direction;
    }
}
