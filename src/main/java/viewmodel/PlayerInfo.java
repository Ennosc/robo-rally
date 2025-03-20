package viewmodel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Model class to hold the player's data for the info board.
 */
import javafx.beans.property.*;

/**
 * Model class to hold the player's data for the info board.
 */
public class PlayerInfo {
    private final IntegerProperty clientId = new SimpleIntegerProperty();
    private final StringProperty playerName = new SimpleStringProperty();
    private final StringProperty robotName = new SimpleStringProperty();
    private final BooleanProperty ready = new SimpleBooleanProperty();
    private final IntegerProperty figureNumber = new SimpleIntegerProperty(-1);


    /**
     * Constructs a PlayerInfo instance.
     *
     * @param clientId The ID of the client.
     * @param playerName The name of the player.
     * @param robotName The name of the robot.
     * @param isReady The readiness status of the player.
     */
    public PlayerInfo(int clientId, String playerName, String robotName, boolean isReady) {
        this.clientId.set(clientId);
        this.playerName.set(playerName);
        this.robotName.set(robotName);
        this.ready.set(isReady);
    }

    public int getClientId() {
        return clientId.get();
    }

    public IntegerProperty clientIdProperty() {
        return clientId;
    }

    public String getPlayerName() {
        return playerName.get();
    }

    public void setPlayerName(String playerName) {
        this.playerName.set(playerName);
    }

    public StringProperty playerNameProperty() {
        return playerName;
    }

    public String getRobotName() {
        return robotName.get();
    }

    public void setRobotName(String robotName) {
        this.robotName.set(robotName);
    }

    public StringProperty robotNameProperty() {
        return robotName;
    }

    public boolean isReady() {
        return ready.get();
    }

    public void setReady(boolean ready) {
        this.ready.set(ready);
    }

    public BooleanProperty readyProperty() {
        return ready;
    }

    public int getFigureNumber() {
        return figureNumber.get();
    }

    public void setFigureNumber(int figureNumber) {
        this.figureNumber.set(figureNumber);
    }

    public IntegerProperty figureNumberProperty() {
        return figureNumber;
    }
}
