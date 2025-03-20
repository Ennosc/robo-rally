package viewmodel;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.server_client.Client;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeShopController {
    private GameDataBridge gameDataBridge ;
    @FXML
    ImageView rearLaser;
    @FXML
    ImageView adminPrivilege;
    @FXML
    ImageView spamBlocker;
    @FXML
    ImageView memorySwap;
    @FXML
    Button buyButton;
    @FXML
    Label energyLabel;
    @FXML
    Label errorLabel;

    private Client client;
    private ImageView selectedCard = null;
    private HashMap<ImageView, Integer> cardPrices;
    private boolean viewMode;
    private HashMap<ImageView, String> cardImageViewToName;
    private List<String> currentAvailable = new ArrayList<>();
    private List<String> purchasedUpgrades = new ArrayList<>();

    /**
     * Constructs an UpgradeShopController.
     *
     * @param client The client instance.
     * @param viewMode True if the shop is in view-only mode.
     */
    public UpgradeShopController(Client client, boolean viewMode) {
        this.client = client;
        this.viewMode = viewMode;
    }

    /**
     * Initializes the upgrade shop, setting up images, prices, and available upgrades.
     */
    @FXML
    public void initialize() {
        setImages();
        setCardPrices();
        setCardNames();
        Animation.attachHoverAnimation(rearLaser);
        Animation.attachHoverAnimation(adminPrivilege);
        Animation.attachHoverAnimation(spamBlocker);
        Animation.attachHoverAnimation(memorySwap);
        updateEnergyLabel(client.getGameDataBridge().getEnergy(client.getClientID()));
        if (viewMode) {
            disableInteraction();
        }
        updateAvailablePiles();

        if (client.getGameDataBridge().getClientIDToUpgradeCards().get(client.getClientID()) != null) {
            purchasedUpgrades = client.getGameDataBridge().getClientIDToUpgradeCards().get(client.getClientID());
        }
    }

    private void setImages() {
        rearLaser.setImage(getImage("RearLaser"));
        adminPrivilege.setImage(getImage("AdminPrivilege"));
        spamBlocker.setImage(getImage("SpamBlocker"));
        memorySwap.setImage(getImage("MemorySwap"));
    }

    private void setCardNames() {
        cardImageViewToName = new HashMap<>();
        cardImageViewToName.put(rearLaser, "RearLaser");
        cardImageViewToName.put(adminPrivilege, "AdminPrivilege");
        cardImageViewToName.put(spamBlocker, "SpamBlocker");
        cardImageViewToName.put(memorySwap, "MemorySwap");
    }

    private Image getImage(String pile) {
        String imagePath = "/images/general/cards/" + pile + ".png";
        URL pngUrl = getClass().getResource(imagePath);
        return new Image(pngUrl.toExternalForm());
    }

    private void setCardPrices() {
        cardPrices = new HashMap<>();
        cardPrices.put(rearLaser, 2);
        cardPrices.put(adminPrivilege, 1);
        cardPrices.put(spamBlocker, 3);
        cardPrices.put(memorySwap, 1);
    }

    @FXML
    private void onPileClicked(MouseEvent event) {
        setError("");
        ImageView clickedPile = (ImageView) event.getSource();

        if (clickedPile.isDisabled()) {
            return;
        }

        if (selectedCard == clickedPile) {
            selectedCard = null;
            buyButton.setText("NO UPGRADE");
        } else {
            selectedCard = clickedPile;
            buyButton.setText("BUY");
        }

        updateCardStyle(rearLaser);
        updateCardStyle(adminPrivilege);
        updateCardStyle(spamBlocker);
        updateCardStyle(memorySwap);
    }

    private void updateCardStyle(ImageView pile) {
        if (pile == selectedCard) {
            pile.setOpacity(1.0);
            Animation.animateScale(pile, 1.1);
            Animation.removeHoverAnimation(pile);
        } else if (selectedCard == null && currentAvailable.contains(cardImageViewToName.get(pile))) {
            pile.setOpacity(1.0);
            Animation.animateScale(pile, 1.0);
        } else if (!currentAvailable.contains(cardImageViewToName.get(pile))) {
            pile.setOpacity(0.1);
            Animation.animateScale(pile, 1.0);
        } else {
            pile.setOpacity(0.5);
            Animation.animateScale(pile, 1.0);
            Animation.attachHoverAnimation(pile);
        }
    }

    @FXML
    private void handleBuyButton() {
        if (selectedCard != null) {
            boolean checkPrice = checkCardPrice(selectedCard);
            if (!checkPrice) {
                setError("You don't have enough energy cubes!");
                return;
            } else {
                String cardName = cardImageViewToName.get(selectedCard);
                // Use UpgradeCardTypeMapper to get the type.
                String type = UpgradeCardTypeMapper.getType(cardName);
                int count = countBoughtByType(type);

                if (count >= 3) {
                    setError("You already have 3 " + type + " upgrade cards!");
                    return;
                }
                client.sendBuyUpgrade(true, cardName);
                Stage stage = (Stage) buyButton.getScene().getWindow();
                stage.close();
            }
        }
        client.sendBuyUpgrade(false, null);
        Stage stage = (Stage) buyButton.getScene().getWindow();
        stage.close();
    }

    private void updateEnergyLabel(int energy) {
        energyLabel.setText(Integer.toString(energy));
    }

    private boolean checkCardPrice(ImageView pile) {
        int energy = client.getGameDataBridge().getEnergy(client.getClientID());
        return energy >= cardPrices.get(pile);
    }

    private void setError(String error) {
        errorLabel.setText(error);
    }

    private void updateAvailablePiles() {
        currentAvailable = client.getGameDataBridge().getAvailableUpgradeCards();
        for (Map.Entry<ImageView, String> entry : cardImageViewToName.entrySet()) {
            ImageView imageView = entry.getKey();
            String cardName = entry.getValue();

            if (currentAvailable.contains(cardName)) {
                if (!viewMode) {
                    imageView.setDisable(false);
                    Animation.attachHoverAnimation(imageView);
                } else {
                    imageView.setDisable(true);
                    Animation.removeHoverAnimation(imageView);
                }
                imageView.setOpacity(1.0);
            } else {
                imageView.setDisable(true);
                Animation.removeHoverAnimation(imageView);
                imageView.setOpacity(0.1);
            }
        }
    }

    private void disableInteraction() {
        buyButton.setDisable(true);
        rearLaser.setDisable(true);
        adminPrivilege.setDisable(true);
        spamBlocker.setDisable(true);
        memorySwap.setDisable(true);
        Animation.removeHoverAnimation(rearLaser);
        Animation.removeHoverAnimation(adminPrivilege);
        Animation.removeHoverAnimation(spamBlocker);
        Animation.removeHoverAnimation(memorySwap);
        setError("It is not your turn to buy upgrade.");
    }

    private int countBoughtByType(String type) {
        int count = 0;
        for (String card : purchasedUpgrades) {
            String currentType = UpgradeCardTypeMapper.getType(card);
            if (currentType != null && currentType.equalsIgnoreCase(type)) {
                count++;
            }
        }
        return count;
    }
}
