package viewmodel;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.server_client.Client;
import network.CardImageMapper;
import network.JsonHandler;
import network.messages.actions8.DiscardSomeMessage;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for the Memory Swap feature.
 * <p>
 * This controller allows a player to select a specific number of cards (defined by {@code MAX_SELECTION})
 * from their hand to swap. The selected cards are then sent to the server for processing.
 * </p>
 */
public class MemorySwapController {
    private static final int MAX_SELECTION = 3;
    private final Set<ImageView> chosenCards = new HashSet<>();
    private Client client;
    private GameDataBridge gameDataBridge;

    private ImageView[] cardSlots;
    @FXML
    private ImageView card0, card1, card2, card3, card4, card5, card6, card7, card8, newCard0, newCard1, newCard2;
    @FXML
    private Button chooseButton;
    @FXML
    private Label errorLabel;


    private void initializeCardsListener() {
        gameDataBridge.getCardsInHand().addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    displayCards();
                }
            }
        });
    }

    /**
     * Sets the client for this controller and initializes the card slots and card listeners.
     *
     * @param client the Client instance to be associated with this controller.
     */
    public void setClient(Client client) {
        this.client = client;
        this.gameDataBridge = client.getGameDataBridge();
        cardSlots = new ImageView[]{card0, card1, card2, card3, card4, card5, card6, card7, card8, newCard0, newCard1, newCard2};

        initializeCardsListener();
    }

    @FXML
    private void onPileClicked(MouseEvent event) {
        removeError();
        ImageView clickedCard = (ImageView) event.getSource();

        if (chosenCards.contains(clickedCard)) {
            chosenCards.remove(clickedCard);
        } else if (chosenCards.size() < MAX_SELECTION) {
            chosenCards.add(clickedCard);
        }

        updateCardUI();
    }

    /**
     * Displays the cards currently in the player's hand on the UI.
     * <p>
     * For each card slot, if a card is available it loads and displays the corresponding card image;
     * otherwise, it shows a default register image.
     * </p>
     */
    public void displayCards() {
        Image register = new Image(
                getClass().getResource("/images/general/cards/DefaultRegister.png").toExternalForm());
        List<String> cardsInHand = gameDataBridge.getCardsInHand();


        for (int i = 0; i < cardSlots.length; i++) {
            if (cardsInHand.isEmpty() || i >= cardsInHand.size()) {
                cardSlots[i].setImage(register);
                cardSlots[i].setUserData(null);
            } else {
                String cardName = cardsInHand.get(i);
                Image cardImage = loadCardImage(cardName);

                if (cardImage != null) {
                    cardSlots[i].setImage(cardImage);
                    cardSlots[i].setUserData(cardName);
                    cardSlots[i].setDisable(false);
                    cardSlots[i].setVisible(true);
                    Animation.attachHoverAnimation(cardSlots[i]);
                }
            }
        }
        updateCardUI();
    }

    private void updateCardUI() {
        for (ImageView card : cardSlots) {
            if (card.getUserData() == null) continue;

            if (chosenCards.contains(card)) {
                card.setOpacity(1.0);
                Animation.animateScale(card, 1.2);
                Animation.removeHoverAnimation(card);
            } else {
                card.setOpacity(0.5);
                Animation.animateScale(card, 1.0);
                Animation.attachHoverAnimation(card);
            }
        }
    }

    @FXML
    private void handleChooseButton() {
        if (chosenCards.size() != MAX_SELECTION) {
            setErrorLabel("Please choose " + MAX_SELECTION + " cards");
        } else {
            List<String>selectedCardNames = new ArrayList<>();
            for (ImageView card : chosenCards) {
                Object userData = card.getUserData();
                if (userData != null) {
                    selectedCardNames.add(userData.toString());
                }
            }
            DiscardSomeMessage dsm = new DiscardSomeMessage(selectedCardNames);
            String jsn = JsonHandler.toJson(dsm);
            client.sendMessageToServer(jsn);
            gameDataBridge.removeUpgradeCardForClient(client.getClientID(), "MemorySwap");
            Stage stage = (Stage) chooseButton.getScene().getWindow();
            stage.close();
        }
    }

    private Image loadCardImage(String cardName) {
        try {
            String cardPath = CardImageMapper.getImagePath(cardName);
            URL pngUrl = getClass().getResource(cardPath);
            return new Image(pngUrl.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }

    private void setErrorLabel(String error) {
        errorLabel.setText(error);
    }

    private void removeError() {
        errorLabel.setText("");
    }
}