package model.game.board.robots;

import model.game.Game;
import model.game.Player;
import model.game.board.Board;
import model.game.board.Direction;

import java.util.Objects;
import java.util.logging.*;

/**
 * Represents a robot on the game board.
 */
public class Robot {
    private String name;
    private int id;
    private Direction direction;
    private boolean isRebooting = false;
    private Player player;
    private Board board;
    private int[] startingPosition;
    private Logger logger = Logger.getLogger(Robot.class.getName());



    /**
     * Rotates the robot in the given direction.
     *
     * @param directionOfRotation either "clockwise" or "counterclockwise"
     */
    public void rotateRobot(String directionOfRotation){
        this.direction = direction.rotate(directionOfRotation);
        if (player != null) {
            logger.info("New Direction: " + direction);
            Game.getInstance().notifyTurning(player.getPlayerId(), directionOfRotation);
        }
    }

    /**
     * Moves the robot in a given direction.
     * <p>
     *     If steps is positive, the robot moves forward that many steps.
     *     If steps is negative, the robot moves backward one step.
     * </p>
     *
     * @param steps number of steps to move
     */
   public void moveRobot(int steps){
        if (steps > 0) {
            for (int i = 0; i < steps; i++) {
                if(!isRebooting){
                    if(board == null){
                        Game.getInstance().getBoard().moveRobot(this, direction);
                    } else {
                        board.moveRobot(this, direction);
                    }
                }
            }
        } else if (steps < 0){
            Direction invertedDirection = direction.invert();
            if(board == null){
                Game.getInstance().getBoard().moveRobot(this, invertedDirection);
            } else {board.moveRobot(this, invertedDirection);}
        }
    }


    /**
     * Performs a U-turn.
     * <p>
     *     The robot's direction is inverted and two turn messages are sent out.
     * </p>
     */
    public void uTurn() {
       if (player != null) {
           logger.info("direction before uturn: " + direction);
       }

       this.direction = direction.invert();
        if (player != null) {
            logger.info("direction after uturn: " + direction);
        }
       if(player != null){
           Game.getInstance().notifyTurning(player.getPlayerId(), "clockwise");
           Game.getInstance().notifyTurning(player.getPlayerId(), "clockwise");
       }
    }

    public void reboot(){
        this.isRebooting = true;
    }

    public Player getPlayer(){
        return player;
    }
    public Direction getDirection(){
        return direction;
    }

    public void setDirection(Direction direction){
        this.direction = direction;
    }

    public boolean getIsRebooting(){
        return isRebooting;
    }

    public void setIsRebooting(boolean isRebooting){
        this.isRebooting = isRebooting;
    }
    public String getName() {
        return name;
    }
    public void selectStartingPosition(int[] startingPosition) {
        this.startingPosition = startingPosition;
    }
    public int[] getStartingPosition() {
        return startingPosition;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    // Override equals and hashCode as previously advised
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Robot robot)) return false;
        return id == robot.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setBoard(Board board) {
       this.board = board;
    }
}