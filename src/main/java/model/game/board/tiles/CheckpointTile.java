package model.game.board.tiles;

import model.game.Game;
import model.game.board.robots.Robot;
/**
 * Represents a checkpoint tile.
 */
public class CheckpointTile extends Tile {
    private final int checkpointNumber;

    /**
     * Constructs a CheckpointTile with the given checkpoint number.
     *
     * @param isOnBoard       a flag indicating whether the tile is on the board.
     * @param checkpointNumber the checkpoint number.
     */
    public CheckpointTile(String isOnBoard,int checkpointNumber) {
        super("CheckPoint", isOnBoard);
        this.checkpointNumber = checkpointNumber;
    }

    /**
     * Activates the checkpoint tile.
     * <p>
     * If the robot's player has reached the previous checkpoint, the checkpoint count is updated,
     * and the checkpoint reached is notified.
     * </p>
     *
     * @param robot the robot activating the tile.
     */
    @Override
    public void activate(Robot robot) {
        int reachedCheckpoints = robot.getPlayer().getCheckpoints();
        if (reachedCheckpoints == (checkpointNumber - 1)) {
            robot.getPlayer().setCheckpoints(checkpointNumber);
            Game.getInstance().notifyCheckpointReached(robot.getPlayer().getPlayerId(), checkpointNumber);
            Game.getInstance().notifyAnimation("Checkpoint");
        }
    }

    public int getCheckpointNumber() {
        return checkpointNumber;
    }
}
