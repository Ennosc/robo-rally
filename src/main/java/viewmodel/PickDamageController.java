package viewmodel;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.server_client.Client;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling the selection of damage cards.
 * <p>
 * This controller manages the UI for allowing a player to choose one or more damage cards from a set of available piles.
 * The player must select the required number of damage cards before confirming the selection.
 * </p>
 */
public class PickDamageController {

    @FXML
    private Label titleLabel;
    @FXML
    private ImageView damagePile1;
    @FXML
    private ImageView damagePile2;
    @FXML
    private ImageView damagePile3;
    @FXML
    private Label errorLabel;
    @FXML
    private HBox damagePilesContainer;
    @FXML
    private Label damageCount1;
    @FXML
    private Label damageCount2;
    @FXML
    private Label damageCount3;

    private Client client;
    private List<String> availablePiles;
    private int pickCount;
    private final Map<String, Integer> chosenCounts = new HashMap<>();

    /**
     * Initializes the damage selection controller.
     * <p>
     * Sets initial styles and hides the damage pile images until they are set with available data.
     * </p>
     */
    @FXML
    public void initialize() {
        setStyles();
        damagePile1.setVisible(false);
        damagePile2.setVisible(false);
        damagePile3.setVisible(false);
    }

    /**
     * Sets the client for this controller.
     *
     * @param client the Client instance to be associated with this controller.
     */
    public void setClient(Client client) {
        this.client = client;
    }

    private void setStyles() {
        Animation.attachHoverAnimation(damagePile1);
        Animation.attachHoverAnimation(damagePile2);
        Animation.attachHoverAnimation(damagePile3);
        titleLabel.setScaleX(0.92);
    }

    /**
     * Configures the damage piles available for selection and the required number of picks.
     * <p>
     * This method updates the UI to display the available damage card piles and sets the title
     * text to indicate how many damage cards the player must choose.
     * </p>
     *
     * @param piles a list of damage pile identifiers.
     * @param count the number of damage cards the player must pick.
     */
    public void setDamagePiles(List<String> piles, int count) {
        this.availablePiles = piles;
        this.pickCount = count;
        titleLabel.setText(
                (pickCount == 1)
                        ? "CHOOSE YOUR DAMAGE CARD"
                        : "CHOOSE " + pickCount + " DAMAGE CARDS"
        );

        if (!piles.isEmpty()) {
            damagePile1.setImage(getImage(piles.get(0)));
            damagePile1.setUserData(piles.get(0));
            damagePile1.setVisible(true);
        }
        if (piles.size() > 1) {
            damagePile2.setImage(getImage(piles.get(1)));
            damagePile2.setUserData(piles.get(1));
            damagePile2.setVisible(true);
        }
        if (piles.size() > 2) {
            damagePile3.setImage(getImage(piles.get(2)));
            damagePile3.setUserData(piles.get(2));
            damagePile3.setVisible(true);
        }
        centerCards(piles.size());
    }

    @FXML
    private void handleChooseButton() {
        int totalSelected = chosenCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalSelected != pickCount) {
            errorLabel.setText("Please choose " + pickCount + " damage cards.");
        } else {
            errorLabel.setText("");
            List<String> selectedList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : chosenCounts.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    selectedList.add(entry.getKey());
                }
            }
            client.sendSelectedDamage(selectedList);
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void onPileClicked(MouseEvent event) {
        errorLabel.setText("");
        ImageView clickedPile = (ImageView) event.getSource();
        String pileName = (String) clickedPile.getUserData();

        if (pickCount == 1) {
            handleSingleSelection(pileName);
        } else {
            handleMultipleSelection(pileName);
        }

        updatePileUI(damagePile1);
        updatePileUI(damagePile2);
        updatePileUI(damagePile3);
    }

    private void handleSingleSelection(String pileName) {
        if (chosenCounts.containsKey(pileName)) {
            chosenCounts.remove(pileName);
        } else {
            chosenCounts.clear();
            chosenCounts.put(pileName, 1);
        }
    }

    private void handleMultipleSelection(String pileName) {
        int currentCount = chosenCounts.getOrDefault(pileName, 0);
        int totalSelected = chosenCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (currentCount == 0) {
            if (totalSelected < pickCount) {
                chosenCounts.put(pileName, 1);
            } else {
                errorLabel.setText("You may only choose " + pickCount + " cards.");
            }
        } else {
            int freeCapacity = pickCount - (totalSelected - currentCount);
            int newCount = currentCount + 1;
            if (newCount > freeCapacity) {
                newCount = 0;
            }
            if (newCount == 0) {
                chosenCounts.remove(pileName);
            } else {
                chosenCounts.put(pileName, newCount);
            }
        }
    }

    private Image getImage(String pile) {
        String imagePath = "/images/general/cards/" + pile + ".png";
        URL pngUrl = getClass().getResource(imagePath);
        return new Image(pngUrl.toExternalForm());
    }

    private void updatePileUI(ImageView pile) {
        if (pile.getUserData() == null) return;

        String pileName = (String) pile.getUserData();
        int count = chosenCounts.getOrDefault(pileName, 0);
        int totalSelected = chosenCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (count == 0) {
            pile.setOpacity(totalSelected > 0 ? 0.5 : 1.0);
            pile.setEffect(null);
            Animation.animateScale(pile, 1.0);
            Animation.attachHoverAnimation(pile);
        } else {
            pile.setOpacity(1.0);
            Animation.animateScale(pile, 1.2);
            Animation.removeHoverAnimation(pile);
            if (count == 1) {
                pile.setEffect(null);
            } else {
                DropShadow ds = new DropShadow();
                ds.setRadius(5);
                ds.setSpread(1);
                ds.setColor(count == 2 ? Color.web("#BBE6FF") : Color.web("#320013"));
                pile.setEffect(ds);
            }
        }
        updateCountLabel(pile, count);
    }

    private void updateCountLabel(ImageView pile, int count) {
        Label countLabel = null;
        if (pile == damagePile1) {
            countLabel = damageCount1;
        } else if (pile == damagePile2) {
            countLabel = damageCount2;
        } else if (pile == damagePile3) {
            countLabel = damageCount3;
        }
        if (countLabel != null) {
            if (count > 0) {
                countLabel.setText(String.valueOf(count));
                countLabel.setVisible(true);
            } else {
                countLabel.setVisible(false);
            }
        }
    }

    private void centerCards(int pileCount) {
        damagePilesContainer.getChildren().clear();
        damagePilesContainer.setSpacing(30);

        switch (pileCount) {
            case 1:
                damagePilesContainer.getChildren().add(damagePile1.getParent());
                HBox.setMargin(damagePile1.getParent(), new Insets(0, 0, 0, 0));
                break;
            case 2:
                damagePilesContainer.getChildren().addAll(
                        damagePile1.getParent(), damagePile2.getParent());
                HBox.setMargin(damagePile1.getParent(), new Insets(0, 15, 0, 0));
                HBox.setMargin(damagePile2.getParent(), new Insets(0, 0, 0, 15));
                break;
            case 3:
                damagePilesContainer.getChildren().addAll(
                        damagePile1.getParent(), damagePile2.getParent(), damagePile3.getParent());
                break;
            default:
                break;
        }
    }
}
