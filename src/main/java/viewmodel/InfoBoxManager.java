package viewmodel;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.Duration;
import model.server_client.Client;
import network.CardImageMapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the display of informational messages and damage card updates.
 * <p>
 * This class is responsible for updating the info message, displaying damage card notifications,
 * and loading card images as needed.
 * </p>
 */
public class InfoBoxManager {

    private final Text infoText;
    private final ImageView infoImage;
    private final Text damageText;
    private final PauseTransition hideDamageLabelTransition;
    private final Client client;
    private final GameDataBridge gameDataBridge;


    /**
     * Constructs an InfoBoxManager with the specified UI elements and client data.
     *
     * @param infoText       the text node used for displaying info messages.
     * @param infoImage      the image view used for displaying card images.
     * @param damageText     the text node used for displaying damage card information.
     * @param client         the client instance.
     * @param gameDataBridge the game data bridge providing game-related data.
     */
    public InfoBoxManager(Text infoText,
                          ImageView infoImage,
                          Text damageText,
                          Client client,
                          GameDataBridge gameDataBridge) {
        this.infoText = infoText;
        this.infoImage = infoImage;
        this.damageText = damageText;
        this.client = client;
        this.gameDataBridge = gameDataBridge;
        gameDataBridge.getDamageCards().addListener(this::handleNewDamageCardsEntry);
        gameDataBridge.getDamageCards().forEach((playerId, damageCardsList) -> attachDamageCardsListener(playerId, damageCardsList));
        hideDamageLabelTransition = new PauseTransition(Duration.seconds(3));
        hideDamageLabelTransition.setOnFinished(event -> damageText.setVisible(false));
    }

    private void handleNewDamageCardsEntry(MapChangeListener.Change<? extends Integer, ? extends ObservableList<String>> change) {
        if (change.wasAdded()) {
            int playerId = change.getKey();
            ObservableList<String> damageCardsList = change.getValueAdded();
            attachDamageCardsListener(playerId, damageCardsList);
        }
    }

    private void attachDamageCardsListener(int playerId, ObservableList<String> damageCardsList) {
        damageCardsList.addListener((ListChangeListener<String>) change -> {
            synchronized (damageCardsList) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        List<String> addedCards = (List<String>) change.getAddedSubList();
                        String playerName = gameDataBridge.getIdToPlayerNameMap().get(playerId);
                        updateDamageLabel(playerName, addedCards);
                    }
                }
            }
        });
    }

    /**
     * Updates the info message based on the current phase of the game.
     * <p>
     * The message is updated with the player name and a phase-specific message. In phase 3, the last played
     * card image is loaded and displayed.
     * </p>
     *
     * @param playerName   the name of the current player.
     * @param currentPhase the current phase of the game.
     */
    public void updateInfoMessage(String playerName, int currentPhase) {
        infoText.setScaleX(0.90);
        if (currentPhase == 0) {
            Platform.runLater(() -> infoText.setText(playerName + " chooses the starting point"));
        } else if (currentPhase == 1) {
            Platform.runLater(() -> infoText.setText(playerName + " buys upgrade card"));
        }
        else if (currentPhase == 3) {
            String cardName = gameDataBridge.getLastPlayedCard(gameDataBridge.getCurrentPlayerID());
            Platform.runLater(() -> infoText.setText(playerName + " played"));
            infoImage.setImage(loadCardImage(cardName));
        }
    }

    private void updateDamageLabel(String playerName, List<String> cards) {
        List<String> safeCopy;
        //to avoid ConcurrentModificationException
        synchronized (cards) {
            safeCopy = new ArrayList<>(cards);
        }

        Platform.runLater(() -> {
            if (safeCopy.size() > 1) {
                damageText.setText(playerName + " has drawn " + safeCopy.size()
                        + " damage cards: " + String.join(", ", safeCopy));
            } else if (safeCopy.size() == 1) {
                damageText.setText(playerName + " has drawn 1 damage card: " + safeCopy.get(0));
            }
            damageText.setVisible(true);
            hideDamageLabelTransition.stop();
            hideDamageLabelTransition.playFromStart();
        });
    }


    /**
     * Loads the image corresponding to the specified card name.
     *
     * @param cardName the name of the card.
     * @return the Image object if loaded successfully; otherwise, null.
     */
    public Image loadCardImage(String cardName) {
        try {
            client.getLogger().info(cardName);
            String cardPath = CardImageMapper.getImagePath(cardName);
            URL url = getClass().getResource(cardPath);
            if (url != null) {
                return new Image(url.toExternalForm());
            }
        } catch (Exception e) {
            client.getLogger().severe("Error loading card image: " + cardName + " => " + e.getMessage());
        }
        return null;
    }
}
