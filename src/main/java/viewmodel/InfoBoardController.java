package viewmodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.server_client.Client;

/**
 * Controller for the information board UI.
 * <p>
 * This controller manages the table view displaying player information such as
 * player name, robot name, and readiness status.
 * </p>
 */
public class InfoBoardController {

    @FXML
    private TableView<PlayerInfo> infoBoard;
    @FXML
    private TableColumn<PlayerInfo, String> playerColumn;
    @FXML
    private TableColumn<PlayerInfo, String> robotColumn;
    @FXML
    private TableColumn<PlayerInfo, Boolean> statusColumn;
    @FXML
    private Label boardHeader;
    private Client client;
    private final ObservableList<PlayerInfo> data = FXCollections.observableArrayList();


    /**
     * Initializes the information board.
     * <p>
     * This method configures the table view, sets up column properties, cell factories,
     * and binds UI elements for proper resizing. It is automatically called after the FXML is loaded.
     * </p>
     */
    @FXML
    public void initialize() {
        VBox.setVgrow(infoBoard, javafx.scene.layout.Priority.ALWAYS);
        infoBoard.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        double totalColumns = 3;
        playerColumn.prefWidthProperty().bind(infoBoard.widthProperty().divide(totalColumns));
        robotColumn.prefWidthProperty().bind(infoBoard.widthProperty().divide(totalColumns));
        statusColumn.prefWidthProperty().bind(infoBoard.widthProperty().divide(totalColumns));

        boardHeader.prefWidthProperty().bind(infoBoard.widthProperty());
        infoBoard.getColumns().forEach(column -> {
            column.setReorderable(false);
            column.setResizable(false);
            column.setSortable(false);
        });
        infoBoard.setFocusTraversable(false);


        playerColumn.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        robotColumn.setCellValueFactory(new PropertyValueFactory<>("robotName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("ready"));

        statusColumn.setCellFactory(column -> new TableCell<PlayerInfo, Boolean>() {
            @Override
            protected void updateItem(Boolean ready, boolean empty) {
                super.updateItem(ready, empty);
                if (empty || ready == null) {
                    setText(null);
                } else {
                    setText(ready ? "Ready" : "Not Ready");
                }
            }
        });
        playerColumn.setStyle("-fx-alignment: CENTER;");
        robotColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setStyle("-fx-alignment: CENTER;");
    }

    /**
     * Sets the client for which the information board will display data.
     *
     * @param client the client instance containing the lobby data bridge.
     */
    public void setClient(Client client) {
        this.client = client;
        infoBoard.setItems(client.getLobbyDataBridge().getPlayerInfoList());
    }

    /**
     * Adds a new player or updates an existing player's information on the board.
     *
     * @param clientId   the unique identifier of the client.
     * @param playerName the player's name.
     * @param robotName  the name of the robot.
     * @param isReady    the readiness status of the player.
     */
    public void addOrUpdatePlayer(int clientId, String playerName, String robotName, boolean isReady) {
        PlayerInfo existing = findPlayerById(clientId);
        if (existing == null) {
            data.add(new PlayerInfo(clientId, playerName, robotName, isReady));
      } else {
            existing.setPlayerName(playerName);
            existing.setRobotName(robotName);
            existing.setReady(isReady);
        }
    }

    /**
     * Updates the readiness status of a player.
     *
     * @param clientId the unique identifier of the client.
     * @param isReady  the new readiness status.
     */
    public void updatePlayerStatus(int clientId, boolean isReady) {
        PlayerInfo existing = findPlayerById(clientId);
        if (existing != null) {
            existing.setReady(isReady);
         }
    }

    /**
     * Finds and returns the PlayerInfo for a given client ID.
     *
     * @param clientId the unique identifier of the client.
     * @return the corresponding PlayerInfo, or null if not found.
     */
    public PlayerInfo findPlayerById(int clientId) {
        for (PlayerInfo p : data) {
            if (p.getClientId() == clientId) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns the player name corresponding to the given client ID.
     *
     * @param clientId the unique identifier of the client.
     * @return the player's name if found; otherwise, null.
     */
    public String getPlayerNameById(int clientId) {
        PlayerInfo player = findPlayerById(clientId);
        return player != null ? player.getPlayerName() : null;
    }

    /**
     * Removes a player's information from the board using the client ID.
     *
     * @param clientId the unique identifier of the client.
     */
    public void removePlayerById(int clientId) {
        PlayerInfo existing = findPlayerById(clientId);
        if (existing != null) {
            data.remove(existing);
        }
    }
}

