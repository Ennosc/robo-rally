package viewmodel;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.util.Duration;
import javafx.scene.image.ImageView;
import network.interpreters.ClientJsonInterpreter;

import java.io.InputStream;
import java.util.Objects;

/**
 * This class creates different types of animations for better user experience.
 */
public class Animation {
    /**
     * Adds a hover effect that slightly enlarges the node when hovered.
     *
     * @param node The node to apply the animation to.
     */
    public static void attachHoverAnimation(Node node) {
        final double scaleUpFactor = 1.2;
        final Duration animationDuration = Duration.millis(200);

        ScaleTransition grow = new ScaleTransition(animationDuration, node);
        grow.setToX(scaleUpFactor);
        grow.setToY(scaleUpFactor);

        ScaleTransition shrink = new ScaleTransition(animationDuration, node);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        node.setOnMouseEntered(e -> grow.playFromStart());
        node.setOnMouseExited(e -> shrink.playFromStart());
    }

    /**
     * Removes the hover effect from a node.
     *
     * @param node The node to remove the animation from.
     */
    public static void removeHoverAnimation(Node node) {
        node.setOnMouseEntered(null);
        node.setOnMouseExited(null);
    }

    /**
     * Scales an ImageView smoothly.
     *
     * @param pile        The ImageView to scale.
     * @param targetScale The target scale factor.
     */
    public static void animateScale(ImageView pile, double targetScale) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), pile);
        scaleTransition.setToX(targetScale);
        scaleTransition.setToY(targetScale);
        scaleTransition.play();
    }

    /**
     * Creates a pop-up effect where the node fades and grows into view.
     *
     * @param node The node to animate.
     */
    public static void animatePopUp(Node node) {
        node.setScaleX(0.7);
        node.setScaleY(0.7);
        node.setOpacity(0);

        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), node);
        scaleTransition.setToX(1);
        scaleTransition.setToY(1);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), node);
        fadeTransition.setToValue(1);

        scaleTransition.play();
        fadeTransition.play();
    }

    /**
     * Animates the movement of a node.
     *
     * @param node The node to move.
     * @param dx The change in X position.
     * @param dy The change in Y position.
     * @param onFinished The action to perform after the animation.
     */
    public static void animateMovement(Node node, double dx, double dy, Runnable onFinished) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(500), node);
        transition.setByX(dx);
        transition.setByY(dy);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.setOnFinished(e -> onFinished.run());
        transition.play();
    }


    /**
     * Animates the rotation of a node.
     *
     * @param node The node to rotate.
     * @param fromAngle The starting angle.
     * @param toAngle The ending angle.
     * @param onFinished The action to perform after the animation.
     */
    public static void animateRotation(Node node, double fromAngle, double toAngle, Runnable onFinished) {
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(500), node);
        rotateTransition.setFromAngle(fromAngle);
        rotateTransition.setToAngle(toAngle);
        rotateTransition.setInterpolator(Interpolator.EASE_BOTH);
        rotateTransition.setOnFinished(e -> onFinished.run());
        rotateTransition.play();
    }


    /**
     * Rotates a gear icon 180 degrees in the specified direction.
     *
     * @param gearNode  The gear node to rotate.
     * @param clockwise If true, rotates clockwise; otherwise counterclockwise.
     */
    public static void animateGearRotation(Node gearNode, boolean clockwise) {
        RotateTransition rt = new RotateTransition(Duration.millis(500), gearNode);
        rt.setFromAngle(0);
        rt.setToAngle(clockwise ? 180 : -180);
        rt.setCycleCount(1);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();
    }


    /**
     * Animates a push panel opening and changing to an activated image.
     *
     * @param pushPanelView The ImageView representing the push panel.
     * @param pushPanel     The name of the push panel.
     */
    public static void popOpenPushPanel(ImageView pushPanelView, String pushPanel) {
        Image originalImage = pushPanelView.getImage();
        Image activatedImage = loadImage("/images/general/tiles/" + pushPanel + "Activated.png");

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), pushPanelView);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);

        scaleUp.setOnFinished(e -> {

            pushPanelView.setImage(activatedImage);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), pushPanelView);
            scaleDown.setFromX(1.2);
            scaleDown.setFromY(1.2);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            scaleDown.setOnFinished(e2 -> {
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e3 -> revertPushPanel(pushPanelView, originalImage));
                pause.play();
            });

            scaleDown.play();
        });

        scaleUp.play();
    }

    /**
     * Loads an image from a given path.
     *
     * @param imagePath The path of the image.
     * @return The loaded Image, or a default image if loading fails.
     */
    private static Image loadImage(String imagePath) {
        try (InputStream is = ClientJsonInterpreter.class.getResourceAsStream(imagePath)) {
            if (is != null) {
                return new Image(is);
            } else {
                return new Image(Objects.requireNonNull(ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Default.png")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Image(Objects.requireNonNull(ClientJsonInterpreter.class.getResourceAsStream("/images/general/tiles/Default.png")));
        }
    }

    /**
     * Reverts the push panel back to its original state.
     *
     * @param pushPanelView The ImageView to revert.
     * @param original      The original image to restore.
     */
    private static void revertPushPanel(ImageView pushPanelView, Image original) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), pushPanelView);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);

        scaleUp.setOnFinished(e -> {
            pushPanelView.setImage(original);
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), pushPanelView);
            scaleDown.setFromX(1.2);
            scaleDown.setFromY(1.2);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });

        scaleUp.play();
    }
}
