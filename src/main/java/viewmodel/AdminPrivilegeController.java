package viewmodel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.server_client.Client;
import network.CardImageMapper;
import javafx.stage.Stage;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the admin privilege pop up.
 * <p>
 * This class handles user interactions for selecting a register to play the admin privilege upgrade card.
 * </p>
 */
public class AdminPrivilegeController {
    private ImageView selectedRegister = null;
    private int selectedRegisterIndex = -1;
    private Client client;
    private GameDataBridge gameDataBridge;
    private HashMap<Integer, String> cardsInRegister = new HashMap<>();
    private boolean played = false;

    @FXML
    private ImageView register0, register1, register2, register3, register4;
    @FXML
    Label errorLabel;
    private List<ImageView> registerSlots;



    /**
     * Initializes the register slots and resets their state.
     */
    @FXML
    public void initialize() {
        registerSlots = List.of(register0, register1, register2, register3, register4);
        displaySelectedRegisters();
        resetAllRegisters();
    }


    /**
     * Sets the client and updates the game data reference.
     */
    public void setClient(Client client) {
        this.client = client;
        this.gameDataBridge = client.getGameDataBridge();
        this.played = client.getGameDataBridge().isAdminPrivilegePlayed();
    }

    /**
     * Handles a click on a register slot.
     * <p>
     * If a slot is clicked, it is selected. Clicking it again deselects it.
     * </p>
     *
     * @param event The mouse event triggered by clicking a register slot.
     */
    @FXML
    private void onPileClicked(MouseEvent event) {
        removeError();
        ImageView clickedRegister = (ImageView) event.getSource();
        int index = registerSlots.indexOf(clickedRegister);

        if (selectedRegister == clickedRegister) {
            resetRegisterUI(selectedRegister);
            selectedRegister = null;
            selectedRegisterIndex = -1;
            resetAllRegisters();
        } else {
            if (selectedRegister != null) {
                resetRegisterUI(selectedRegister);
            }
            selectedRegister = clickedRegister;
            selectedRegisterIndex = index;

            Animation.animateScale(clickedRegister, 1.2);
            Animation.removeHoverAnimation(clickedRegister);
            clickedRegister.setOpacity(1.0);
            updateRegisterOpacities();
        }
    }

    /**
     * Resets the visual effects of a register slot.
     *
     * @param register The register slot to reset.
     */
    private void resetRegisterUI(ImageView register) {
        Animation.animateScale(register, 1.0);
        Animation.attachHoverAnimation(register);
        register.setOpacity(1.0); // Restore opacity
    }

    /**
     * Resets all register slots to their default appearance.
     */
    private void resetAllRegisters() {
        for (ImageView register : registerSlots) {
            register.setOpacity(1.0);
            Animation.attachHoverAnimation(register);
        }
    }

    /**
     * Updates the opacity of register slots to highlight the selected one.
     */
    private void updateRegisterOpacities() {
        for (ImageView register : registerSlots) {
            if (register != selectedRegister) {
                register.setOpacity(0.5);
            }
        }
    }

    /**
     * Displays the currently selected registers with their assigned card images.
     */
    private void displaySelectedRegisters() {
        Platform.runLater(() -> {
            Map<Integer, String> selectedCards = gameDataBridge.getSelectedCards();
            for (int i = 0; i < registerSlots.size(); i++) {
                ImageView registerSlot = registerSlots.get(i);
                String cardName = selectedCards.get(i);

                if (cardName != null) {
                    Image cardImage = loadCardImage(cardName);
                    if (cardImage != null) {
                        registerSlot.setImage(cardImage);
                        registerSlot.setUserData(cardName);
                    }
                } else {
                    registerSlot.setImage(new Image(getClass()
                            .getResource("/images/general/cards/AdminPrivilegeBS.png")
                            .toExternalForm()));
                    registerSlot.setUserData(null);
                }
            }
        });
    }
    /**
     * Handles the selection of a register slot and sends play card JSON.
     *
     * @param event The mouse event triggered by clicking on choose button.
     */
    @FXML
    public void handleChooseButton(MouseEvent event) {
        if (!played) {
            if (selectedRegisterIndex != -1) {
                client.sendChooseRegister(selectedRegisterIndex);
                client.sendPlayCard("AdminPrivilege");
                this.played = true;
                client.getGameDataBridge().setAdminPrivilegePlayed(true);
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.close();
            }
        } else {
            setError("You have already played this card for the round.");
        }
    }

    /**
     * Cancels the selection and closes the window.
     *
     * @param event The mouse event triggered by clicking the abort button.
     */
    @FXML
    public void handleAbortButton(MouseEvent event) {
        selectedRegister = null;
        selectedRegisterIndex = -1;
        resetAllRegisters(); // Restore full opacity
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the corresponding image for a card name.
     *
     * @param cardName The name of the card.
     * @return The image of the card, or null if not found.
     */
    private Image loadCardImage(String cardName) {
        try {
            String cardPath = CardImageMapper.getImagePath(cardName);
            URL pngUrl = getClass().getResource(cardPath);
            return new Image(pngUrl.toExternalForm());
        } catch (Exception e) {
            return null;
        }
    }

    private void setError(String error) {
        errorLabel.setText(error);
    }

    private void removeError() {
        errorLabel.setText("");
    }


    /**
     * Updates the played status.
     *
     * @param played Whether the Admin Privilege card has been played.
     */
    public void setPlayed(boolean played) {
        this.played = played;
    }
}
