package model.server_client;

import network.interpreters.ClientJsonInterpreter;
import network.messages.lobby3.MapSelectedMessage;
import network.messages.lobby3.PlayerValuesMessage;
import viewmodel.*;
import network.JsonHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a client that connects to a server and handles sending and receiving messages.
 * The Client class manages its connection to the server, processes input from the user,
 * and displays server messages.
 */
public class Client extends BaseClient {
    private SelectRobotController selectRobotController;
    private HashMap<Integer, String> nameBox;
    private ChatViewController chatViewController;
    private SelectMapController selectMapController;
    private boolean selectMapErrorLabel = false;
    private final ClientJsonInterpreter clientJsonInterpreter;

    /**
     * Constructs a new Client with a reference to the associated UI controller.
     *
     * @param controller The {@link SelectRobotController} that handles the UI logic.
     */
    public Client(SelectRobotController controller, String host, int port) {
        super(host, port);
        this.nameBox = new HashMap<>();
        this.selectRobotController = controller;
        this.selectMapController = new SelectMapController();
        this.clientJsonInterpreter = new ClientJsonInterpreter(this,logger);
        this.isAI = false;
        initializeRobotMap();
    }

    /**
     * Continuously listens for messages from the server and processes them appropriately.
     */
    @Override
    protected void listenForMessages() {
        new Thread(() -> {
            try {
                String messageFromServer;
                while ((messageFromServer = in.readLine()) != null) {
                    interpretMessage(messageFromServer);
                }
            } catch (IOException e) {
             logger.severe("Failed to read message from server.");
            }
        }).start();
    }

    /**
     * Sends player values (name and selected figure) to the server.
     *
     * @param name         the player's name.
     * @param figureNumber the chosen figure number.
     */
    public void sendPlayerValues(String name, int figureNumber) {
        this.clientName = name;
        PlayerValuesMessage pvm = new PlayerValuesMessage(name, figureNumber);
        String json = JsonHandler.toJson(pvm);
        sendMessageToServer(json);
    }

    /**
     * Retrieves the robot name corresponding to a given number.
     *
     * @param number the robot number.
     * @return the robot name, or "Not Selected" if not found.
     */
    public String getRobotNameByNumber(int number){
        for (Map.Entry<String, Integer> entry : robotNameToNumberMap.entrySet()) {
            if (entry.getValue() == number) {
                return entry.getKey();
            }
        }
        return "Not Selected";
    }

    public void setSelectMapController(SelectMapController selectMapController) {
        this.selectMapController = selectMapController;
    }


    public void setChatViewController(ChatViewController chatViewController) {
        this.chatViewController = chatViewController;
    }

    /**
     * Sends a message indicating that a specific map has been selected.
     *
     * @param mapName The name of the map that was chosen.
     */
    public void sendMapSelected(String mapName){
        MapSelectedMessage mss = new MapSelectedMessage(mapName);
        String json = JsonHandler.toJson(mss);
        sendMessageToServer(json);
    }

    public void addUserToBox(int id, String name){
        nameBox.put(id, name);
    }

    public HashMap<Integer,String> getNameBox(){
        return nameBox;
    }


    public HashMap<String, Integer> getRobotNameToNumberMap(){
        return robotNameToNumberMap;
    }

    /**
     * Processes an incoming message by delegating it to the client JSON interpreter.
     *
     * @param message the incoming message from the server.
     */
    @Override
    protected void interpretMessage(String message) {
        clientJsonInterpreter.interpretMessage(message);
    }

    public void setSelectMapErroLabel(boolean b){
        selectMapErrorLabel = b;
    }

    public SelectMapController getSelectMapController() {
        return selectMapController;
    }
}