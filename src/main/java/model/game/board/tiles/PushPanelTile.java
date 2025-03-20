package model.game.board.tiles;

import model.game.Game;
import model.game.board.Board;
import model.game.board.Direction;
import model.game.board.robots.Robot;

import java.util.ArrayList;

/**
 * Represents a push panel tile.
 */
public class PushPanelTile extends Tile {
    private Direction direction;
    private ArrayList<Integer> activationRegisters;

    /**
     * Constructs a PushPanelTile with the specified direction and activation registers.
     *
     * @param isOnBoard          a flag indicating whether the tile is on the board.
     * @param direction          the direction in which the panel pushes.
     * @param activationRegisters the registers in which this panel is active.
     */
    public PushPanelTile(String isOnBoard, Direction direction, ArrayList<Integer> activationRegisters) {
        super("PushPanel", isOnBoard);
        this.direction = direction;
        this.activationRegisters = activationRegisters;
        this.isOnBoard = isOnBoard;
    }

    @Override
    public void activate(Robot robot) {
    }

    /**
     * Activates the push panel effect on a robot if the current register matches an activation register.
     *
     * @param robot           the robot to push.
     * @param currentRegister the current register index.
     * @param board           the game board.
     */
    public void activatePush(Robot robot, int currentRegister, Board board) {
        for (int i = 0; i < activationRegisters.size(); i++) {
            if((activationRegisters.get(i)-1) == currentRegister) {
                board.moveRobot(robot, direction);
                if(robot.getPlayer()!=null) {
                    Game.getInstance().notifyAnimation("PushPanel");
                }
            }
        }
    }

    public Direction getPushDirection() {
        return direction;
    }

    public ArrayList<Integer> getActivationRegisters() {
        return activationRegisters;
    }
}

