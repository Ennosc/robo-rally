package viewmodel.tilesGUI;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import helpers.RobotModel;
import viewmodel.Animation;
import model.game.board.Direction;

/**
 * Responsible for rendering and animating robot movements on the game board.
 */
public class RobotRenderer {

    /**
     * Renders a robot on the GridPane and binds its position and direction.
     *
     * @param gridPane The GridPane representing the game board.
     * @param robot    The Robot instance to display.
     */
    public static void renderRobot(GridPane gridPane, RobotModel robot) {
        // Load the robot image based on its name
        Image robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/tiles/Empty.png"));
        String robotName = robot.getName();
        switch (robotName) {
            case "ZoomBot":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/ZoomBot.png"));
                break;
            case "HammerBot":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/HammerBot.png"));
                break;
            case "HulkX90":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/HulkX90.png"));
                break;
            case "SmashBot":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/SmashBot.png"));
                break;
            case "SpinBot":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/SpinBot.png"));
                break;
            case "Twonky":
                robotImage = new Image(RobotRenderer.class.getResourceAsStream("/images/general/robots/Twonky.png"));
                break;
        }

        double cellSize = 0;
        var node = gridPane.getChildren().get(0);
        if (node instanceof StackPane cell) {
            for (var child : cell.getChildren()) {
                if (child instanceof ImageView imageView) {
                    cellSize = imageView.getFitWidth();
                }
            }
        }

        ImageView robotView = new ImageView(robotImage);
        robotView.setFitWidth(cellSize);
        robotView.setFitHeight(cellSize);

        GridPane.setColumnIndex(robotView, robot.getX());
        GridPane.setRowIndex(robotView, robot.getY());

        final int[] lastCellPos = { robot.getX(), robot.getY() };
        double finalCellSize = cellSize;
        var moveListener = (javafx.beans.value.ChangeListener<Number>) (obs, oldVal, newVal) -> {
            int targetX = robot.getX();
            int targetY = robot.getY();

            if (targetX != lastCellPos[0] || targetY != lastCellPos[1]) {
                animateRobotMovement(robotView, lastCellPos[0], lastCellPos[1], targetX, targetY, finalCellSize, lastCellPos);
            }
        };

        robot.xProperty().addListener(moveListener);
        robot.yProperty().addListener(moveListener);

        robot.directionProperty().addListener((obs, oldDir, newDir) -> {
            animateRotation(robotView, newDir);
        });
        applyRotation(robotView, (robot.getDirection()));
        gridPane.getChildren().add(robotView);
    }

    /**
     * Applies rotation to the robot's ImageView based on the direction.
     *
     * @param robotView The ImageView representing the robot.
     * @param direction The direction the robot is facing.
     */
    private static void applyRotation(ImageView robotView, Direction direction) {
        switch (direction) {
            case TOP:
                robotView.setRotate(0);
                break;
            case RIGHT:
                robotView.setRotate(90);
                break;
            case BOTTOM:
                robotView.setRotate(180);
                break;
            case LEFT:
                robotView.setRotate(270);
                break;
        }
    }


    private static void animateRobotMovement(ImageView robotView,
                                             int startX, int startY,
                                             int targetX, int targetY,
                                             double cellSize,
                                             int[] lastCellPos) {
        double dx = (targetX - startX) * cellSize;
        double dy = (targetY - startY) * cellSize;
        Animation.animateMovement(robotView, dx, dy, () -> {
            GridPane.setColumnIndex(robotView, targetX);
            GridPane.setRowIndex(robotView, targetY);
            robotView.setTranslateX(0);
            robotView.setTranslateY(0);
            lastCellPos[0] = targetX;
            lastCellPos[1] = targetY;
        });
    }

    private static void animateRotation(ImageView robotView, Direction newDirection) {
        double targetAngle = getAngleForDirection(newDirection);
        double currentAngle = robotView.getRotate();
        double delta = targetAngle - currentAngle;

        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }

        double animatedTarget = currentAngle + delta;

        Animation.animateRotation(robotView, currentAngle, animatedTarget, () -> {
            robotView.setRotate(targetAngle);
        });
    }


    private static double getAngleForDirection(Direction direction) {
        return switch (direction) {
            case TOP -> 0;
            case RIGHT -> 90;
            case BOTTOM -> 180;
            case LEFT -> 270;
        };
    }
}
