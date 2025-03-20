package viewmodel;

import helpers.SoundFX;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.game.AI.*;
import model.server_client.Client;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class SelectRobotController {
    @FXML
    private Button leftButton;
    @FXML
    private Button rightButton;
    @FXML
    private ImageView robotImageSmall1;
    @FXML
    private ImageView robotImageSmall2;
    @FXML
    private ImageView robotImageSmall3;
    @FXML
    private ImageView robotImageSmall4;
    @FXML
    private ImageView robotImageSmall5;
    @FXML
    private ImageView robotImageSmall6;
    @FXML
    private ImageView robotInfo;
    @FXML
    private Label pageHeader;
    @FXML
    private Button chooseButton;
    @FXML
    public VBox vbox_messages;
    @FXML
    private Label robotErrorLabel;
    @FXML
    private VBox chatPlaceholder;
    @FXML
    private VBox infoBoardPlaceholder;
    @FXML
    private Button addMediumAIButton;
    @FXML
    private Button addStupidAIButton;
    @FXML
    private Button addSmartAIButton;
    @FXML
    private TextField username_tf;
    @FXML
    private Label chosenRobotLabel;
    @FXML
    private Button confirmButton;

    private InfoBoardController infoBoardViewController;
    private ChatViewController chatViewController;
    private LobbyDataBridge lobbyDataBridge;
    private List<Image> imageList;
    private List<Image> centralImageList;
    private int currentIndex; // The currently selected robot index
    private int robotSelection = -1; // -1 indicates no selection
    private Client client;
    private SceneManager sceneManager;
    private Timeline countdownTimeline;
    private int countdownTime = 1; // 5 seconds
    private final Set<Integer> unavailableRobots = new HashSet<>();
    private final Map<Integer, Integer> playerRobotMap = new HashMap<>(); // Maps client IDs to robot IDs
    private final List<AIClient> activeStupidAIs = new ArrayList<>();
    private final List<AIClient> activeMediumAIs = new ArrayList<>();
    private final List<AIClient> activeSmartAIs = new ArrayList<>();
    private Logger logger;
    private final String host;
    private int port;


    public SelectRobotController(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    public void initialize() {
        setStyles();
        loadImages();

        //INFO IMAGES
        loadCentralImages();
        currentIndex = 0;
        updateCentralImage(currentIndex);
        updateSmallImages();

        chooseButton.setDisable(true);
        setAiButtonsDisable();
        disableRobotSelection(true);
        activateSelectionListener();
        chosenRobotLabel.setVisible(false);

        username_tf.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> handleUsernameInput();
                default -> {}
            }
        });

    }

    public void connectToServer() {
        this.client = new Client(this, host, port);
        sceneManager.setClient(this.client);

        if (this.chatViewController != null) {
            this.client.setChatViewController(chatViewController);
            chatViewController.initialize(client);
        }

        Thread clientThread = new Thread(client);
        clientThread.setDaemon(true);
        clientThread.start();

        this.logger = client.getLogger();
        logger.info("Connecting to server");
        this.lobbyDataBridge = client.getLobbyDataBridge();
        attachFigureListener();
        Platform.runLater(() -> {
            client.getLobbyDataBridge().getClientIDToFigure().forEach((clientId, figure) -> {
                markRobotUnavailable(figure);
            });
        });

        client.getLobbyDataBridge().validChoiceProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> {
                    try {
                        sceneManager.switchToSelectMap(client, client.getRobotNameByNumber(robotSelection));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        });
    }
    private void initializeErrorListener() {
        robotErrorLabel.setWrapText(true);
        robotErrorLabel.textProperty().bind(lobbyDataBridge.robotErrorMessageProperty());
        robotErrorLabel.visibleProperty().bind(lobbyDataBridge.robotErrorMessageProperty().isNotEmpty());
    }

    /**
     * Called by SceneManager after creation, to embed the already-loaded UI and controllers
     * into this scene.
     */
    public void setChatAndInfoBoard(Parent chatViewRoot, ChatViewController chatCtrl) {
        chatPlaceholder.getChildren().clear();
        chatPlaceholder.getChildren().add(chatViewRoot);



        this.chatViewController = chatCtrl;


        if (chatViewController != null && this.client != null) {
            chatViewController.initialize(client);
            this.client.setChatViewController(chatViewController);
        }
    }

    /**
     * Sets up the styles for various UI components.
     */
    private void setStyles() {
        Animation.attachHoverAnimation(robotImageSmall1);
        Animation.attachHoverAnimation(robotImageSmall2);
        Animation.attachHoverAnimation(robotImageSmall3);
        Animation.attachHoverAnimation(robotImageSmall4);
        Animation.attachHoverAnimation(robotImageSmall5);
        Animation.attachHoverAnimation(robotImageSmall6);
        Animation.attachHoverAnimation(leftButton);
        Animation.attachHoverAnimation(rightButton);
        Animation.attachHoverAnimation(addStupidAIButton);
        Animation.attachHoverAnimation(addMediumAIButton);
        Animation.attachHoverAnimation(addMediumAIButton);
        Animation.attachHoverAnimation(addSmartAIButton);
        pageHeader.setScaleX(0.92);
    }

    @FXML
    private void handleChooseButton() {
        if (robotSelection == -1 || unavailableRobots.contains(robotSelection)) {
            lobbyDataBridge.setRobotErrorMessage("Please choose another robot.", -1);
            return;
        }

        SoundFX.playSoundEffect("selection.wav");
        sendSelectedRobot(robotSelection);
        client.getLobbyDataBridge().addClientIDToFigure(client.getClientID(), robotSelection);
    }


    /**
     * Activates a listener on the central robot image to handle selections.
     */
    private void activateSelectionListener() {
        robotInfo.imageProperty().addListener(new ChangeListener<Image>() {
            @Override
            public void changed(ObservableValue<? extends Image> observable, Image oldImage, Image newImage) {
                if (newImage != null && imageList != null && !imageList.isEmpty()) {
                    int figureNumber = imageList.indexOf(newImage);
                    if (figureNumber >= 0) {
                        robotSelection = figureNumber;

                        markRobotUnavailable(robotSelection);
                        chooseButton.setDisable(false);
                    }
                }
            }
        });
    }

    //TODO evtl hier ändern
    private void attachFigureListener() {
        client.getLobbyDataBridge().getClientIDToFigure().addListener((MapChangeListener<? super Integer, ? super Integer>) (change) -> {
            if (change.wasAdded()) {
                int figure = change.getValueAdded();
                Platform.runLater(() -> {
                    markRobotUnavailable(figure);
                });
            }
            if (change.wasRemoved()) {
                int figure = change.getValueRemoved();
                Platform.runLater(() -> {
                    markRobotAvailable(figure);
                });
            }
        });
    }

    @FXML
    private void confirmButtonClicked() {
        handleUsernameInput();
    }

    private void handleUsernameInput() {
        String username = username_tf.getText().trim();
        if (!username.isEmpty()) {
            client.setClientName(username);
            confirmButton.setDisable(true);
            username_tf.setDisable(true);

           int initialRobot = -1;
            for (int i = 0; i < imageList.size(); i++) {
                if (!unavailableRobots.contains(i)) {
                    initialRobot = i;
                    robotSelection = initialRobot;
                    playerRobotMap.put(client.getClientID(), initialRobot);
                    updateCentralImage(initialRobot);
                    break;
                }
            }
               if (initialRobot == -1) {
                //robotErrorLabel.setText("All robots are taken!");
                return;
            }


            disableRobotSelection(false);
            chooseButton.setDisable(false);
            updateAddAIButtonState();
            SoundFX.playSoundEffect("selection.wav");

            logger.info("Player added with username: " + username);
        } else {
            // If username is empty or cleared, disable selection
            disableRobotSelection(true);
            chooseButton.setDisable(true);
            setAiButtonsDisable();
            SoundFX.playSoundEffect("selection_decline.wav");
        }
        initializeErrorListener();
    }

    /**
    /**
     * Updates the info board with the newly chosen robot (ID)
     * and whether the player is ready or not.
     */
    private void updateInfoBoard(int robotId) {
        int clientID = client.getClientID();
        String playerName = client.getClientName();

        Platform.runLater(() -> {
            String robotName = (robotId == -1)
                    ? "Not Selected"
                    : client.getRobotNameByNumber(robotId);
            infoBoardViewController.addOrUpdatePlayer(
                    clientID,
                    playerName,
                    robotName,
                    false
            );
            updateRobotLabel(robotName);
        });
    }

    public void markRobotUnavailable(int robotId) {
        if (robotId != -1 && robotId != robotSelection) {
            unavailableRobots.add(robotId);
            applyRobotAvailability(robotId);
        }
    }

    public void markRobotAvailable(int robotId) {
        unavailableRobots.remove(robotId);
        applyRobotAvailability(robotId);
    }

    /**
     * Applies the grayscale effect and disables/enables the robot's small ImageView based on availability.
     *
     * @param robotId The ID of the robot.
     */
    private void applyRobotAvailability(int robotId) {
        Platform.runLater(() -> {
            if (robotId >= 0 && robotId < imageList.size()) {
                ImageView smallImageView = getSmallImageView(robotId);
                if (smallImageView != null) {
                    if (unavailableRobots.contains(robotId)) {
                        ColorAdjust grayscale = new ColorAdjust();
                        grayscale.setSaturation(-1);
                        smallImageView.setEffect(grayscale);
                    } else {
                        smallImageView.setEffect(null);
                    }

                    // If this robot is the local player's selection, ensure it's not grayed out
                    if (robotId == robotSelection) {
                        smallImageView.setEffect(null);
                    }
                }
            }
        });
    }

    private void disableRobotSelection(boolean disable) {
        Platform.runLater(() -> {
            for (int i = 0; i < imageList.size(); i++) {
                ImageView smallImageView = getSmallImageView(i);
                if (smallImageView != null) {
                    ColorAdjust grayscale = new ColorAdjust();
                    grayscale.setSaturation(disable ? -1 : 0);
                    smallImageView.setEffect(grayscale);
                    smallImageView.setDisable(disable);
                }
            }
            leftButton.setDisable(disable);
            rightButton.setDisable(disable);
            robotInfo.setDisable(disable);
        });
    }

    /**
     * Loads all your robot images into a list.
     */
    private void loadImages() {
        imageList = new ArrayList<>();
        try {
            imageList.add(new Image(getClass().getResource("/images/general/robots/ZoomBot.png").toExternalForm()));
            imageList.add(new Image(getClass().getResource("/images/general/robots/HammerBot.png").toExternalForm()));
            imageList.add(new Image(getClass().getResource("/images/general/robots/HulkX90.png").toExternalForm()));
            imageList.add(new Image(getClass().getResource("/images/general/robots/SmashBot.png").toExternalForm()));
            imageList.add(new Image(getClass().getResource("/images/general/robots/SpinBot.png").toExternalForm()));
            imageList.add(new Image(getClass().getResource("/images/general/robots/Twonky.png").toExternalForm()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCentralImages() {
        centralImageList = new ArrayList<>();
        try {
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/ZoomBotInfo.png").toExternalForm()));
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/HammerBotInfo.png").toExternalForm()));
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/HulkX90Info.png").toExternalForm()));
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/SmashBotInfo.png").toExternalForm()));
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/SpinBotInfo.png").toExternalForm()));
            centralImageList.add(
                    new Image(getClass().getResource("/images/general/robotInfos/TwonkyInfo.png").toExternalForm()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Updates the central robot image based on the current index.
     */
    private void updateCentralImage(int robotId) {
        if (centralImageList == null || centralImageList.isEmpty()) {
            robotInfo.setImage(null);
            return;
        }
        Image currentImage = centralImageList.get(robotId);
        robotInfo.setImage(currentImage);
    }

    /**
     * Updates the small robot images and applies grayscale if necessary.
     */
    private void updateSmallImages() {
        if (imageList.isEmpty()) return;
        for (int i = 0; i < 6; i++) {
            if (i >= imageList.size()) break;
            ImageView smallImageView = getSmallImageView(i);
            if (smallImageView != null) {
                smallImageView.setImage(imageList.get(i));
            }
        }
    }

    /**
     * Retrieves the small ImageView based on the robot ID.
     *
     * @param robotId The ID of the robot.
     * @return The corresponding ImageView.
     */
    private ImageView getSmallImageView(int robotId) {
        switch (robotId) {
            case 0:
                return robotImageSmall1;
            case 1:
                return robotImageSmall2;
            case 2:
                return robotImageSmall3;
            case 3:
                return robotImageSmall4;
            case 4:
                return robotImageSmall5;
            case 5:
                return robotImageSmall6;
            default:
                return null;
        }
    }

    @FXML
    private void handleLeftButton() {
        if (imageList.isEmpty()) return;
        moveToNextAvailableRobot(-1); // Direction -1 for left

        SoundFX.playSoundEffect("selection_switch.wav");
    }

    @FXML
    private void handleRightButton() {
        if (imageList.isEmpty()) return;
        moveToNextAvailableRobot(1); // Direction +1 for right

        SoundFX.playSoundEffect("selection_switch.wav");
    }

    @FXML
    private void handleSmallImageClick(MouseEvent event) {
        ImageView clickedImageView = (ImageView) event.getSource();
        Image clickedImage = clickedImageView.getImage();
        if (clickedImage != null && imageList != null && !imageList.isEmpty()) {
            int clickedIndex = imageList.indexOf(clickedImage);
            if (clickedIndex != -1) {
                if (unavailableRobots.contains(clickedIndex)) {
                   lobbyDataBridge.setRobotErrorMessage("This figure is already cohsen by another player.", -1);
                } else {
                    currentIndex = clickedIndex;
                    updateCentralImage(currentIndex);
                    updateSmallImages();

                    // Update robot selection and notify server
                    robotSelection = clickedIndex;

                    markRobotUnavailable(robotSelection);
                    SoundFX.playSoundEffect("selection_switch.wav");
                }
            }
        }
    }

    private void sendSelectedRobot(int robotId) {
        robotSelection = robotId;
        client.sendPlayerValues(client.getClientName(), robotSelection);
        //updateInfoBoard(robotSelection);
        markRobotUnavailable(robotSelection);
    }

    private void moveToNextAvailableRobot(int direction) {
        int totalRobots = imageList.size();
        int newIndex = currentIndex;

        for (int i = 0; i < totalRobots; i++) {
            newIndex = (newIndex + direction + totalRobots) % totalRobots;
            if (!unavailableRobots.contains(newIndex) || newIndex == robotSelection) {
                break;
            }
        }

        currentIndex = newIndex;
        updateCentralImage(currentIndex);
        //TODO richitg so?
        lobbyDataBridge.addClientIDToFigure(client.getClientID(), robotSelection);
        updateSmallImages();
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    private void updateAddAIButtonState() {
        Platform.runLater(() -> {
            if (playerRobotMap.size() >= 6) {
                setAiButtonsDisable();

            } else {
                setAiButtonsEnable();
            }
        });
    }

    @FXML
    private void handleAddStupidAIButton() {
        if (playerRobotMap.size() < 6) {
            setAiButtonsDisable();
            AIClient aiClient = new AIClient(host, port,0);
            activeStupidAIs.add(aiClient);
            new Thread(aiClient).start();
            SoundFX.playSoundEffect("selection.wav");
            logger.info("Added AI Client with ID: " + aiClient.getClientID());
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                setAiButtonsEnable();
            });
            pause.play();
        } else {
            logger.info("Maximum Player/AI clients reached.");
        }
    }

    @FXML
    private void handleAddNormalAIButton() {
        if (playerRobotMap.size() < 6) {
            setAiButtonsDisable();
            AIClient aiClient = new AIClient(host, port,1);
            activeMediumAIs.add(aiClient);
            new Thread(aiClient).start();
            SoundFX.playSoundEffect("selection.wav");
            logger.info("Added AI Client with ID: " + aiClient.getClientID());
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                setAiButtonsEnable();
            });
            pause.play();
        } else {
            logger.info("Maximum Player/AI clients reached.");
        }
    }

    @FXML
    private void handleAddSmartAIButton() {
        if (playerRobotMap.size() < 6) {
            setAiButtonsDisable();
            AIClient aiClient = new AIClient(host, port,2);
            activeSmartAIs.add(aiClient);
            new Thread(aiClient).start();
            SoundFX.playSoundEffect("selection.wav");
            logger.info("Added AI Client with ID: " + aiClient.getClientID());
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                setAiButtonsEnable();
            });
            pause.play();
        } else {
            logger.info("Maximum Player/AI clients reached.");
        }
    }


    private void updateRobotLabel(String robotName){
        chosenRobotLabel.setText(robotName);
        chosenRobotLabel.setVisible(true);
    }

    public void setChooseButtonDisable(){
        chooseButton.setDisable(true);

    }
    public void setChooseButtonEnable(){
        chooseButton.setDisable(false);
    }

    private void setAiButtonsDisable(){
        addMediumAIButton.setDisable(true);
        addStupidAIButton.setDisable(true);
        addSmartAIButton.setDisable(true);
    }

    private void setAiButtonsEnable(){
        addMediumAIButton.setDisable(false);
        addStupidAIButton.setDisable(false);
        addSmartAIButton.setDisable(false);
    }

}
