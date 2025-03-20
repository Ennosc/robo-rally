package viewmodel;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controller for the Game Over screen.
 * <p>
 * This class is responsible for displaying the game over information such as the winner's name
 * and the corresponding robot image.
 * </p>
 */
public class GameOverController {
    @FXML
    private Label playerNameLabel;
    @FXML
    private ImageView robotImageView;

    /**
     * Sets the details of the game winner.
     * <p>
     * This method updates the label with the winner's name and sets the corresponding robot image.
     * The robot image is loaded from the resources path "/images/general/robots/" followed by the
     * given robot name and the ".png" extension. Additionally, the image is rotated by 180 degrees.
     * </p>
     *
     * @param playerName the name of the winning player.
     * @param robotName  the name of the robot associated with the winning player.
     */
    public void setWinnerDetails(String playerName, String robotName) {
        playerNameLabel.setText(playerName);
        String imagePath = "/images/general/robots/" + robotName + ".png";
        Image robotImage = new Image(getClass().getResource(imagePath).toExternalForm());
        robotImageView.setImage(robotImage);
        robotImageView.setRotate(180);
    }
}
