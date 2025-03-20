package viewmodel;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Duration;
import model.server_client.Client;

import java.util.HashMap;
import java.util.Optional;

/**
 * Manages the data shared between the lobby UI and the game logic.
 * It maintains player information, game status, error messages, and
 * map selection states for all connected clients.
 */
public class LobbyDataBridge {
    private final ObservableList<PlayerInfo> playerInfoList = FXCollections.observableArrayList();
    private HashMap<Integer, String> idToPlayerNameMap = new HashMap<>();
    private ObservableMap<Integer, Integer> clientIDToFigure = FXCollections.observableHashMap();
    private SimpleBooleanProperty gameRunning = new SimpleBooleanProperty(false);
    private SimpleStringProperty errorMessage = new SimpleStringProperty("");
    private ObservableMap<Integer, Boolean> clientIDToMapSelection = FXCollections.observableHashMap();
    private SimpleBooleanProperty start = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty selectButton = new SimpleBooleanProperty(true);
    private SimpleBooleanProperty validChoice = new SimpleBooleanProperty(false);
    private SimpleStringProperty robotErrorMessage = new SimpleStringProperty("");

    public LobbyDataBridge() {
    }
    /**
     * Adds a player's ID and name mapping to the internal storage.
     *
     * @param clientID   The unique identifier of the client.
     * @param playerName The name of the player.
     */
    public void addIdToPlayerNameMap(int clientID, String playerName) {
        idToPlayerNameMap.put(clientID, playerName);
    }
    public void setValidChoice(boolean validChoice) {
        this.validChoice.set(validChoice); // Hier wird das Property aktualisiert
    }
    public SimpleBooleanProperty validChoiceProperty() {
        return validChoice;
    }
    public ObservableList<PlayerInfo> getPlayerInfoList() {
        return playerInfoList;
    }
    /**
     * Updates or adds a new player to the player list.
     *
     * @param clientId     The client's unique ID.
     * @param playerName   The name of the player.
     * @param figureNumber The number representing the player's robot.
     * @param isReady      Indicates if the player is ready.
     */
    public void addOrUpdatePlayer(int clientId, String playerName, int figureNumber, boolean isReady) {
        Optional<PlayerInfo> existingPlayer = playerInfoList.stream()
                .filter(p -> p.getClientId() == clientId)
                .findFirst();

        if (existingPlayer.isPresent()) {
            PlayerInfo pInfo = existingPlayer.get();
            pInfo.setPlayerName(playerName);
            pInfo.setRobotName(mapFigureNumberToName(figureNumber));
            pInfo.setReady(isReady);
            pInfo.setFigureNumber(figureNumber);

        } else {
            PlayerInfo newInfo = new PlayerInfo(
                    clientId,
                    playerName,
                    mapFigureNumberToName(figureNumber),
                    isReady
            );
            newInfo.setFigureNumber(figureNumber);
            playerInfoList.add(newInfo);
        }
    }

    public void setPlayerReady(int clientId, boolean ready) {
        for (PlayerInfo pInfo : playerInfoList) {
            if (pInfo.getClientId() == clientId) {
                pInfo.setReady(ready);
                break;
            }
        }
    }


    private String mapFigureNumberToName(int figureNumber) {
        if (figureNumber == -1) {
            return "Not Selected";
        }
        switch (figureNumber) {
            case 0: return "ZoomBot";
            case 1: return "HammerBot";
            case 2: return "HulkX90";
            case 3: return "SmashBot";
            case 4: return "SpinBot";
            case 5: return "Twonky";
            default: return "Unknown Robot";
        }
    }

    public void setGameRunning(boolean gameRunning) {
        this.gameRunning.set(gameRunning);
    }
    /**
     * Sets an error message to be displayed to all clients,
     * optionally associating it with a specific player.
     *
     * @param eMessage The error message.
     * @param clientID The ID of the client associated with the error (-1 if general).
     */
    public void setErrorMessage(String eMessage, int clientID) {
        String err;
        if(clientID != -1) {
            String name = idToPlayerNameMap.get(clientID);
            err = name.toUpperCase() + " " + eMessage;

        }else{
            err = eMessage.toUpperCase();
        }
        String finalErr = err;
        Platform.runLater(() -> {
            this.errorMessage.set(finalErr);

        });
        PauseTransition pause = new PauseTransition(Duration.seconds(7));
        pause.setOnFinished(event -> {
            this.errorMessage.set("");

        });
        setSelectButton(false);
        setSelectButton(true);
        pause.play();
    }

    public void setRobotErrorMessage(String eMessage, int clientID) {
        String err;
        if(clientID != -1) {
            String name = idToPlayerNameMap.get(clientID);
            err = name.toUpperCase() + " " + eMessage;

        }else{
            err = eMessage.toUpperCase();
        }
        String finalErr = err;
        Platform.runLater(() -> {
            this.robotErrorMessage.set(finalErr);

        });
        PauseTransition pause = new PauseTransition(Duration.seconds(7));
        pause.setOnFinished(event -> {
            this.robotErrorMessage.set("");

        });
        setSelectButton(false);
        setSelectButton(true);
        pause.play();
    }

    public SimpleStringProperty getErrorMessage() {
        return errorMessage;
    }

    public ObservableMap<Integer, Boolean> getClientIDToMapSelection() {
        return clientIDToMapSelection;
    }

    public void addClientIDToMapSelection(int clientID, boolean chooser) {
        this.clientIDToMapSelection.put(clientID, chooser);
    }

    public SimpleBooleanProperty startProperty() {
        return start;
    }

    public void setStart(boolean start) {
        this.start.set(start);
    }

    public void addClientIDToFigure(int clientID, int figure) {
        clientIDToFigure.put(clientID, figure);
    }

    public ObservableMap<Integer, Integer> getClientIDToFigure() {
        return clientIDToFigure;
    }
    public SimpleBooleanProperty selectButtonProperty() {
        return selectButton;
    }

    public void setSelectButton(boolean selectButton) {
        this.selectButton.set(selectButton);
    }

    public SimpleStringProperty robotErrorMessageProperty() {
        return robotErrorMessage;
    }
}