package viewmodel;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.game.board.Direction;
import model.server_client.Client;

/**
 * Controller for handling the reboot direction selection.
 * <p>
 * This class manages the user interface for choosing a reboot direction
 * when a robot needs to restart.
 * </p>
 */
public class RebootDirectionController {
    @FXML
    private Button buttonTop;
    @FXML
    private Button buttonLeft;
    @FXML
    private Button buttonRight;
    @FXML
    private Button buttonDown;
    @FXML
    private Button confirmButton;
    @FXML
    private Label rebootLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ImageView robotImageView;

    private Direction currentRotation = Direction.TOP;
    private static final int TIMER_DURATION = 10;
    private Timeline countdownTimeline;
    private Client client;

    /**
     * Initializes the reboot direction controller by setting buttons and starting countdown
     * for direction selection.
     */
    @FXML
    public void initialize() {
        setStyles();
        setupButtons();
        startCountdownTimer();
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private void setStyles() {
        Animation.attachHoverAnimation(buttonDown);
        Animation.attachHoverAnimation(buttonTop);
        Animation.attachHoverAnimation(buttonLeft);
        Animation.attachHoverAnimation(buttonRight);
        rebootLabel.setScaleX(0.92);
    }

    /**
     * Sets the robot image based on the robot name.
     *
     * @param robotName The name of the robot.
     */
    public void setRobotImage(String robotName) {
        String robotImagePath = "/images/general/robots/" + robotName + ".png";
        robotImageView.setImage(
                new Image(getClass().getResource(robotImagePath).toExternalForm())
        );
    }

    private void setupButtons() {
        buttonTop.setOnAction(e -> rotateRobot(Direction.TOP));
        buttonRight.setOnAction(e -> rotateRobot(Direction.RIGHT));
        buttonDown.setOnAction(e -> rotateRobot(Direction.BOTTOM));
        buttonLeft.setOnAction(e -> rotateRobot(Direction.LEFT));
        confirmButton.setOnAction(e -> sendRebootDirection(currentRotation));
    }

    private void rotateRobot(Direction targetRotation) {
        double targetAngle = switch (targetRotation) {
            case TOP -> 0;
            case RIGHT -> 90;
            case BOTTOM -> 180;
            case LEFT -> 270;
        };

        double currentAngle = switch (currentRotation) {
            case TOP -> 0;
            case RIGHT -> 90;
            case BOTTOM -> 180;
            case LEFT -> 270;
        };

        double delta = targetAngle - currentAngle;
        if (delta > 180) {
            delta -= 360;
        } else if (delta < -180) {
            delta += 360;
        }
        double animatedTarget = currentAngle + delta;

        Animation.animateRotation(robotImageView, currentAngle, animatedTarget, () -> {
            currentRotation = targetRotation;
            robotImageView.setRotate(targetAngle);
        });
    }

    private void sendRebootDirection(Direction direction) {
        client.sendRebootDirection(direction.toLowercaseString());
        stopTimer();
        closeScreen();
    }

    private void startCountdownTimer() {
        final int[] countdownTime = {TIMER_DURATION};
        countdownTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            countdownTime[0]--;
                            timerLabel.setText(String.valueOf(countdownTime[0]));

                            if (countdownTime[0] <= 0) {
                                Direction autoDirection = MapParser.getRestartPointOrientation();
                                sendRebootDirection(autoDirection);
                                closeScreen();
                            }
                        }
                )
        );
        countdownTimeline.setCycleCount(TIMER_DURATION);
        countdownTimeline.play();
    }

    private void stopTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private void closeScreen() {
        Stage stage = (Stage) rebootLabel.getScene().getWindow();
        stage.close();
    }
}
