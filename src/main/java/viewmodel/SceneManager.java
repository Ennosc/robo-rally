package viewmodel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.server_client.Client;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Manages scene transitions and UI interactions in the game.
 * <p>
 * This class is responsible for switching between different game views
 * and handling popups.
 * </p>
 */
public class SceneManager {
    private final Stage stage;
    private final String serverHost;
    private final int serverPort;
    private Parent chatViewRoot;
    private ChatViewController chatViewController;
    private Parent infoBoardRoot;
    private InfoBoardController infoBoardController;
    private Client client;

    /**
     * Initializes the scene manager with the server's host and port.
     *
     * @param serverHost The server hostname.
     * @param serverPort The server port number.
     */
    public SceneManager(String serverHost, int serverPort) {
        this.stage = new Stage();
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        try {
            FXMLLoader chatLoader = new FXMLLoader(
                    getClass().getResource("/ServerClient/chatView.fxml")
            );
            chatViewRoot = chatLoader.load();
            chatViewController = chatLoader.getController();

            FXMLLoader infoBoardLoader = new FXMLLoader(
                    getClass().getResource("/ServerClient/infoBoard.fxml")
            );
            infoBoardRoot = infoBoardLoader.load();
            infoBoardController = infoBoardLoader.getController();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setClient(Client client) {
        this.client = client;
    }
    /**
     * Switches the scene to the robot selection view.
     * <p>
     * Loads the robot selection screen, initializes the controller, and sets up the chat UI.
     * </p>
     *
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void switchToSelectRobot() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/ServerClient/selectRobot.fxml"))
        );
        loader.setControllerFactory(param -> new SelectRobotController(serverHost, serverPort));

        Parent root = loader.load();
        SelectRobotController selectRobotController = loader.getController();
        selectRobotController.setSceneManager(this);
        selectRobotController.connectToServer();

        selectRobotController.setChatAndInfoBoard(
                chatViewRoot,
                chatViewController
        );

        infoBoardController.setClient(client);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    /**
     * Switches the scene to the map selection view.
     * <p>
     * Loads the map selection screen, initializes the controller, and displays the available maps.
     * </p>
     *
     * @param client    The client instance managing game data.
     * @param robotName The name of the selected robot.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void switchToSelectMap(Client client, String robotName) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/ServerClient/selectMap.fxml"))
        );
        Parent root = loader.load();

        SelectMapController selectMapController = loader.getController();

        selectMapController.setClient(client);
        selectMapController.setSceneManager(this);


        selectMapController.setChatAndInfoBoard(
                chatViewRoot,
                chatViewController,
                infoBoardRoot,
                infoBoardController
        );

        selectMapController.updateInitialImage(robotName);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        client.setSelectMapErroLabel(true);

    }
    /**
     * Switches the scene to the game view.
     * <p>
     * Loads the main game screen, initializes the game controller, and sets up the chat UI.
     * </p>
     *
     * @param client The client instance managing game data.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void switchToGameView(Client client) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                //Objects.requireNonNull(getClass().getResource("/ServerClient/gameView.fxml"))
                Objects.requireNonNull(getClass().getResource("/ServerClient/game.fxml"))
        );
        GameDataBridge gameDataBridge = client.getGameDataBridge();
        loader.setControllerFactory(param -> new GameController(client, gameDataBridge));
        Parent root = loader.load();
        GameController gameViewController = loader.getController();

        gameViewController.setSceneManager(this);
        gameViewController.setChat(chatViewRoot, chatViewController);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        //stage.setFullScreen(true);
    }
    /**
     * Opens the game over popup displaying the winner.
     *
     * @param playerName The name of the winning player.
     * @param robotName  The robot used by the winner.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openGameOverPopUp(String playerName, String robotName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/gameOver.fxml"));
        Parent root = loader.load();

        GameOverController gameOverController = loader.getController();
        gameOverController.setWinnerDetails(playerName, robotName);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("GAME OVER");
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
    /**
     * Opens the reboot direction selection popup.
     * <p>
     * Allows the player to choose a reboot direction.
     * </p>
     *
     * @param client    The client instance.
     * @param robotName The player's robot name.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openRebootDirectionPopUp(Client client, String robotName) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/rebootDirection.fxml"));
        Parent root = loader.load();
        RebootDirectionController rebootController = loader.getController();
        rebootController.setClient(client);
        rebootController.setRobotImage(robotName);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("REBOOT");
        stage.setOnCloseRequest(event -> event.consume());
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
    /**
     * Opens the damage selection popup.
     * <p>
     * Allows the player to select damage cards when taking damage.
     * </p>
     *
     * @param client      The client instance.
     * @param damageCards The list of available damage cards.
     * @param count       The number of cards the player must pick.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openPickDamagePopUp(Client client, List<String> damageCards, int count) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/pickDamage.fxml"));
        Parent root = loader.load();

        PickDamageController damageController = loader.getController();
        damageController.setClient(client);
        damageController.setDamagePiles(damageCards, count);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Pick Damage");
        stage.setOnCloseRequest(event -> event.consume());
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
    /**
     * Opens the upgrade shop popup.
     * <p>
     * Allows players to buy or view available upgrades.
     * </p>
     *
     * @param client   The client instance.
     * @param viewMode If true, the shop is in view-only mode.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openUpgradeShopPopUp(Client client, boolean viewMode) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/upgradeShop.fxml"));
        loader.setControllerFactory(param -> new UpgradeShopController(client, viewMode));
        Parent root = loader.load();
        UpgradeShopController upgradeShopController = loader.getController();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Upgrade Shop");
        if (!viewMode) {
            stage.setOnCloseRequest(event -> event.consume());
        }
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
    /**
     * Opens the memory swap popup.
     * <p>
     * Allows players to swap memory cards during gameplay.
     * </p>
     *
     * @param client The client instance.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openMemorySwapPopUp(Client client) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/memorySwap.fxml"));
        Parent root = loader.load();

        MemorySwapController memorySwapController = loader.getController();
        memorySwapController.setClient(client);

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Memory Swap");
        stage.setOnCloseRequest(event -> event.consume());
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
    /**
     * Opens the admin privilege popup.
     * <p>
     * Allows players to activate admin privileges in the game.
     * </p>
     *
     * @param client The client instance.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void openAdminPrivilege(Client client) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerClient/adminPrivilege.fxml"));
        Parent root = loader.load();

        AdminPrivilegeController adminPrivilegeController = loader.getController();
        adminPrivilegeController.setClient(client);

        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Admin Privilege");
        stage.setOnCloseRequest(event -> event.consume());
        Animation.animatePopUp(root);
        stage.showAndWait();
    }
}
