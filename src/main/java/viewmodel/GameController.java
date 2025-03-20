package viewmodel;

import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.server_client.Client;
import network.CardImageMapper;
import network.JsonHandler;
import network.messages.cards6.PlayCardMessage;
import network.messages.phases7.programming.SelectedCardMessage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controls the game logic and UI updates.
 * This class manages player interactions, board updates, and game events.
 */
public class GameController {
    @FXML
    private VBox chatPlaceHolder;
    @FXML
    private Label phaseLabel;
    @FXML
    private Label label2;
    @FXML
    private GridPane gameBoard;
    @FXML
    private VBox mapPlaceholder;
    @FXML
    private Label errorMessage;
    @FXML
    private HBox checkpointInfoHolder;
    @FXML
    private Label playerNameLabel;
    @FXML
    private VBox register;
    @FXML
    private VBox checkpointsHolder;
    @FXML
    private ChatViewController chatViewController;
    private SceneManager sceneManager;
    private Client client;
    private GameDataBridge gameDataBridge;
    private ImageView[] cardSlots;
    private ImageView[] registerSlots;
    private ImageView[] upgradeCardsSlots;
    private ImageView source;
    private HashMap<Integer, String> robotIdToName = new HashMap<>();
    @FXML
    private ImageView registerCard0, registerCard1, registerCard2, registerCard3, registerCard4;
    @FXML
    private Label energyCountLabel;
    @FXML
    private VBox handPlaceholder;
    @FXML
    private VBox infoPlaceholder;
    @FXML
    private Text infoText;
    @FXML
    private ImageView infoImage;
    @FXML
    private Text damageText;
    @FXML
    private Button upgradeShopButton;
    private List<Integer> playerClientIDs = new ArrayList<>();
    private final Map<Integer, List<Rectangle>> checkpointRectanglesByColumn = new HashMap<>();
    private PauseTransition hideDamageLabelTransition;
    private Logger logger;
    private int selectedPlayerID = -1;
    private int currentPhase;
    private Timeline countdownTimeline;
    private int countdownTime = 3;
    private Map<Integer, ImageView> robotImageViews = new HashMap<>();
    private ImageView currentSelectedRobot = null;
    @FXML
    private Label timer;
    private boolean dragDisabled = false;
    InfoBoxManager infoBoxManager;
    @FXML
    private ImageView permanentCard1, permanentCard2, permanentCard3, temporaryCard1, temporaryCard2, temporaryCard3;
    private ImageView selectedUpgrade = null;
    @FXML
    private HBox registerCardHolder;
    @FXML
    private HBox upgradeCardsHBox;


    private double cachedMapPlaceholderWidth;
    private double cachedMapPlaceholderHeight;

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    /**
     * Constructs the GameController with a client and game data bridge.
     *
     * @param client The client instance.
     * @param gameDataBridge The game data bridge instance.
     */
    public GameController(Client client, GameDataBridge gameDataBridge) {
        this.client = client;
        this.gameDataBridge = gameDataBridge;
        this.logger = client.getLogger();
    }

    @FXML
    public void initialize() {
        TimerManager timerManager = new TimerManager(gameDataBridge, label2, logger);
        infoBoxManager = new InfoBoxManager(infoText, infoImage, damageText,
                                            client, gameDataBridge);
        currentPhase = gameDataBridge.getPhaseValue();
        initializeFields();
        initializeListeners();
        attachMap();
        buildCheckpointsDisplay(playerClientIDs.size(), gameDataBridge.getCheckpointNumber());
        managePhaseView();
        hideDamageLabelTransition = new PauseTransition(Duration.seconds(3));
        hideDamageLabelTransition.setOnFinished(event -> damageText.setVisible(false));
        applyStylesBasedOnRobot(client.getGameDataBridge().getClientIDToFigure().get(client.getClientID()));
        applyCheckpointStyleBasedOnRobot(client.getGameDataBridge().getClientIDToFigure().get(client.getClientID()));
        playerNameLabel.setText("YOUR CARDS");
        initializeShadowRegisterCardListener();
    }

    private void initializeFields() {
        cardSlots = new ImageView[]{Card0, Card1, Card2, Card3, Card4, Card5, Card6, Card7, Card8};
        registerSlots = new ImageView[]{registerCard0, registerCard1, registerCard2, registerCard3, registerCard4};
        upgradeCardsSlots = new ImageView[]{permanentCard1, permanentCard2, permanentCard3, temporaryCard1,
                temporaryCard2, temporaryCard3};

        phaseLabel.setScaleX(0.90);
        phaseLabel.setStyle("-fx-text-alignment: center; -fx-alignment: center;");
        setDefaultImages();
        disableUpgradeCards();
    }

    private void initializeListeners() {
        if (gameDataBridge != null) {
            setCardsInHandListener();
            setRandomCardsListener();
            setSelectedCardsListener();
            bindEnergyUpdates();
            playerClientIDs.addAll(gameDataBridge.getIdToPlayerNameMap().keySet());
            gameDataBridge.getPlayedCards().addListener(this::handleNewPlayedCardsEntry);
            gameDataBridge.getPlayedCards().forEach(this::attachPlayedCardsListener);
            initializePhaseListener();
            initializeCheckpointListener();
            initializeGameOverListener();
            initializeCurrentPlayerListener();
            initializeRebootListener();
            initializeErrorListener();
            initializePickDamage();
            initializeReplacedCardListener();
            initializeUpgradeCardListener();
            //initializeClickedUpgradeCardListeners();
        }
    }


    private void handleNewPlayedCardsEntry(MapChangeListener.Change<? extends Integer, ? extends ObservableList<String>> change) {
        if (change.wasAdded()) {
            int clientId = change.getKey();
            ObservableList<String> playedCardsList = change.getValueAdded();
            attachPlayedCardsListener(clientId, playedCardsList);
        }
    }

    private void attachPlayedCardsListener(int clientId, ObservableList<String> playedCardsList) {
        playedCardsList.addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    updatePlayedCardsUI(clientId, playedCardsList);
                }
            }
        });
    }

    private void updatePlayedCardsUI(int clientId, ObservableList<String> playedCardsList) {
        Platform.runLater(() -> {
            String playerName = gameDataBridge.getIdToPlayerNameMap().get(clientId);
            infoBoxManager.updateInfoMessage(playerName, currentPhase);
            if (selectedPlayerID == clientId && currentPhase == 3) {
                updateActivationPhaseView(clientId);
            }
        });
    }

    private void initializePhaseListener() {
        gameDataBridge.getPhase().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                String phaseText = getPhaseString(gameDataBridge.getPhaseValue());
                currentPhase = gameDataBridge.getPhaseValue();
                updateLabel(phaseText);
                managePhaseView();
                if (currentPhase == 3) {
                    infoText.setText("");
                    infoImage.setImage(null);
                }
                if (newValue.intValue() != 3 && oldValue.intValue() == 3) { // Exiting Activation Phase
                    resetActivationPhaseView();
                    resetRobotClicked();
                }
            });
        });
    }

    private void initializeCheckpointListener() {
        gameDataBridge.getClientIDToCheckpoint().addListener(
                (MapChangeListener<Integer, Integer>) change -> {
                    if (change.wasAdded()) {
                        int clientId = change.getKey();
                        int newCheckpointCount = change.getValueAdded();
                        Platform.runLater(() -> updateCheckpointRectangles(clientId, newCheckpointCount));
                    }
                }
        );
    }

    private void initializeGameOverListener() {
        gameDataBridge.gameOverProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                try {
                    int winnerId = gameDataBridge.getWinnerID();
                    setGameOver(winnerId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initializeCurrentPlayerListener() {
        gameDataBridge.currentPlayerIDProperty().addListener((observable, oldValue, newValue) -> {
            String playerName = gameDataBridge.getIdToPlayerNameMap().get(newValue.intValue());
            if (playerName != null) {
                if (currentPhase == 0) {
                    infoBoxManager.updateInfoMessage(playerName, 0);
                    updateRobotImage(infoImage, gameDataBridge.getCurrentPlayerID());
                }
                if (currentPhase == 1) {
                    infoBoxManager.updateInfoMessage(playerName, 1);
                    updateRobotImage(infoImage, gameDataBridge.getCurrentPlayerID());
                    if (gameDataBridge.getCurrentPlayerID() == client.getClientID()) {
                        Platform.runLater(() -> {
                            try {
                                sceneManager.openUpgradeShopPopUp(client, false);
                                gameDataBridge.setAdminPrivilegePlayed(false);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
        });
    }

    private void initializeRebootListener() {
        gameDataBridge.rebootedClientIDProperty().addListener((obs, oldValue, newValue) -> {
            int rebootedID = newValue.intValue();
            updateRebootedRobotIcon(rebootedID);
            if (rebootedID == client.getClientID()) {
                Platform.runLater(() -> {
                    Integer figureNumber = gameDataBridge.getClientIDToFigure().get(rebootedID);
                    String robotName = getRobotName(figureNumber);
                    try {
                        sceneManager.openRebootDirectionPopUp(client, robotName);
                    } catch (IOException e) {
                        logger.severe("Failed to open RebootDirection popup: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    //Action after ConnectionLost
    private void initializeErrorListener() {
        errorMessage.setWrapText(true);
        errorMessage.textProperty().bind(gameDataBridge.getErrorMessage());
    }

    private void initializePickDamage() {
        gameDataBridge.pickDamageTriggeredProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                try {
                    sceneManager.openPickDamagePopUp(
                            client,
                            gameDataBridge.getAvailableDamageCards(),
                            gameDataBridge.getPickDamageCount()
                    );
                } catch (IOException e) {
                    logger.severe("Failed to open Pick Damage pop-up: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    gameDataBridge.setPickDamageTriggered(false);
                }
            }
        });
    }

    private void initializeReplacedCardListener() {
        gameDataBridge.getReplacedCard().addListener(
                (MapChangeListener<Integer, Map<Integer, String>>) change -> {
                    if (change.wasAdded()) {
                        Map<Integer, String> replacements = change.getValueAdded();
                        if (replacements != null) {
                            replacements.forEach((registerIndex, newCard) ->
                                                         updateRegisterImage(change.getKey(), registerIndex, newCard)
                            );
                        }
                    }
                }
        );
    }

    private void initializeUpgradeCardListener() {
        ObservableList<String> myUpgrades = gameDataBridge.getClientIDToUpgradeCards().get(client.getClientID());
        if (myUpgrades == null) {
            myUpgrades = FXCollections.observableArrayList();
            gameDataBridge.getClientIDToUpgradeCards().put(client.getClientID(), myUpgrades);
        }
        ObservableList<String> finalMyUpgrades = myUpgrades;
        myUpgrades.addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    Platform.runLater(() -> updateUpgradeCardUI(finalMyUpgrades));
                }
            }
        });
    }

    private void initializeShadowRegisterCardListener() {
        gameDataBridge.activeRegisterSlotProperty().addListener((obs, oldSlot, newSlot) -> {
            // Entferne den Effekt vom alten Slot, falls gültig
            int oldIndex = oldSlot.intValue();
            if (oldIndex >= 0 && oldIndex < registerSlots.length) {
                registerSlots[oldIndex].setEffect(null);
            }

            int newIndex = newSlot.intValue();
            if (newIndex == -1) {
                registerSlots[oldIndex].setEffect(null);
            }
            if (newIndex >= 0 && newIndex < registerSlots.length) {
                DropShadow ds = new DropShadow();
                ds.setColor(Color.web("#BBE6FF"));
                ds.setSpread(1);
                ds.setRadius(3.3);
                registerSlots[newIndex].setEffect(ds);
            }

        });
    }

    private void attachMap() {
        Platform.runLater(() -> {
            if (gameDataBridge != null && gameDataBridge.getGameMapGridPane() != null) {
                // Use pre-rendered map
                gameBoard = gameDataBridge.getGameMapGridPane();
            }

            // Attach the map to the VBox
            mapPlaceholder.getChildren().clear();
            VBox.setVgrow(gameBoard, Priority.ALWAYS);
            gameBoard.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            mapPlaceholder.getChildren().add(gameBoard);

            // Set initial cached values
            cachedMapPlaceholderWidth = mapPlaceholder.getMaxWidth();
            cachedMapPlaceholderHeight = mapPlaceholder.getMaxHeight();
            logger.info("Initial cachedMapPlaceholderWidth: " + cachedMapPlaceholderWidth);
            logger.info("Initial cachedMapPlaceholderHeight: " + cachedMapPlaceholderHeight);

            int rows = gameDataBridge.getGameMap().size();
            int columns = gameDataBridge.getGameMap().get(0).size();

            MapParser.adjustCellSizes(gameBoard, columns, rows, cachedMapPlaceholderWidth, cachedMapPlaceholderHeight);
            MapParser.renderGameWithRobots(gameBoard, client);

            // Add a listener for window size changes
            addWindowResizeListener(rows, columns);
        });
    }

    private void addWindowResizeListener(int rows, int columns) {
        Platform.runLater(() -> {
            Stage stage = (Stage) gameBoard.getScene().getWindow();

            // Add listeners to the width and height properties of the window
            stage.widthProperty().addListener((observable, oldValue, newValue) -> {
                logger.info("Window resized - Old Width: " + oldValue + ", New Width: " + newValue);
                logger.info("Stage Width: " + stage.getWidth());

                // Calculate the width change ratio
                double widthChangeRatio = newValue.doubleValue() / oldValue.doubleValue();
                logger.info("Calculated widthChangeRatio: " + widthChangeRatio);

                // Calculate the total width of the parent GridPane's columns
                GridPane parentGridPane = (GridPane) mapPlaceholder.getParent();
                double totalColumnWidth = parentGridPane.getColumnConstraints().stream()
                        .mapToDouble(column -> column.getPrefWidth() > 0 ? column.getPrefWidth() : column.getMaxWidth())
                        .sum();
                logger.info("Total Column Width: " + totalColumnWidth);

                // Use the minimum of the cached width and total column width
                double oldCachedWidth = cachedMapPlaceholderWidth;
                //cachedMapPlaceholderWidth = Math.min(cachedMapPlaceholderWidth * widthChangeRatio, totalColumnWidth);
                cachedMapPlaceholderWidth = cachedMapPlaceholderWidth * widthChangeRatio;
                logger.info("Updated cachedMapPlaceholderWidth - Old: " + oldCachedWidth + ", New: " +
                            cachedMapPlaceholderWidth);

                adjustCellSizes(rows, columns);

                double currentStageWidth = stage.getWidth();
                addMargin(currentStageWidth);
                resizeRegisterCards(currentStageWidth);
                resizeWidthCheckpoint(currentStageWidth);


            });

            stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                logger.info("Window resized - Old Height: " + oldValue + ", New Height: " + newValue);
                logger.info("Stage Height: " + stage.getHeight());

                double heightChangeRatio = newValue.doubleValue() / oldValue.doubleValue();
                logger.info("Calculated heightChangeRatio: " + heightChangeRatio);

                double oldCachedHeight = cachedMapPlaceholderHeight;
                cachedMapPlaceholderHeight = cachedMapPlaceholderHeight * heightChangeRatio;
                logger.info("Updated cachedMapPlaceholderHeight - Old: " + oldCachedHeight + ", New: " +
                            cachedMapPlaceholderHeight);

                adjustCellSizes(rows, columns);
                double currentStageHeigth = stage.getHeight();
                resizeHeigthCheckpoint(currentStageHeigth);


            });
        });
    }

    private void resizeWidthCheckpoint(double currentStageWidth) {
        int cSW = (int) Math.round(currentStageWidth);

        int widthCategory = (cSW > 2200) ? 9 : (cSW > 2000) ? 8 : (cSW > 1800) ? 7 : (cSW > 1620) ? 6 :
                (cSW > 1500) ? 5 : (cSW > 1350) ? 4 : (cSW > 1200) ? 3 : (cSW > 1110) ? 2 : (cSW > 900) ? 1 :
                        (cSW > 700) ? 0 : -1;

        GridPane gridPane = (GridPane) checkpointInfoHolder.getChildren().get(0);

        switch (widthCategory) {
            case 9 -> gridPane.setHgap(60);
            case 8 -> gridPane.setHgap(47);
            case 7 -> gridPane.setHgap(42);
            case 6 -> {
                gridPane.setHgap(37);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(31);
                        rect.setHeight(29);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(47);
                    robotImage.setFitHeight(47);
                }
            }
            case 5 -> {
                gridPane.setHgap(27);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(29);
                        rect.setHeight(27);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(47);
                    robotImage.setFitHeight(47);
                }
            }
            case 4 -> {
                gridPane.setHgap(21);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(27);
                        rect.setHeight(25);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(47);
                    robotImage.setFitHeight(47);
                }
            }
            case 3 -> {
                gridPane.setHgap(15);
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(45);
                    robotImage.setFitHeight(45);
                }
            }
            case 2 -> {
                gridPane.setHgap(10);
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(40);
                    robotImage.setFitHeight(40);
                }
            }
            case 1 -> {
                gridPane.setHgap(7);
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(35);
                    robotImage.setFitHeight(35);
                }
            }
            default -> gridPane.setHgap(0);
        }


    }

    private void resizeHeigthCheckpoint(double currentStageHeigth) {


        int cSH = (int) Math.round(currentStageHeigth);

        int heightCategory = (cSH > 1330) ? 9 : (cSH > 1270) ? 8 : (cSH > 1170) ? 7 : (cSH > 1100) ? 6 :
                (cSH > 1050) ? 5 : (cSH > 980) ? 4 : (cSH > 940) ? 3 : (cSH > 900) ? 2 : (cSH > 850) ? 1 : 0;

        GridPane gridPane = (GridPane) checkpointInfoHolder.getChildren().get(0);

        switch (heightCategory) {
            case 9 -> gridPane.setVgap(50);
            case 8 -> gridPane.setVgap(45);
            case 7 -> {
                gridPane.setVgap(35);
            }
            case 6 -> {
                gridPane.setVgap(28);
            }
            case 5 -> {
                gridPane.setVgap(20);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(35);
                        rect.setHeight(32);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(56);
                    robotImage.setFitHeight(56);
                }
            }
            case 4 -> {
                gridPane.setVgap(15);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(33);
                        rect.setHeight(31);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(54);
                    robotImage.setFitHeight(54);
                }
            }
            case 3 -> {
                gridPane.setVgap(11);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(31);
                        rect.setHeight(29);
                    }//27, 25
                }
                for (ImageView robotImage : robotImageViews.values()) {
                    robotImage.setFitWidth(52);
                    robotImage.setFitHeight(52);
                }
            }
            case 2 -> {
                gridPane.setVgap(6);
                for (List<Rectangle> rectangles : checkpointRectanglesByColumn.values()) {
                    for (Rectangle rect : rectangles) {
                        rect.setWidth(29);
                        rect.setHeight(27);
                    }//27, 25
                }
            }
            case 1 -> gridPane.setVgap(3);
            default -> gridPane.setVgap(0);
        }
    }


    private void addMargin(double currentStageWidth) {
        //set margin
        logger.info(currentStageWidth + "currenstagewidth");
        double leftMargin = 0;
        if (currentStageWidth > 1800) {
            leftMargin = 200;
        } else if (currentStageWidth > 1650) {
            leftMargin = 100;
        } else if (currentStageWidth > 1350) {
            leftMargin = 50;
        } else if (currentStageWidth > 1200) {
            leftMargin = 40;
        } else if (currentStageWidth > 1100) {
            leftMargin = 30;
        }
        mapPlaceholder.setPadding(new Insets(10, 0, 10, leftMargin));
    }

    private void resizeRegisterCards(double currentStageWidth) {
        if (currentStageWidth > 1470) {
            for (ImageView card : registerSlots) {
                card.fitWidthProperty().bind(registerCardHolder.widthProperty().divide(6));
                card.fitHeightProperty().bind(registerCardHolder.heightProperty().subtract(10));
                for(ImageView uC : upgradeCardsSlots){
                    uC.fitWidthProperty().bind(registerCardHolder.widthProperty().divide(8));
                    uC.fitHeightProperty().bind(registerCardHolder.heightProperty().subtract(20));
                }
            }
        } else if (currentStageWidth < 1111) {
            for (ImageView card : registerSlots) {
                card.fitWidthProperty().bind(registerCardHolder.widthProperty().divide(7));
                card.fitHeightProperty().bind(registerCardHolder.heightProperty().subtract(15));
            }
            for(ImageView uC : upgradeCardsSlots){
                uC.fitWidthProperty().bind(registerCardHolder.widthProperty().divide(9));
                uC.fitHeightProperty().bind(registerCardHolder.heightProperty().subtract(5));
            }

        }
    }

    private void adjustCellSizes(int rows, int columns) {
        logger.info("Adjusting cell sizes with Cached Width: " + cachedMapPlaceholderWidth + ", Cached Height: " +
                    cachedMapPlaceholderHeight);

        // Calculate the new cell size
        double cellWidth = (cachedMapPlaceholderWidth * 0.8) / columns;
        double cellHeight = (cachedMapPlaceholderHeight * 0.8) / rows;
        double newCellSize = Math.min(cellWidth, cellHeight); // Keep cells square

        // Adjust the size of cells
        MapParser.adjustCellSizes(gameBoard, columns, rows, cachedMapPlaceholderWidth, cachedMapPlaceholderHeight);

        // Adjust the size of robots
        gameBoard.getChildren().forEach(node -> {
            if (node instanceof ImageView imageView) {
                // Update the size of robot images
                imageView.setFitWidth(newCellSize);
                imageView.setFitHeight(newCellSize);
            }
        });

        logger.info("Adjusted robot image sizes to new cell size: " + newCellSize);
    }

    public void setChat(Parent chatViewRoot, ChatViewController chatCtrl) {
        chatPlaceHolder.getChildren().clear();
        chatPlaceHolder.getChildren().add(chatViewRoot);
        this.chatViewController = chatCtrl;
    }

    //KARTEN ANZEIGEN
    // Add a listener to notify when cardsInHand changes
    public void setCardsInHandListener() {
        gameDataBridge.getCardsInHand().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {

                if (gameDataBridge.getCardsInHand().size() == 9) {
                    displayCards();
                }

            }
        });

    }

    //NEU
    public void setRandomCardsListener() {
        gameDataBridge.getRandomCardsForRegister().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                displayRandomCardsInRegister();

            }
        });
    }

    private void setSelectedCardsListener() {
        gameDataBridge.getSelectedCards().addListener((MapChangeListener<? super Integer, ? super String>) change -> {
            Platform.runLater(() -> {
                int selectedCount = gameDataBridge.getSelectedCards().size();
                if (selectedCount == 5) {

//                    startSelectionTimer();
//                    startCountdownSelectedCards();

//                } else {
//                    cancelSelectionTimer();
                }
            });
        });
    }

    /**
     * Displays cards in hand by loading their corresponding images into card slots.
     *
     * <p>Retrieves card names from {@code gameDataBridge}, loads their images with
     * {@code loadCardImage}, and assigns them to the appropriate {@code cardSlots}.</p>
     *
     * @throws ArrayIndexOutOfBoundsException if more cards exist than slots.
     */
    public void displayCards() {
        Image register = new Image(
                getClass().getResource("/images/general/cards/DefaultRegister.png").toExternalForm());
        List<String> cardsInHand = gameDataBridge.getCardsInHand(); //Strings der 9 Karten
        if ((gameDataBridge.getPhaseValue() == 2) && (!cardsInHand.isEmpty())) {
            for (ImageView imageView : registerSlots) {
                imageView.setImage(register);
                gameDataBridge.getSelectedCards().clear();
                imageView.setUserData(null);
            }
        }

        for (int i = 0; i < 9; i++) {
            if (cardsInHand.isEmpty()) {
                cardSlots[i].setImage(register);
                cardSlots[i].setUserData(null);
            } else {
                String cardName = cardsInHand.get(i); //übergebene Karten die angezeigt werden sollen
                Image cardImage = loadCardImage(cardName); //korrespondierendes Bild
                cardSlots[i].setImage(cardImage);
                cardSlots[i].setUserData(cardName);

                if (cardImage != null) {
                    cardSlots[i].setImage(cardImage); //laden des Bildes (cardImage) in das jeweilige Register
                    cardSlots[i].setDisable(false);
                    cardSlots[i].setVisible(true);
                }
            }
        }
    }

    private Image loadCardImage(String cardName) {
        try {
            String cardPath = CardImageMapper.getImagePath(cardName);
            URL pngUrl = getClass().getResource(cardPath);
            return new Image(pngUrl.toExternalForm());
        } catch (Exception e) {
            logger.severe("Error loading card image: " + cardName);
            return null;
        }
    }

    @FXML
    private ImageView Card0, Card1, Card2, Card3, Card4, Card5, Card6, Card7, Card8;

    //DRAG AND DROP

    @FXML
    private void handleDragOver(DragEvent event) {
        //logger.info("---------- TEST Hovering over target: " + event.getGestureTarget());
        if (event.getDragboard().hasImage()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    private void handleDragDetection(MouseEvent event) {

        int currentPhase = gameDataBridge.getPhase().get();
        if (gameDataBridge.getSelectedCards().size() == 5) {
            gameDataBridge.setErrorMessage("TOO LATE", -1);
        }
        if (currentPhase == 3) {
            //TODO also when cards are chosen, 30 sec timer
            gameDataBridge.setErrorMessage("TOO LATE", -1);
            event.consume();
            return;
        }
        if (dragDisabled) {
            gameDataBridge.setErrorMessage("GO BACK TO YOUR REGISTER PLEASE", -1);

            event.consume();
            return;
        }
        source = (ImageView) event.getSource(); // Karten Weitergabe (Logik)
        String cardName = (String) source.getUserData();

        if (cardName == null) {
            logger.warning("Card name is null");
            return;
        }

        Dragboard dragboard = source.startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        content.putImage(source.getImage());

        Image originalImage = loadCardImage(cardName);
        double scaledWidth = 50;
        double scaledHeight = 150;
        Image scaledImage = new Image(originalImage.getUrl(), scaledWidth, scaledHeight, true, true);
        dragboard.setDragView(scaledImage, scaledWidth, scaledHeight);

        if (cardName != null) {
            content.putString(cardName);
        }

        dragboard.setContent(content);
        event.consume();
    }

    @FXML
    private void handleDropInRegister(DragEvent event) {
        if (gameDataBridge.isDragDisabled()) {
            gameDataBridge.setErrorMessage("TOO LATE", -1);
            event.consume();
            return;
        }
        Dragboard dragboard = event.getDragboard();
        String cardName = dragboard.getString();


        if (dragboard.hasImage()) {
            Image draggedImage = dragboard.getImage();
            Object target = event.getGestureTarget();

            int oldSlotIndex = identifySlotIndex(source);
            boolean sourceWasRegister = oldSlotIndex != -1;
            if (target instanceof ImageView targetImageView) {
                int slotIndex = identifySlotIndex(targetImageView);
                if (sourceWasRegister && oldSlotIndex != slotIndex) {
                    if (targetImageView.getUserData() == null) {
                        if (slotIndex == 0 && "Again".equals(cardName)) {
                            gameDataBridge.setErrorMessage("AGAIN IN FIRST REGISTER? \n\n NO.", -1);
                        } else {
                            fromRegisterToRegister(oldSlotIndex, cardName, slotIndex);
                            targetImageView.setImage(draggedImage);
                            targetImageView.setUserData(cardName);
                            logger.info("Moved card " + cardName + " from register " + oldSlotIndex + " to " + slotIndex);
                        }
                    }

                } else {
                    if (slotIndex == 0 && "Again".equals(cardName)) {

                        gameDataBridge.setErrorMessage("AGAIN IN FIRST REGISTER? \n NO.", -1);
                    } else if (slotIndex != -1) {
                        if (targetImageView.getUserData() == null) {
                            targetImageView.setImage(draggedImage);
                            targetImageView.setUserData(cardName);
                            updateDataStructure(source, targetImageView, cardName, slotIndex, true);
                        }
                    }
                }

                event.setDropCompleted(true);
            }
            event.consume();
        }
    }

    private void displayRandomCardsInRegister() {
        for (int i = 0; i < registerSlots.length; i++) {
            ImageView imageView = registerSlots[i];

            if (i < gameDataBridge.getRandomCardsForRegister().size()) {
                String cardName = gameDataBridge.getRandomCardsForRegister().get(i);
                String url = CardImageMapper.getImagePath(cardName);

                if (url != null) {
                    imageView.setImage(new Image(getClass().getResource(url).toExternalForm()));
                }
            }
        }
    }

    @FXML
    private void handleDropInHand(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        String cardName = dragboard.getString();

        if (dragboard.hasImage()) {
            Image draggedImage = dragboard.getImage();
            ImageView targetImageView = (ImageView) event.getGestureTarget();
            int slotIndex = identifySlotIndex(source);
            if ((slotIndex) != -1 && (targetImageView.getUserData() == null)) {
                targetImageView.setImage(draggedImage); //vorübergehend TODO ändern
                targetImageView.setUserData(cardName);
                updateDataStructure(source, targetImageView, cardName, slotIndex, false);
            }
            event.setDropCompleted(true);
            event.consume();
        }
    }

    private void updateDataStructure(ImageView source, ImageView target, String cardName, int slotIndex, boolean toRegister) {
        if (toRegister) {
            gameDataBridge.setCardInSlot(slotIndex, cardName);
            //logger.info(cardName + slotIndex + " slotindex");
            notifySelectedCard(cardName, slotIndex);
            source.setUserData(null);
            source.setImage(
                    new Image(getClass().getResource("/images/general/cards/DefaultHandCard.png").toExternalForm()));
        } else { //zurück in die Hand
            source.setUserData(null);
            gameDataBridge.removeCardFromSlot(slotIndex);
            //logger.info("null " + slotIndex + " slotindex");
            notifySelectedCard("null", slotIndex);
            source.setImage(
                    new Image(getClass().getResource("/images/general/cards/DefaultRegister.png").toExternalForm()));
        }
    }

    private void fromRegisterToRegister(int oldSlotIndex, String cardName, int slotIndex) {
        logger.info("selected cards  " + gameDataBridge.getSelectedCards());
        logger.info("from register to register");
        notifySelectedCard("null", oldSlotIndex);
        gameDataBridge.removeCardFromSlot(oldSlotIndex);
        source.setImage(
                new Image(getClass().getResource("/images/general/cards/DefaultRegister.png").toExternalForm()));
        source.setUserData(null);

        logger.info("selected cards  " + gameDataBridge.getSelectedCards());

        notifySelectedCard(cardName, slotIndex);
        gameDataBridge.setCardInSlot(slotIndex, cardName);

    }

    private int identifySlotIndex(ImageView targetImageView) {
        if (targetImageView.getId().equals("registerCard0")) {
            return 0;
        }
        if (targetImageView.getId().equals("registerCard1")) {
            return 1;
        }
        if (targetImageView.getId().equals("registerCard2")) {
            return 2;
        }
        if (targetImageView.getId().equals("registerCard3")) {
            return 3;
        }
        if (targetImageView.getId().equals("registerCard4")) {
            return 4;
        }
        return -1;
    }

    private String getPhaseString(int phase) {
        switch (phase) {
            case 0:
                return "SETUP";
            case 1:
                return "UPGRADE";
            case 2:
                return "PROGRAMMING";
            case 3:
                return "ACTIVATION";
            default:
                return null;
        }
    }


    private void updateLabel(String i) {
        phaseLabel.setText(i + "\n\nPHASE");
    }

    public String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        String[] parts = url.split("/");
        String fileNameWithExtension = parts[parts.length - 1];

        // Entfernt die Dateiendung (falls gewünscht)
        int dotIndex = fileNameWithExtension.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileNameWithExtension.substring(0, dotIndex);
        }

        return fileNameWithExtension;
    }

    private void buildCheckpointsDisplay(int playerCount, int checkpointCount) {
        GridPane checkpointsGrid = createGridPane();

        addColumnConstraints(checkpointsGrid, playerCount);
        addRowConstraints(checkpointsGrid, checkpointCount);
        checkpointRectanglesByColumn.clear();

        setCheckpointsGrid(checkpointsGrid, playerCount, checkpointCount);
        setRobotImages(checkpointsGrid, playerCount, checkpointCount);

        checkpointInfoHolder.getChildren().clear();
        checkpointInfoHolder.getChildren().add(checkpointsGrid);
    }

    private GridPane createGridPane() {
        GridPane grid = new GridPane();
        grid.setAlignment(javafx.geometry.Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(0);
        return grid;
    }

    private void addColumnConstraints(GridPane grid, int playerCount) {
        ColumnConstraints cpNumCol;
        if (playerCount < 5) {
            cpNumCol = new ColumnConstraints(40); // Erste Spalte für Checkpoint-Zahlen
        } else {
            cpNumCol = new ColumnConstraints(25);
        }

        grid.getColumnConstraints().add(cpNumCol);

        for (int p = 0; p < playerCount; p++) {
            ColumnConstraints playerCol = new ColumnConstraints();
            playerCol.setHgrow(Priority.ALWAYS);  // Spalten sollen sich gleichmäßig anpassen
            playerCol.setPercentWidth(100.0 / (playerCount + 1)); // Gleichmäßig verteilen
            playerCol.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(playerCol);
        }
    }


    private void addRowConstraints(GridPane grid, int checkpointCount) {
        for (int i = 0; i < checkpointCount; i++) {
            RowConstraints rowC = new RowConstraints(30);
            grid.getRowConstraints().add(rowC);
        }
    }

    private void setCheckpointsGrid(GridPane grid, int playerCount, int checkpointCount) {
        for (int row = 0; row < checkpointCount; row++) {
            addCheckpointLabel(grid, row, checkpointCount);
            addCheckpointRectangles(grid, row, playerCount);
        }
    }

    private void addCheckpointLabel(GridPane grid, int row, int checkpointCount) {
        int checkpointNumber = checkpointCount - row;
        Label cpLabel = new Label(String.valueOf(checkpointNumber));
        cpLabel.setStyle("-fx-text-fill: #bbe6ff; -fx-alignment: center; -fx-font-weight: bold");
        cpLabel.setMinWidth(40);
        cpLabel.setMaxWidth(Double.MAX_VALUE);
        cpLabel.setAlignment(javafx.geometry.Pos.CENTER);
        grid.add(cpLabel, 0, row);
    }

    private void addCheckpointRectangles(GridPane grid, int row, int playerCount) {
        for (int col = 1; col <= playerCount; col++) {
            Rectangle rect = createCheckpointRectangle();
            grid.add(rect, col, row);
            GridPane.setHalignment(rect, HPos.CENTER);
            GridPane.setValignment(rect, javafx.geometry.VPos.CENTER);

            checkpointRectanglesByColumn.computeIfAbsent(col, k -> new ArrayList<>()).add(rect);
        }
    }

    private Rectangle createCheckpointRectangle() {
        Rectangle rect = new Rectangle(27, 25);
        rect.setArcWidth(0);
        rect.setArcHeight(0);
        rect.setFill(Color.web("#3F3F3F"));
        rect.setStroke(Color.web("#BBE6FF"));
        rect.setStrokeWidth(2);
        return rect;
    }

    private void setRobotImages(GridPane grid, int playerCount, int checkpointCount) {
        final ImageView[] currentlyClickedRobot = {null};

        for (int col = 1; col <= playerCount; col++) {
            int clientID = getClientIDForPlayerColumn(col);
            ImageView robotImage = createRobotImage(col, currentlyClickedRobot);
            grid.add(robotImage, col, checkpointCount);
            GridPane.setHalignment(robotImage, HPos.CENTER);
            GridPane.setValignment(robotImage, javafx.geometry.VPos.CENTER);

            if (clientID == this.client.getClientID()) {
                DropShadow clickShadow = createShadowEffect("#77C8FF", 20, 0.5);
                robotImage.setEffect(clickShadow);
                currentlyClickedRobot[0] = robotImage;
            }
        }
    }

    private ImageView createRobotImage(int col, ImageView[] currentlyClickedRobot) {
        ImageView robotImage = new ImageView();
        int clientID = getClientIDForPlayerColumn(col);
        if (clientID != -1) {
            updateRobotImage(robotImage, clientID);
            robotImageViews.put(clientID, robotImage);
        }

        configureRobotImage(robotImage);
        setRobotImageInteractions(robotImage, clientID, currentlyClickedRobot);

        return robotImage;
    }

    private void configureRobotImage(ImageView robotImage) {
        robotImage.setFitWidth(45);
        robotImage.setFitHeight(45);
        robotImage.setPreserveRatio(true);
        robotImage.setSmooth(true);
        robotImage.setCache(true);
        robotImage.setRotate(180);
        robotImage.setEffect(createShadowEffect("#BBE6FF", 20, 0.5));
    }

    private DropShadow createShadowEffect(String color, int radius, double spread) {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(color));
        shadow.setRadius(radius);
        shadow.setSpread(spread);
        return shadow;
    }

    private void setRobotImageInteractions(ImageView robotImage, int clientID, ImageView[] currentlyClickedRobot) {
        DropShadow normalShadow = createShadowEffect("#BBE6FF", 20, 0.5);
        DropShadow clickShadow = createShadowEffect("#77C8FF", 20, 0.5);
        robotImage.setOnMouseEntered(event -> {
            if (currentlyClickedRobot[0] != robotImage) {
                robotImage.setEffect(clickShadow);
            }
            robotImage.getScene().setCursor(Cursor.HAND);
        });

        robotImage.setOnMouseExited(event -> {
            if (currentlyClickedRobot[0] != robotImage) {
                robotImage.setEffect(normalShadow);
            }
            robotImage.getScene().setCursor(Cursor.DEFAULT);
        });

        robotImage.setOnMouseClicked(event -> {
            if (currentlyClickedRobot[0] != null) {
                currentlyClickedRobot[0].setEffect(normalShadow);
            }
            currentlyClickedRobot[0] = robotImage;
            robotImage.setEffect(clickShadow);
            handleRobotClicked(clientID);
        });
    }

    public void sendSelectedCards(Map<Integer, String> selectedCards) {
        try {
            for (Map.Entry<Integer, String> entry : selectedCards.entrySet()) {
                int registerIndex = entry.getKey();
                String cardName = entry.getValue();

                notifySelectedCard(cardName, registerIndex);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.severe("Error sending the selected cards: " + e.getMessage());
        }
    }

    public void updateEnergyDisplay(int energyLevel) {
        energyCountLabel.setText(String.valueOf(energyLevel));
    }

    private void bindEnergyUpdates() {
        gameDataBridge.getClientIDToEnergy()
                      .addListener((MapChangeListener<? super Integer, ? super Integer>) change -> {
                          if (change.wasAdded() && client != null && change.getKey().equals(client.getClientID())) {
                              Platform.runLater(() -> updateEnergyDisplay(change.getValueAdded()));
                          }
                      });
    }

    private void updateNameLabel(int clientID) {
        String playerName = gameDataBridge.getIdToPlayerNameMap().get(clientID);
        if (playerName != null) {
            Platform.runLater(() -> playerNameLabel.setText(playerName.toUpperCase()));
        }
    }

    private void updateRobotImage(ImageView robotImage, int clientID) {
        Integer robotFigure = gameDataBridge.getClientIDToFigure().get(clientID);
        if (robotFigure != null) {
            String robotName = getRobotName(robotFigure);
            String imagePath = "/images/general/robots/" + robotName + ".png";
            URL robotUrl = getClass().getResource(imagePath);
            Image robot = new Image(robotUrl.toExternalForm());
            robotImage.setImage(robot);
        }
    }

    private String getRobotName(int robotId) {
        robotIdToName.put(0, "ZoomBot");
        robotIdToName.put(1, "HammerBot");
        robotIdToName.put(2, "HulkX90");
        robotIdToName.put(3, "SmashBot");
        robotIdToName.put(4, "SpinBot");
        robotIdToName.put(5, "Twonky");
        return robotIdToName.get(robotId);
    }

    private int getClientIDForPlayerColumn(int col) {
        if (col - 1 < playerClientIDs.size()) {
            return playerClientIDs.get(col - 1);
        }
        return -1;
    }

    private void updateCheckpointRectangles(int clientId, int checkpointCountReached) {
        int columnIndex = playerClientIDs.indexOf(clientId) + 1; //col=0 is the label column

        List<Rectangle> checkpointRects = checkpointRectanglesByColumn.get(columnIndex);
        for (Rectangle r : checkpointRects) {
            r.setFill(Color.web("#3F3F3F"));
        }
        int totalCheckpoints = checkpointRects.size();
        for (int i = 0; i < checkpointCountReached; i++) {
            int rowFromBottom = totalCheckpoints - 1 - i;
            if (rowFromBottom >= 0 && rowFromBottom < checkpointRects.size()) {
                checkpointRects.get(rowFromBottom).setFill(Color.web("#BBE6FF"));
            }
        }
    }

    public void setGameOver(int winnerId) throws IOException {
        String playerName = gameDataBridge.getIdToPlayerNameMap().get(winnerId);
        int robotId = gameDataBridge.getClientIDToFigure().get(winnerId);
        String robotName = getRobotName(robotId);
        sceneManager.openGameOverPopUp(playerName, robotName);
    }

    private void notifySelectedCard(String cardName, int register) {
        SelectedCardMessage scm = new SelectedCardMessage(cardName, register);
        String jsonMessage = JsonHandler.toJson(scm);
        client.sendMessageToServer(jsonMessage);
    }

    private void handleRobotClicked(int otherPlayerID) {
        Map<Integer, Integer> robotMap = client.getGameDataBridge().getClientIDToFigure();
        selectedPlayerID = otherPlayerID;
        currentSelectedRobot = robotImageViews.get(otherPlayerID);
        int energy = gameDataBridge.getClientIDToEnergy().get(selectedPlayerID);

        if (selectedPlayerID == client.getClientID()) {
            playerNameLabel.setStyle("-fx-text-fill: #bbe6ff");
            Platform.runLater(() -> {
                Integer robot = robotMap.get(client.getClientID());
                applyStylesBasedOnRobot(robot);
                applyCheckpointStyleBasedOnRobot(robot);
                playerNameLabel.setText("YOUR CARDS");
                updateEnergyDisplay(energy);
                displaySelectedRegisters();
                updateUpgradeCards(client.getClientID());
                dragDisabled = false;

            });
        } else {
            playerNameLabel.setStyle("-fx-text-fill: #66a0bd");
            playerNameLabel.setStyle("-fx-text-fill: #bbdbea");
            updateViewForSelectedPlayer();
            updateNameLabel(selectedPlayerID);
            updateEnergyDisplay(energy);
            updateUpgradeCards(selectedPlayerID);
            dragDisabled = true;

        }

        Platform.runLater(() -> {
            Integer robot = robotMap.get(otherPlayerID);
            applyStylesBasedOnRobot(robot);
        });
    }


    /**
     * Applies the appropriate styles based on the robot ID.
     *
     * @param robot the robot ID
     */
    private void applyStylesBasedOnRobot(Integer robot) {
        if (robot == null) return;
        switch (robot) {
            case 0:
                register.setStyle("-fx-background-color: #00221b;");
                playerNameLabel.setStyle("-fx-text-fill: #2bc5a0");
                break;
            case 1:
                register.setStyle("-fx-background-color: #220035;");
                playerNameLabel.setStyle("-fx-text-fill: #a971cc");
                break;
            case 2:
                register.setStyle("-fx-background-color: #320000;");
                playerNameLabel.setStyle("-fx-text-fill: #d1504d");
                break;
            case 3:
                register.setStyle("-fx-background-color: #291a06;");
                playerNameLabel.setStyle("-fx-text-fill: #ddc36c");
                break;
            case 4:
                register.setStyle("-fx-background-color: #001b29;");
                playerNameLabel.setStyle("-fx-text-fill: #88cfdd");
                break;
            case 5:
                register.setStyle("-fx-background-color: #320013;");
                playerNameLabel.setStyle("-fx-text-fill: #c85a8c");
                break;
            default:
                logger.warning("Unknown robot ID: " + robot);
        }
    }

    private void applyCheckpointStyleBasedOnRobot(Integer robot) {
        if (robot == null) return;
        switch (robot) {
            case 0:
                checkpointsHolder.setStyle("-fx-background-color: #00221b;");
                break;
            case 1:
                checkpointsHolder.setStyle("-fx-background-color: #220035;");
                break;
            case 2:
                checkpointsHolder.setStyle("-fx-background-color: #320000;");
                break;
            case 3:
                checkpointsHolder.setStyle("-fx-background-color: #291a06;");
                break;
            case 4:
                checkpointsHolder.setStyle("-fx-background-color: #001b29;");
                break;
            case 5:
                checkpointsHolder.setStyle("-fx-background-color: #320013;");
                break;
            default:
                logger.warning("Unknown robot ID: " + robot);
        }
    }

    private void updateViewForSelectedPlayer() {
        if (selectedPlayerID < 0) {
            return;
        }

        if (currentPhase == 0 || currentPhase == 1 || currentPhase == 2) {
            showEmptyRegistersForOtherPlayer();
        } else if (currentPhase == 3) {
            updateActivationPhaseView(selectedPlayerID);
        }
    }

    private void showEmptyRegistersForOtherPlayer() {
        for (ImageView registerSlot : registerSlots) {
            registerSlot.setImage(new Image(getClass()
                    .getResource("/images/general/cards/DefaultRegister.png")
                    .toExternalForm()));
        }
    }

    private void displaySelectedRegisters() {
        Platform.runLater(() -> {
            Map<Integer, String> selectedCards = gameDataBridge.getSelectedCards();

            for (int i = 0; i < registerSlots.length; i++) {
                ImageView registerSlot = registerSlots[i];
                String cardName = selectedCards.get(i);

                if (cardName != null) {
                    Image cardImage = loadCardImage(cardName);
                    if (cardImage != null) {
                        registerSlot.setImage(cardImage);
                        registerSlot.setUserData(cardName);
                    }
                } else {
                    registerSlot.setImage(new Image(getClass()
                            .getResource("/images/general/cards/DefaultRegister.png")
                            .toExternalForm()));
                    registerSlot.setUserData(null);
                }
            }
        });
    }

    private void updateActivationPhaseView(int playerId) {
        if (playerId == client.getClientID()) {
            logger.info("Skipping updateActivationPhaseView for self");
            return;
        }
        ImageView[] registers = registerSlots;
        logger.info("registers " + registers.toString());
        List<String> playedCards = gameDataBridge.getPlayedCards().get(playerId);

        logger.info(playedCards + " playedCards " + playedCards.toString());
        int slotIndex = 0;
        for (String cardName : playedCards) {
            if ("spam".equalsIgnoreCase(cardName)) {
                logger.info("Skipping spam card");
                continue;
            }
            if (slotIndex < registers.length) {
                Image cardImage = loadCardImage(cardName);
                registers[slotIndex].setImage(cardImage);
                registers[slotIndex].setUserData(cardName);
                slotIndex++;
            }
        }
        for (int i = slotIndex; i < registers.length; i++) {
            registers[i].setImage(new Image(
                    getClass().getResource("/images/general/cards/DefaultRegister.png").toExternalForm()));
            registers[i].setUserData(null);
        }
    }

    private void resetActivationPhaseView() {
        gameDataBridge.resetPlayedCards();
        showEmptyRegistersForOtherPlayer();
    }

    private void managePhaseView() {
        if (currentPhase == 2) {
            dragDisabled = false;

            handPlaceholder.setVisible(true);
            handPlaceholder.setManaged(true);

            infoPlaceholder.setVisible(false);
            infoPlaceholder.setManaged(false);
        } else {
            dragDisabled = true;
            handPlaceholder.setVisible(false);
            handPlaceholder.setManaged(false);

            infoPlaceholder.setVisible(true);
            infoPlaceholder.setManaged(true);
        }
        if (currentPhase == 0) {
            infoBoxManager.updateInfoMessage(
                    gameDataBridge.getIdToPlayerNameMap().get(gameDataBridge.getCurrentPlayerID()), 0);
            updateRobotImage(infoImage, gameDataBridge.getCurrentPlayerID());
        } else if (currentPhase == 1 || currentPhase == 2) {
            for (Map.Entry<Integer, ImageView> entry : robotImageViews.entrySet()) {
                Integer someClientID = entry.getKey();
                ImageView robotView = entry.getValue();
                robotView.setRotate(180);
                updateRobotImage(robotView, someClientID);
            }
        }
    }

    public void resetRobotClicked() {
        Platform.runLater(() -> {
            ImageView ourRobot = robotImageViews.get(client.getClientID());

            if (ourRobot != null) {
                if (currentSelectedRobot != null && currentSelectedRobot != ourRobot) {
                    currentSelectedRobot.setEffect(createShadowEffect("#BBE6FF", 20, 0.5));
                }
                DropShadow clickShadow = createShadowEffect("#77C8FF", 20, 0.5);
                ourRobot.setEffect(clickShadow);
                currentSelectedRobot = ourRobot;
                handleRobotClicked(client.getClientID());
            } else {
                logger.warning("No robot ImageView found for client ID: " + client.getClientID());
            }
        });
    }

    private void updateRegisterImage(int clientId, int registerIndex, String newCard) {
        if (clientId == client.getClientID()) {
            Platform.runLater(() -> {
                Image cardImage = loadCardImage(newCard);
                if (registerIndex < registerSlots.length) {
                    ImageView registerSlot = registerSlots[registerIndex];
                    registerSlot.setImage(cardImage);
                    registerSlot.setUserData(newCard);
                    gameDataBridge.setCardInSlot(registerIndex, newCard);
                }
            });
        }
    }

    @FXML
    private void handleShopButton(MouseEvent event) {
        Platform.runLater(() -> {
            try {
                sceneManager.openUpgradeShopPopUp(client, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setDefaultImages() {
        permanentCard1.setImage(loadCardImage("UpgradePermanentBS"));
        permanentCard2.setImage(loadCardImage("UpgradePermanentBS"));
        permanentCard3.setImage(loadCardImage("UpgradePermanentBS"));
        temporaryCard1.setImage(loadCardImage("UpgradeTemporaryBS"));
        temporaryCard2.setImage(loadCardImage("UpgradeTemporaryBS"));
        temporaryCard3.setImage(loadCardImage("UpgradeTemporaryBS"));
    }

    @FXML
    private void onUpgradeClicked(MouseEvent event) {
        ImageView clickedCard = (ImageView) event.getSource();

        if (selectedUpgrade == clickedCard) {
            removeUpgradeEffect(clickedCard);
            selectedUpgrade = null;
        } else {
            if (selectedUpgrade != null) {
                removeUpgradeEffect(selectedUpgrade);
            }
            selectedUpgrade = clickedCard;
            addUpgradeEffect(clickedCard);
        }
    }

    private void addUpgradeEffect(ImageView cardImage) {
        DropShadow borderEffect = createShadowEffect("#BBE6FF", 4, 1);
        cardImage.setEffect(borderEffect);
    }

    private void removeUpgradeEffect(ImageView cardImage) {
        cardImage.setEffect(null);
    }

    private void openMemorySwap() {
        Platform.runLater(() -> {
            try {
                sceneManager.openMemorySwapPopUp(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void openAdminPrivilege() {
        Platform.runLater(() -> {
            try {
                sceneManager.openAdminPrivilege(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handlePlayButton(MouseEvent event) {
        if (selectedUpgrade != null) {
            String upCardToPlay = selectedUpgrade.getUserData().toString();
            if (upCardToPlay.equalsIgnoreCase("MemorySwap")) {
                if (currentPhase == 2) {
                    PlayCardMessage pcm = new PlayCardMessage(upCardToPlay);
                    String jsn = JsonHandler.toJson(pcm);
                    client.sendMessageToServer(jsn);
                    openMemorySwap();
                } else {
                    gameDataBridge.setErrorMessage("YOU CAN'T PLAY MEMORY SWAP NOW.", client.getClientID());
                }

            } else if (upCardToPlay.equalsIgnoreCase("AdminPrivilege")) {
                openAdminPrivilege();
            } else {
                logger.info("Played card: " + upCardToPlay + " " + client.getClientID());
                client.sendPlayCard(upCardToPlay);
                client.getGameDataBridge().addActivatedUpgrade(client.getClientID(), upCardToPlay);
                if ("temporary".equals(UpgradeCardTypeMapper.getType(upCardToPlay))) {
                    gameDataBridge.removeUpgradeCardForClient(client.getClientID(), upCardToPlay);
                    removeUpgradeEffect(selectedUpgrade);
                }
            }

        } else {
            gameDataBridge.setErrorMessage("CHOOSE AN UPGRADE CARD TO PLAY", client.getClientID());
        }
    }

    private void updateUpgradeCardUI(ObservableList<String> upgradeList) {
        int permanentIndex = 0;
        int temporaryIndex = 0;
        resetUpgradeCards();
        for (String upgrade : upgradeList) {
            String type = UpgradeCardTypeMapper.getType(upgrade);
            Image upgradeImage = loadCardImage(upgrade);

            if (upgradeImage == null) {
                continue;
            }

            if ("permanent".equalsIgnoreCase(type)) {
                if (permanentIndex == 0) {
                    permanentCard1.setImage(upgradeImage);
                    setUpgradeDataAndEffect(permanentCard1, upgrade);
                } else if (permanentIndex == 1) {
                    permanentCard2.setImage(upgradeImage);
                    setUpgradeDataAndEffect(permanentCard2, upgrade);
                } else if (permanentIndex == 2) {
                    permanentCard3.setImage(upgradeImage);
                    setUpgradeDataAndEffect(permanentCard3, upgrade);
                }
                permanentIndex++;
            } else if ("temporary".equalsIgnoreCase(type)) {
                if (temporaryIndex == 0) {
                    temporaryCard1.setImage(upgradeImage);
                    setUpgradeDataAndEffect(temporaryCard1, upgrade);
                } else if (temporaryIndex == 1) {
                    temporaryCard2.setImage(upgradeImage);
                    setUpgradeDataAndEffect(temporaryCard2, upgrade);
                } else if (temporaryIndex == 2) {
                    temporaryCard3.setImage(upgradeImage);
                    setUpgradeDataAndEffect(temporaryCard3, upgrade);
                }
                temporaryIndex++;
            }
        }
    }

    private void setUpgradeDataAndEffect(ImageView imageView, String cardName) {
        imageView.setUserData(cardName);
        imageView.setDisable(false);
    }

    private void disableUpgradeCards() {
        temporaryCard1.setDisable(true);
        temporaryCard2.setDisable(true);
        temporaryCard3.setDisable(true);
        permanentCard1.setDisable(true);
        permanentCard2.setDisable(true);
        permanentCard3.setDisable(true);
    }

    private void resetUpgradeCards() {
        setDefaultImages();
        temporaryCard1.setUserData(null);
        temporaryCard2.setUserData(null);
        temporaryCard3.setUserData(null);
        permanentCard1.setUserData(null);
        permanentCard2.setUserData(null);
        permanentCard3.setUserData(null);
    }

    private void updateUpgradeCards(int clientID) {
        ObservableList<String> upgradeCards = gameDataBridge.getClientIDToUpgradeCards().get(clientID);
        if (upgradeCards != null) {
            updateUpgradeCardUI(upgradeCards);
        }
    }

    private void updateRebootedRobotIcon(int clientID) {
        Platform.runLater(() -> {
            ImageView rebootedRobot = robotImageViews.get(clientID);
            if (rebootedRobot != null) {
                rebootedRobot.setImage(
                        new Image(getClass()
                                          .getResource("/images/general/robots/DeadRobot.png")
                                          .toExternalForm())
                );
            }
        });
    }

}
