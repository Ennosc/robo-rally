package viewmodel;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import model.game.AI.*;
import view.ClientGUI;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import model.server_client.Server;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Controller for the application launcher.
 * <p>
 * This controller handles the startup configuration for the client application, including
 * server selection (local or remote) and launching the appropriate client GUI.
 * </p>
 */
public class LauncherController {
    private static final String host = "localhost";
    private static final Logger logger = Logger.getLogger(LauncherController.class.getName());

    private String serverAddress = "sep21.dbs.ifi.lmu.de";
    private int serverPort = 52020;
    @FXML
    private ToggleGroup groupClient;
    @FXML
    private ToggleGroup groupServer;
    @FXML
    private CheckBox local_box;
    @FXML
    private CheckBox internet_box;
    @FXML
    private Button startButton;

//    serverAddress = "localhost";
//    serverPort = 8080;
    //serverAddress = "sep21.dbs.ifi.lmu.de";
    //serverPort = 52023;

    /**
     * Initializes the launcher.
     * <p>
     * Configures the logger and sets up the behavior of the local and internet checkboxes so that only
     * one can be selected at a time.
     * </p>
     */
    @FXML
    public void initialize() {
        initializeLogger();
        local_box.setOnAction(event -> {
            if (local_box.isSelected()) {
                internet_box.setSelected(false);
            }
        });

        internet_box.setOnAction(event -> {
            if (internet_box.isSelected()) {
                local_box.setSelected(false);
            }
        });
    }

    /**
     * Handles the start button click event.
     * <p>
     * Determines the server selection (local or remote), configures the server address and port,
     * closes the current stage, and launches the client GUI.
     * </p>
     */
    @FXML
    private void startButtonClicked(){
        String selected = local_box.isSelected() ? "local" : "dbs";
        logger.config(selected  + " server");

        serverSelection(selected);

        logger.config("Server address: " + serverAddress);
        logger.config("Server port: " + serverPort);


        Stage stage = (Stage) local_box.getScene().getWindow();
        stage.close();
        startClientGUI(serverAddress, serverPort);
        startButton.setDisable(true);
    }

    /**
     * Configures the server address and port based on the selection.
     *
     * @param serverSelection a string indicating the server selection ("local" or "dbs").
     */
    private void serverSelection(String serverSelection){
        if(serverSelection.equalsIgnoreCase("dbs")){
            serverAddress = "sep21.dbs.ifi.lmu.de";
            serverPort = 52020; //protocoll 2.0
            //serverPort = 52023; // 1.0
        }else if(serverSelection.equalsIgnoreCase("local")){
            serverAddress = "localhost";
            serverPort = 8080;
            if(!isServerRunning(serverPort)){
                startServerInBackground(serverPort);
            }
        }
    }
    /**
     * Checks if a server is running on the specified port.
     */
    private static boolean isServerRunning(int port) {
        try (Socket ignored = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    /**
     * Starts a local Server in the background on the given port.
     */
    private static void startServerInBackground(int port) {
        logger.info("Starting Server on port " + port + " ...");
        Thread serverThread = new Thread(() -> {
            Server server = new Server(port);
            server.run();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Small delay to allow the server to fully start before the client tries to connect
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Launches the normal GUI client.
     */
    private static void startClientGUI(String serverAddress, int serverPort) {
        try{
            ClientGUI.startClient(serverAddress, serverPort);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * Launches the AI client.
     */
//    private void startAIClient() {
//        // Launch the AI in its own Thread so the Launcher doesn't block
//        new Thread(() -> {
//            if (client.equalsIgnoreCase("easy AI")) {
//                logger.config("starting easy AI");
//                BasicSmartAIClient basicSmartAIClient = new BasicSmartAIClient(serverAddress, serverPort);
//                basicSmartAIClient.run();
//            } else if (client.equalsIgnoreCase("medium AI")) {
//                logger.config("starting medium AI");
//                MediumSmartAIClient mediumSmartAIClient = new MediumSmartAIClient(serverAddress, serverPort);
//                mediumSmartAIClient.run();
//            }else if (client.equalsIgnoreCase("hard AI")) {
//                logger.config("starting hard/smart AI");
//                SmartAIClient smartAI = new SmartAIClient(serverAddress, serverPort);
//                smartAI.run();
//            }
//        }).start();
//    }

    /**
     * Configures the logger.
     */
    public static void initializeLogger() {
        logger.setUseParentHandlers(false);
        logger.setLevel(java.util.logging.Level.ALL);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(java.util.logging.Level.ALL);
        consoleHandler.setFormatter(formatLogger());
        logger.addHandler(consoleHandler);
    }

    /**
     * Returns a custom Formatter for the logger that includes coloring and timestamps.
     *
     * @return a Formatter instance for formatting log records.
     */
    public static Formatter formatLogger() {
        return new Formatter() {
            @Override
            public String format(LogRecord record) {
                String color = switch (record.getLevel().getName()) {
                    case "SEVERE" -> "\u001B[31m";   // Red
                    case "WARNING" -> "\u001B[33m";  // Yellow
                    case "CONFIG"  -> "\u001B[34m";  // Blue
                    case "INFO"    -> "\u001B[37m";  // White
                    default        -> "\u001B[0m";   // Reset
                };

                String className = (record.getSourceClassName() != null) ? record.getSourceClassName() : "UnknownClass";
                String methodName = (record.getSourceMethodName() != null) ? record.getSourceMethodName() : "UnknownMethod";

                return String.format(
                        "%s[%s] [%s] [%s#%s] %s\u001B[0m%n",
                        color,
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                                new Date(record.getMillis())),
                        record.getLevel().getName(),
                        className,
                        methodName,
                        record.getMessage()
                );
            }
        };
    }

}
