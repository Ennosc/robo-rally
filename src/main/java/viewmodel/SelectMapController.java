package viewmodel;

import helpers.SoundFX;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.server_client.Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Controller for the map selection screen in the lobby.
 * <p>
 * This class handles user interactions for selecting a game map, updating the lobby UI,
 * listening for lobby status changes, and managing readiness states.
 * </p>
 */
public class SelectMapController {
    @FXML
    private Button leftButton;
    @FXML
    private Button rightButton;
    @FXML
    private ImageView mapInfo;
    @FXML
    private ImageView mapImageSmall1, mapImageSmall2, mapImageSmall3, mapImageSmall4, mapImageSmall5;
    @FXML
    private VBox chatPlaceholder;
    @FXML
    private VBox infoBoardPlaceholder;
    @FXML
    private Button readyButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label mapNameLabel;
    @FXML
    private Button selectButton;
    @FXML
    private HBox imageHolder;

    private List<Image> imageList;
    private List<String> mapNames;
    private int currentIndex;
    private int chooserId;
    private Timeline countdownTimeline;
    private int countdownTime = 1;
    private boolean isReady = false;
    private String robotName;

    private ChatViewController chatViewController;
    private GameDataBridge gameDataBridge;
    private InfoBoardController infoBoardController;
    private LobbyDataBridge lobbyDataBridge;
    private SceneManager sceneManager;
    private Client client;
    private Logger logger;

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
    }

    public void setChatAndInfoBoard(Parent chatViewRoot, ChatViewController chatCtrl,
                                    Parent infoBoardRoot, InfoBoardController infoBoardCtrl) {
        chatPlaceholder.getChildren().clear();
        chatPlaceholder.getChildren().add(chatViewRoot);

        infoBoardPlaceholder.getChildren().clear();
        infoBoardPlaceholder.getChildren().add(infoBoardRoot);

        this.chatViewController = chatCtrl;
        this.infoBoardController = infoBoardCtrl;
    }

    public void setClient(Client client) {
        this.client = client;
        this.logger = client.getLogger();

        setStyles();
        loadImages();
        currentIndex = 0;
        updateSmallImages();
        disableChooseMap();
        this.lobbyDataBridge = client.getLobbyDataBridge();
        this.gameDataBridge = client.getGameDataBridge();
        initializeStartListener();
        initializeSelectMapListener();
        initializeStatusListener();
        initializeErrorListener();
        initializeSelectButtonListener();
    }


    private void setStyles() {
        Animation.attachHoverAnimation(mapImageSmall1);
        Animation.attachHoverAnimation(mapImageSmall2);
        Animation.attachHoverAnimation(mapImageSmall3);
        Animation.attachHoverAnimation(mapImageSmall4);
        Animation.attachHoverAnimation(mapImageSmall5);
        Animation.attachHoverAnimation(leftButton);
        Animation.attachHoverAnimation(rightButton);
    }
    /**
     * Initializes event listeners for player status updates.
     * <p>
     * This listener tracks changes in the player readiness status and updates the UI accordingly.
     * </p>
     */
    public void initializeStatusListener() {
        lobbyDataBridge.getPlayerInfoList().addListener((ListChangeListener<? super PlayerInfo>) (change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PlayerInfo newPlayer : change.getAddedSubList()) {
                        updateLocalReadinessUI(newPlayer);
                    }
                }
                if (change.wasUpdated() || change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        PlayerInfo updatedPlayer =
                                client.getLobbyDataBridge().getPlayerInfoList().get(i);
                        updateLocalReadinessUI(updatedPlayer);
                    }
                }
            }
        });
    }

    private void initializeSelectMapListener() {
        lobbyDataBridge.getClientIDToMapSelection().addListener(
                (MapChangeListener<Integer, Boolean>) change -> {
                    if (change.wasAdded()) {
                        int clientId = change.getKey();
                        boolean chooser = change.getValueAdded();
                        if (clientId == client.getClientID() && chooser) {
                            enableChooseMap(clientId);
                        } else {
                            disableChooseMap();
                        }
                    }
                }
        );
    }

    private void initializeStartListener() {
        lobbyDataBridge.startProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                startCountdown();
            }
        });
    }

    private void initializeErrorListener() {
        errorLabel.setWrapText(true);
        errorLabel.textProperty().bind(client.getLobbyDataBridge().getErrorMessage());
    }

    private void initializeSelectButtonListener() {
        lobbyDataBridge.selectButtonProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                selectButton.setDisable(false);
            }
        });
    }

    private void updateLocalReadinessUI(PlayerInfo playerInfo) {
        if (playerInfo.getClientId() == client.getClientID()) {
            this.isReady = playerInfo.isReady();
            Platform.runLater(() -> {
                readyButton.setText(isReady ? "Not Ready" : "Ready");
            });
        }
    }
    private void bindReadyStatus(PlayerInfo player) {
        readyButton.textProperty().bind(
                player.readyProperty().asString().map(Boolean::parseBoolean).map(ready -> ready ? "Ready" : "Not Ready")        );
    }


    /**
     * Loads images into the imageList and also sets up mapNames so that each
     * image has an associated name or ID.
     */
    private void loadImages() {
        imageList = new ArrayList<>();
        mapNames = new ArrayList<>();  // parallel to imageList

        try {
            imageList.add(new Image(getClass().getResource("/images/general/map_dizzy_highway.pdf.png").toExternalForm()));
            mapNames.add("Dizzy Highway");//Dizzy Highway

            imageList.add(new Image(getClass().getResource("/images/general/map_extra_crispy.pdf.png").toExternalForm()));
            mapNames.add("Extra Crispy");

            imageList.add(new Image(getClass().getResource("/images/general/map_lost_bearings.pdf.png").toExternalForm()));
            mapNames.add("Lost Bearings");

            imageList.add(new Image(getClass().getResource("/images/general/map_death_trap.pdf.png").toExternalForm()));
            mapNames.add("Death Trap");

            imageList.add(new Image(getClass().getResource("/images/general/map_twister.png").toExternalForm()));
            mapNames.add("Twister");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCentralImage() {
        if (imageList.isEmpty()) {
            mapInfo.setImage(null);
            return;
        }
        Image currentImage = imageList.get(currentIndex);
        mapInfo.setImage(currentImage);
    }

    /**
     * Placeholder image until the player is allowed to choose the map.
     *
     * @param robot player's figure
     */
    public void updateInitialImage(String robot) {
        robotName = robot;
        Image robotImage = new Image(getClass().getResource("/images/general/robots/" + robot + ".png").toExternalForm());
        mapInfo.setImage(robotImage);
        mapInfo.setRotate(180);
    }

    private void updateSmallImages() {
        if (imageList.isEmpty()) return;

        int total = imageList.size();
        int smallImageCount = 5;

        for (int i = 0; i < smallImageCount; i++) {
            int index = i % total;
            ImageView smallImageView = getSmallImageView(i);
            if (smallImageView != null) {
                smallImageView.setImage(imageList.get(index));
            }
        }
    }

    private ImageView getSmallImageView(int i) {
        switch (i) {
            case 0:
                return mapImageSmall1;
            case 1:
                return mapImageSmall2;
            case 2:
                return mapImageSmall3;
            case 3:
                return mapImageSmall4;
            case 4:
                return mapImageSmall5;
            default:
                return null;
        }
    }

    @FXML
    private void handleLeftButton() {
        if (imageList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + imageList.size()) % imageList.size();
        updateCentralImage();
        updateNameLabel();
        SoundFX.playSoundEffect("selection_switch.wav");
    }

    @FXML
    private void handleRightButton() {
        if (imageList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % imageList.size();
        updateCentralImage();
        updateNameLabel();
        SoundFX.playSoundEffect("selection_switch.wav");
    }

    @FXML
    private void handleSmallImageClick(MouseEvent event) {
        ImageView clickedImageView = (ImageView) event.getSource();
        Image clickedImage = clickedImageView.getImage();
        if (clickedImage != null) {
            int clickedIndex = imageList.indexOf(clickedImage);
            if (clickedIndex != -1) {
                currentIndex = clickedIndex;
                updateCentralImage();
                updateNameLabel();
                SoundFX.playSoundEffect("selection_switch.wav");
            }
        }
    }

    private void disableChooseMap() {
        selectButton.setVisible(false);
        mapNameLabel.setVisible(false);
        leftButton.setVisible(false);
        rightButton.setVisible(false);
        imageHolder.setVisible(false);

    }
    /**
     * Enables the map selection UI if the player is the current chooser.
     *
     * @param chooserId The ID of the player allowed to choose the map.
     */
    public void enableChooseMap(int chooserId) {
        this.chooserId = chooserId;
        if (chooserId == client.getClientID()) {
            updateCentralImage();
            selectButton.setVisible(true);
            mapNameLabel.setVisible(true);
            leftButton.setVisible(true);
            rightButton.setVisible(true);
            imageHolder.setVisible(true);
        } else {
            disableChooseMap();
        }
    }

    @FXML
    private void handleReadyButton() {
        if (!isReady) {
            isReady = true;
            readyButton.setText("Not Ready");
            updatePlayerStatus(client.getClientID(), true);
            client.sendSetStatus(true);
            SoundFX.playSoundEffect("selection.wav");
        } else {
            isReady = false;
            readyButton.setText("Ready");
            updatePlayerStatus(client.getClientID(), false);
            client.sendSetStatus(false);
            disableChooseMap();
            updateInitialImage(robotName);
            SoundFX.playSoundEffect("selection.wav");
        }
    }

    @FXML
    private void handleSelectButton() {
        String selectedMapName = mapNames.get(currentIndex);
        client.sendMapSelected(selectedMapName);
        selectButton.setDisable(true);
        SoundFX.playSoundEffect("selection.wav");
        lobbyDataBridge.setSelectButton(false);
    }
    /**
     * Starts the countdown to transition to the game view.
     * <p>
     * Waits for the map rendering to complete before switching to the game scene.
     * </p>
     */
    public void startCountdown() {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            MapParser mapParser = new MapParser();
            mapParser.setLogger(logger);
            GridPane gameBoard = new GridPane();
            client.getGameDataBridge().setGameMapGridPane(gameBoard);
            mapParser.renderGameMap(client.getGameDataBridge().getGameMap(), client, gameBoard);
            updateCheckpointNumber(mapParser.getCheckpointNumber());
            latch.countDown(); // Ensure latch is decremented even if rendering fails
        }).start();
        countdownTime = 1;
        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    countdownTime--;
                    if (countdownTime <= 0) {
                        stopCountdown();
                        new Thread(() -> {
                            try {
                                latch.await(); // Wait for map rendering to complete
                                Platform.runLater(() -> {
                                    try {
                                        sceneManager.switchToGameView(client);
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                });
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                })
        );

        countdownTimeline.setCycleCount(5);
        countdownTimeline.play();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }
    /**
     * Displays an error message in the UI.
     *
     * @param errorMessage The error message to display.
     */
    public void showErrorMessage(String errorMessage) {
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
        //TODO after merge conflict not clear if needed
        //selectButton.setDisable(false);
    }
    /**
     * Hides the error message from the UI.
     */
    public void removeError() {
        errorLabel.setVisible(false);
    }
    /**
     * Updates the stored checkpoint number in the game data bridge.
     *
     * @param newCheckpointNumber The new checkpoint number.
     */
    public void updateCheckpointNumber(int newCheckpointNumber) {
        client.getGameDataBridge().setCheckpointNumber(newCheckpointNumber);
    }

    private void updateNameLabel() {
        String selectedMapName = mapNames.get(currentIndex);
        mapNameLabel.setText(selectedMapName);
    }
    /**
     * Updates the player's status in the UI and info board.
     *
     * @param clientId The ID of the player whose status is being updated.
     * @param status   The new status (true if ready, false otherwise).
     */
    public void updatePlayerStatus(int clientId, boolean status) {
        Platform.runLater(() -> {
            if (clientId == client.getClientID()) {
                this.isReady = status;
                readyButton.setText(isReady ? "Not Ready" : "Ready");
            }
            infoBoardController.updatePlayerStatus(clientId, status);
        });
    }

    public Client getClient() {
        return client;
    }
}
