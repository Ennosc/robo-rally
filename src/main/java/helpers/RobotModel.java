package helpers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import model.game.board.Direction;

/**
 * Represents a robot with its position, direction and name.
 */
public class RobotModel {
    private final IntegerProperty x;
    private final IntegerProperty y;
    private final ObjectProperty<Direction> direction;
    private String name;

    /**
     * Creates a new robot model.
     *
     * @param startX         the starting x coordinate
     * @param startY         the starting y coordinate
     * @param startDirection the initial direction
     * @param name           the name of the robot
     */
    public RobotModel(int startX, int startY, Direction startDirection, String name) {
        this.x = new SimpleIntegerProperty(startX);
        this.y = new SimpleIntegerProperty(startY);
        this.direction = new SimpleObjectProperty<>(startDirection);
        this.name = name;
    }

    // Getters and setters for properties
    public IntegerProperty xProperty() {
        return x;
    }

    public IntegerProperty yProperty() {
        return y;
    }

    public ObjectProperty<Direction> directionProperty() {
        return direction;
    }

    public int getX() {
        return x.get();
    }

    public void setX(int x) {
        this.x.set(x);
    }

    public int getY() {
        return y.get();
    }

    public void setY(int y) {
        this.y.set(y);
    }

    public Direction getDirection() {
        return direction.get();
    }

    public void setDirection(Direction direction) {
        this.direction.set(direction);
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
}
