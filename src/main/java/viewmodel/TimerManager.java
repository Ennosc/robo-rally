package viewmodel;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.logging.Logger;

public class TimerManager {
    private final GameDataBridge gameDataBridge;
    private final Label timerLabel;
   private Timeline countdownTimeline;
    private Timeline blinkingTimeline;
    private final Logger logger;


    /**
     * Constructs a timer manager.
     *
     * @param gameDataBridge The game data bridge instance.
     * @param timerLabel The label displaying the timer.
     * @param logger Logging instance for logging timer events.
     */
    public TimerManager(GameDataBridge gameDataBridge, Label timerLabel, Logger logger) {
        this.gameDataBridge = gameDataBridge;
        this.timerLabel = timerLabel;
        this.logger = logger;
        initialize();
    }

    private void initialize() {
        setupTimerListener();
    }


    /**
     * Listens for timer updates and starts countdown when needed.
     */
    private void setupTimerListener() {
        gameDataBridge.timerValueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> obs, Number oldVal, Number newVal) {
                Platform.runLater(() -> {
                    int timeLeft = newVal.intValue();
                    timerLabel.setText(String.format("00:%02d", timeLeft));
                    if (timeLeft == 30) {
                        startCountdown();
                    }
                });
            }
        });

        // Bind visibility of the timer label based on timer value
        timerLabel.visibleProperty().bind(
                gameDataBridge.timerValueProperty().greaterThan(-1)
        );
    }

    /**
     * Starts the countdown timer and triggers effects when needed.
     */
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    int current = gameDataBridge.getTimerValue();
                    if (current > 0) {
                        gameDataBridge.setTimerValue(current - 1);
                        if (current == 10) {
                            startBlinkingEffect();
                        }
                    } else {
                        stopBlinkingEffect();
                        countdownTimeline.stop();
                    }
                })
        );

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        logger.info("Countdown started.");
    }

    /**
     * Starts a blinking effect when the timer is low.
     */
    private void startBlinkingEffect() {
        if (blinkingTimeline != null) {
            blinkingTimeline.stop();
        }

        blinkingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), event -> {
                    Platform.runLater(() -> {
                        if (timerLabel.getOpacity() == 1.0) {
                            timerLabel.setOpacity(0.5);
                        } else {
                            timerLabel.setOpacity(1.0);
                        }
                    });
                })
        );

        blinkingTimeline.setCycleCount(Timeline.INDEFINITE);
        blinkingTimeline.play();
        logger.info("Blinking effect started.");
    }

    /**
     * Stops the blinking effect when the timer reaches zero.
     */
    private void stopBlinkingEffect() {
        if (blinkingTimeline != null) {
            blinkingTimeline.stop();
            blinkingTimeline = null;
        }
        Platform.runLater(() -> timerLabel.setOpacity(1.0));
        logger.info("Blinking effect stopped.");
    }

    /**
     * Resets the timer and stops all effects.
     */
    public void resetTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        if (blinkingTimeline != null) {
            blinkingTimeline.stop();
            blinkingTimeline = null;
        }
        timerLabel.setText("");
        timerLabel.setOpacity(1.0);
    }
}
