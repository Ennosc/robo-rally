package viewmodel;

import helpers.RobotModel;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.sql.SQLOutput;
import java.util.*;

import javafx.collections.ObservableMap;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import model.game.board.Direction;
import model.game.board.Position;
import network.messages.lobby3.GameStartedMessage;
import network.messages.phases7.activation.CurrentCardsMessage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Manages game state data and provides a bridge between the client and the UI.
 * This class handles player data, robot models, game phases, and various
 * game mechanics such as checkpoints, energy, upgrades, and damage.
 */
public class GameDataBridge {
    private ObservableList<String> playerNames = FXCollections.observableArrayList();
    private final HashMap<Integer, String> idToPlayerNameMap = new HashMap<>(); // key = id value = playername
    private final ObservableList<String> cardsInHand = FXCollections.observableArrayList();
    private final ObservableMap<Integer, String> selectedCards = FXCollections.observableHashMap();
    private final ObservableList<String> randomCardsForRegister = FXCollections.observableArrayList();
    private final ObservableList<String> availableDamageCards = FXCollections.observableArrayList();
    private final ObservableList<String> availableUpgradeCards = FXCollections.observableArrayList();
    private final ObservableList<CurrentCardsMessage.ActiveCard> currentCards = FXCollections.observableArrayList();
    private final ObservableMap<Integer, Position> availableStartingPoints = FXCollections.observableHashMap();
    private final SimpleIntegerProperty phase = new SimpleIntegerProperty();
    public ObservableMap<Integer, RobotModel> clientIDToRoboModel = FXCollections.observableHashMap();
    private final SimpleIntegerProperty checkpointNumber = new SimpleIntegerProperty();
    private final ObservableMap<Integer, Integer> clientIDToCheckpoint = FXCollections.observableHashMap();
    private final ObservableMap<Integer, Integer> clientIDToEnergy = FXCollections.observableHashMap();
    private final ObservableMap<Integer, Integer> clientIDToFigure = FXCollections.observableHashMap();
    private final SimpleIntegerProperty timerValue = new SimpleIntegerProperty(-1);
    private List<List<List<GameStartedMessage.Field>>> gameMap;
    private GridPane gameMapGridPane;
    private final BooleanProperty doneButtonDisabled = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty gameOver = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty currentPlayerID = new SimpleIntegerProperty();
    private final SimpleIntegerProperty winnerID = new SimpleIntegerProperty(-1);
    private final ObservableMap<Integer, ObservableList<String>> playedCards = FXCollections.observableHashMap();
    private final ObservableMap<Integer, ObservableList<String>> damageCards = FXCollections.observableHashMap();
    private final SimpleIntegerProperty rebootedClientID = new SimpleIntegerProperty(-1);
    private final SimpleStringProperty errorMessage = new SimpleStringProperty("");
    private final SimpleIntegerProperty pickDamageCount = new SimpleIntegerProperty(0);
    private final BooleanProperty pickDamageTriggered = new SimpleBooleanProperty(false);
    private final List<Integer> rebootedClientIDs = new ArrayList<>();
    private final ObservableList<Integer> deadRobots = FXCollections.observableArrayList();
    private final ObservableMap<Integer, Map<Integer, String>> replacedCard = FXCollections.observableHashMap();
    private final ObservableMap<Integer, ObservableList<String>> clientIDToBoughtUpgradeCards = FXCollections.observableHashMap();
    private final Map<Position, Direction> wallLaserMap = new HashMap<>();
    private final Map<Integer, List<String>> clientIDToactivatedUpgradesMap = new HashMap<>();
    private boolean adminPrivilegePlayed = false;
    private final SimpleIntegerProperty activeRegisterSlot = new SimpleIntegerProperty(-1);


    public void addActivatedUpgrade(int clientID, String upgrade) {
        clientIDToactivatedUpgradesMap.computeIfAbsent(clientID, _ -> new ArrayList<>()).add(upgrade);
    }

    public Map<Integer, List<String>> getClientIDToActivatedUpgradesMap() {
        return clientIDToactivatedUpgradesMap;
    }
    private ObservableMap<Position, ImageView> wallLaserNodes = FXCollections.observableHashMap();
    private final BooleanProperty dragDisabled = new SimpleBooleanProperty(false);
    private SimpleIntegerProperty registerIndex = new SimpleIntegerProperty(0);

    public ObservableMap<Integer, ObservableList<String>> getClientIDToBoughtUpgradeCards() {
        return clientIDToBoughtUpgradeCards;
    }


    public void removeActivatedUpgrade(int clientID, String upgrade) {
        List<String> upgrades = clientIDToactivatedUpgradesMap.get(clientID);
        if (upgrades != null) {
            upgrades.remove(upgrade);
            if (upgrades.isEmpty()) {
                clientIDToactivatedUpgradesMap.remove(upgrade);
            }
        }
    }

    public void addDeadRobot(int clientID) {
        deadRobots.add(clientID);
    }

    public synchronized void setGameMap(List<List<List<GameStartedMessage.Field>>> gameMap) {
        this.gameMap = gameMap;
    }

    public List<List<List<GameStartedMessage.Field>>> getGameMap() {
        return gameMap;
    }

    public ObservableMap<Integer, String> getSelectedCards() {
        return selectedCards;
    }

    public HashMap<Integer, String> getIdToPlayerNameMap() {
        return idToPlayerNameMap;
    }

    public ObservableList<String> getPlayers() {
        return playerNames;
    }

    public void addPlayer(String playerName, int playerId) {
        playerNames.add(playerName);
        playedCards.put(playerId, FXCollections.observableArrayList());
    }

    public GameDataBridge() {
        this.playerNames = FXCollections.observableArrayList();
    }

    public void setPlayers(ObservableList<String> playerNames) {
        this.playerNames = playerNames;
    }

    public ObservableList<String> getCardsInHand() {
        return cardsInHand;
    }

    public synchronized void setCardsInHand(List<String> cards) {

        clearCardsInHand();
        cardsInHand.addAll(cards);

    }

    public void clearCardsInHand() {
        cardsInHand.clear();
    }

    public ObservableList<String> getRandomCardsForRegister() {
        return randomCardsForRegister;
    }

    public void handleCardsYouGotNow(List<String> cards) {
        randomCardsForRegister.setAll(cards);
    }

    public synchronized void setAvailableStartingPoints(List<Position> startingPoints) {
        availableStartingPoints.clear();
        for (int i = 0; i < startingPoints.size(); i++) {
            availableStartingPoints.put(i + 1, startingPoints.get(i));
        }
    }

    public synchronized List<Position> getAvailableStartingPoints() {
        return new ArrayList<>(availableStartingPoints.values());
    }


    public synchronized void removeStartingPoint(int x, int y) {
        Position target = new Position(x, y);
        Optional<Integer> keyToRemove = availableStartingPoints.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(target))
                .map(Map.Entry::getKey)
                .findFirst();
        keyToRemove.ifPresent(availableStartingPoints::remove);
    }

    public ObservableList<String> getAvailableDamageCards() {
        return availableDamageCards;
    }

    public void setAvailableDamageCards(List<String> availableDamageCards) {
        this.availableDamageCards.clear();
        this.availableDamageCards.addAll(availableDamageCards);
    }

    public void setRandomCards(List<String> cards) {
        this.randomCardsForRegister.clear();
        this.randomCardsForRegister.addAll(cards);
    }

    public ObservableList<CurrentCardsMessage.ActiveCard> getCurrentCards() {
        return currentCards;
    }

    public void setCurrentCards(List<CurrentCardsMessage.ActiveCard> cards) {
        this.currentCards.clear();
        this.currentCards.setAll(cards);
    }

    public void updateRobotPosition(int clientID, int x, int y) {
        if(!(deadRobots.contains(clientID))){
        RobotModel robotModel = clientIDToRoboModel.get(clientID);

        if(robotModel != null){
            robotModel.setX(x);
            robotModel.setY(y);
        }
        }

    }

    public void setErrorMessage(String eMessage, int clientID) {
        String err;
        if(eMessage != null) {
            if (clientID != -1) {
                String name = idToPlayerNameMap.get(clientID);
                err = null;
                if (name != null) {
                    err = name.toUpperCase() + " " + eMessage;
                }


            } else {
                err = eMessage.toUpperCase();
            }
            String finalErr = err;
            Platform.runLater(() -> {
                this.errorMessage.set(finalErr);

            });
            PauseTransition pause = new PauseTransition(Duration.seconds(7));
            pause.setOnFinished(_ -> {
                this.errorMessage.set("");

            });
            pause.play();
        }
    }

    public SimpleStringProperty getErrorMessage() {
        return errorMessage;
    }

    public ObservableMap<Integer, RobotModel> getClientIDToRoboModelToRobotModel() {
        return clientIDToRoboModel;
    }

    public void removeClientIDToRoboModelToRobotModel(int clientId) {
        clientIDToRoboModel.remove(clientId);
    }

    public void addRobotModelToMap(int clientID, RobotModel robotModel) {
        clientIDToRoboModel.put(clientID, robotModel);
    }

    public void rotateRobotModel(int clientId, String rotation) {
        if (!deadRobots.contains(clientId)) {
            RobotModel robotModel = clientIDToRoboModel.get(clientId);
            Direction newDirection = robotModel.getDirection().rotate(rotation);
            robotModel.setDirection(newDirection);
        }
    }

    public void setRobotDirection(int clientID, Direction direction) {
        RobotModel robotModel = clientIDToRoboModel.get(clientID);
        robotModel.setDirection(direction);
    }

    public void setGameMapGridPane(GridPane gameMapGridPane) {
        this.gameMapGridPane = gameMapGridPane;
    }

    public GridPane getGameMapGridPane() {
        return gameMapGridPane;
    }

    public void setPhase(int phase) {
        this.phase.set(phase);
    }

    public SimpleIntegerProperty getPhase() {
        return this.phase;
    }

    public int getPhaseValue() {
        return this.phase.get();
    }

    //DRAG AND DROP

    public void setCardInSlot(int slotIndex, String cardName) {
        selectedCards.put(slotIndex, cardName);
    }

    public void removeCardFromSlot(int slotIndex) {
        selectedCards.remove(slotIndex);
    }

    public void setCheckpointNumber(int checkpointNumber) {
        this.checkpointNumber.set(checkpointNumber);
    }

    public int getCheckpointNumber() {
        return checkpointNumber.get();
    }

    public void setCurrentPlayerID(int playerID) {
        this.currentPlayerID.set(playerID);
    }

    public int getCurrentPlayerID() {
        return currentPlayerID.get();
    }

    public ObservableMap<Integer, Integer> getClientIDToCheckpoint() {
        return clientIDToCheckpoint;
    }

    public void addCurrentCheckpointToMap(int clientID, int currentCheckpoint) {
        this.clientIDToCheckpoint.put(clientID, currentCheckpoint);
    }

    public void updateCurrentCheckpoint(int clientID, int newCheckpoint) {
        this.clientIDToCheckpoint.put(clientID, newCheckpoint);
    }

    public void setEnergy(int clientID, int energy) {
        this.clientIDToEnergy.put(clientID, energy);
    }

    public int getEnergy(int clientID) {
        return this.clientIDToEnergy.get(clientID);
    }

    public ObservableMap<Integer, Integer> getClientIDToEnergy() {
        return clientIDToEnergy;
    }

    public void addClientIDToFigure(int clientID, int figure) {
        clientIDToFigure.put(clientID, figure);
    }

    public ObservableMap<Integer, Integer> getClientIDToFigure() {
        return clientIDToFigure;
    }

    public SimpleIntegerProperty timerValueProperty() {
        return timerValue;
    }

    public int getTimerValue() {
        return timerValue.get();
    }

    public void setTimerValue(int value) {
        timerValue.set(value);
    }

    public BooleanProperty doneButtonDisabledProperty() {
        return doneButtonDisabled;
    }

    public boolean isDoneButtonDisabled() {
        return doneButtonDisabled.get();
    }

    public void setDoneButtonDisabled(boolean disabled) {
        doneButtonDisabled.set(disabled);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver.set(gameOver);
    }

    public SimpleBooleanProperty gameOverProperty() {
        return gameOver;
    }

    public void setWinnerID(int winnerID) {
        this.winnerID.set(winnerID);
    }

    public int getWinnerID() {
        return winnerID.get();
    }

    public ObservableMap<Integer, ObservableList<String>> getPlayedCards() {
        return playedCards;
    }

    public void addPlayedCard(int playerId, String cardName) {
        playedCards.computeIfAbsent(playerId, _ -> FXCollections.observableArrayList()).add(cardName);
    }

    public void resetPlayedCards() {
        playedCards.values().forEach(ObservableList::clear);
    }

    public SimpleIntegerProperty currentPlayerIDProperty() {
        return currentPlayerID;
    }

    public String getLastPlayedCard(int clientId) {
        if(!(deadRobots.contains(clientId))) {
            ObservableList<String> cards = playedCards.get(clientId);
            if (cards != null && !cards.isEmpty()) {
                return cards.get(cards.size() - 1);
            }
        }
        return null;
    }

    public ObservableMap<Integer, ObservableList<String>> getDamageCards() {
        return damageCards;
    }

    public void addDamageCard(int playerId, List<String> cards) {
        Platform.runLater(() -> {
            ObservableList<String> playerDamageCards = damageCards.computeIfAbsent(playerId, _ -> FXCollections.observableArrayList());
            playerDamageCards.addAll(cards);
        });
    }

    public SimpleIntegerProperty rebootedClientIDProperty() {
        return rebootedClientID;
    }

    public void addRebootedClientID(int rebootedID) {
        this.rebootedClientIDs.add(rebootedID);
        Platform.runLater(() -> {
            // First set to a dummy value so the next set will fire a change event if the same player reboots again
            rebootedClientID.set(-1);
            rebootedClientID.set(rebootedID);
        });
    }

    public List<Integer> getRebootedClientIDs() {
        return this.rebootedClientIDs;
    }

    public void resetRebootedClientIDs() {
        this.rebootedClientIDs.clear();
    }

    public void removePlayerData(int clientId) {

        String name = idToPlayerNameMap.remove(clientId);
        if (name != null) {
            playerNames.remove(name);
        }
        clientIDToFigure.remove(clientId);
        clientIDToCheckpoint.remove(clientId);
        clientIDToEnergy.remove(clientId);
        playedCards.remove(clientId);
        damageCards.remove(clientId);
        clientIDToRoboModel.remove(clientId);

    }

    public int getPickDamageCount() {
        return pickDamageCount.get();
    }

    public void setPickDamageCount(int count) {
        this.pickDamageCount.set(count);
    }

    public SimpleIntegerProperty pickDamageCountProperty() {
        return pickDamageCount;
    }

    public boolean isPickDamageTriggered() {
        return pickDamageTriggered.get();
    }

    public void setPickDamageTriggered(boolean triggered) {
        this.pickDamageTriggered.set(triggered);
    }

    public BooleanProperty pickDamageTriggeredProperty() {
        return pickDamageTriggered;
    }

    public ObservableMap<Integer, Map<Integer, String>> getReplacedCard() {
        return replacedCard;
    }

    public void updateReplacedCard(int clientId, int registerIndex, String newCard) {
        Map<Integer, String> singleClientMap = new HashMap<>();
        singleClientMap.put(registerIndex, newCard);
        replacedCard.put(clientId, singleClientMap);
        javafx.application.Platform.runLater(() -> {
            replacedCard.remove(clientId);
        });
    }

    public ObservableList<String> getAvailableUpgradeCards() {
        return availableUpgradeCards;
    }

    public void setAvailableUpgradeCards(List<String> newUpgradeCards) {
        this.availableUpgradeCards.clear();
        this.availableUpgradeCards.addAll(newUpgradeCards);
    }

    public void addAvailableUpgradeCard(List<String> upgradeCards) {
        this.availableUpgradeCards.addAll(upgradeCards);
    }

    public ObservableMap<Integer, ObservableList<String>> getClientIDToUpgradeCards() {
        return this.clientIDToBoughtUpgradeCards;
    }

    public void addToWallLaserMap(Position position, Direction direction) {
        wallLaserMap.put(position, direction);
    }

    public Map<Position,Direction> getWallLaserMap() {
        return wallLaserMap;
    }

    public void removeAvailableUpgradeCard(String cardName) {
        this.availableUpgradeCards.remove(cardName);
    }

    public void setWallLaserMap(Map<Position, Direction> wallLaserMap) {
        this.wallLaserMap.clear();
        this.wallLaserMap.putAll(wallLaserMap);
    }

    public void addUpgradeCardForClient(int clientID, String upgradeCard) {
        ObservableList<String> list = clientIDToBoughtUpgradeCards.get(clientID);
        if (list == null) {
            list = FXCollections.observableArrayList();
            clientIDToBoughtUpgradeCards.put(clientID, list);
        }
        list.add(upgradeCard);
    }

    public void removeUpgradeCardForClient(int clientID, String upgradeCard) {
        ObservableList<String> personalUpgradeCards = clientIDToBoughtUpgradeCards.get(clientID);
        if (personalUpgradeCards != null) {
            personalUpgradeCards.remove(upgradeCard);
        }
    }

    public void setDragDisabled(boolean disabled){
        dragDisabled.set(disabled);
    }
    public boolean isDragDisabled(){
        return dragDisabled.get();
    }
    public boolean isAdminPrivilegePlayed() {
        return adminPrivilegePlayed;
    }
    public void setAdminPrivilegePlayed(boolean adminPrivilegePlayed) {
        this.adminPrivilegePlayed = adminPrivilegePlayed;
    }


    public SimpleIntegerProperty activeRegisterSlotProperty() {
        return activeRegisterSlot;
    }

    public int getActiveRegisterSlot() {
        return activeRegisterSlot.get();
    }

    public void setActiveRegisterSlot(int slotIndex) {
        activeRegisterSlot.set(slotIndex);
    }
    public int getCurrentRegisterIndex(){
        return registerIndex.get();
    }

    public void incrementRegisterIndex(){
        if (registerIndex.get() == 5) {
            registerIndex.set(0);
        }
        registerIndex.set(registerIndex.get() + 1);
    }

}
