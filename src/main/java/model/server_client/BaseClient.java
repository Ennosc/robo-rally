package model.server_client;

import network.JsonHandler;
import network.messages.actions8.ChooseRegisterMessage;
import network.messages.actions8.RebootDirectionMessage;
import network.messages.actions8.SelectedDamageMessage;
import network.messages.cards6.PlayCardMessage;
import network.messages.chat4.SendChatMessage;
import network.messages.lobby3.SetStatusMessage;
import network.messages.phases7.programming.SelectedCardMessage;
import network.messages.phases7.setup.SetStartingPointMessage;
import network.messages.phases7.upgrade.BuyUpgradeMessage;
import network.messages.specialMessage5.ConnectionUpdateMessage;
import viewmodel.ChatDataBridge;
import viewmodel.GameDataBridge;
import viewmodel.LobbyDataBridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.logging.*;

/**
 * Abstract base class for a client. Provides common functionality for connecting to the server,
 * sending messages, handling logging, and managing shared data bridges.
 */
public abstract class BaseClient implements Runnable {
    protected static final String DEFAULT_HOST = "localhost";
    protected static final int DEFAULT_PORT = 8080;

    protected String protocolVersion = "Version 2.0";

    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected Logger logger;
    protected String host;
    protected int port;
    protected int clientId;
    protected String clientName;
    protected boolean isReady = false;
    protected HashMap<String, Integer> robotNameToNumberMap = new HashMap<>();
    protected ChatDataBridge chatDataBridge;
    protected LobbyDataBridge lobbyDataBridge;
    protected GameDataBridge gameDataBridge;
    protected boolean isAI;
    protected String group;

    // BaseClient Constructor
    /**
     * Constructs a BaseClient with default host and port.
     */
    public BaseClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);

    }

    /**
     * Constructs a BaseClient with the specified host and port.
     *
     * @param host the server host.
     * @param port the server port.
     */
    public BaseClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gameDataBridge = new GameDataBridge();
        this.chatDataBridge = new ChatDataBridge();
        this.lobbyDataBridge = new LobbyDataBridge();
        this.group = "EdleEisbecher";
        initializeLogger();
    }


    /**
     * Runs the client, establishing a connection to the server, starting the InputHandler
     * for user input, and listening for messages from the server.
     */

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            listenForMessages();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to connect to server: " + e.getMessage(), e);
        }
    }

    /**
     * Initializes the logger with a custom formatter and attaches both console and file handlers.
     */
    protected void initializeLogger() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.setUseParentHandlers(false); // Prevent default console handler

        // Create and configure ConsoleHandler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(createFormatter());
        logger.addHandler(consoleHandler);

//        try {
//            FileHandler fileHandler = new FileHandler("Client_Log_File.txt", true);
//            fileHandler.setFormatter(createFormatter());
//            logger.addHandler(fileHandler);
//        } catch (IOException e) {
//            logger.log(Level.SEVERE, "Failed to create FileHandler for logger", e);
//        }
    }

    // Formatter for Log Messages
    private Formatter createFormatter() {
        return new Formatter() {
            @Override
            public String format(LogRecord record) {
                String color = switch (record.getLevel().getName()) {
                    case "SEVERE" -> "\u001B[31m"; // Red
                    case "WARNING" -> "\u001B[33m"; // Yellow
                    case "INFO" -> "\u001B[37m"; // White
                    default -> "\u001B[0m"; // Reset
                };

                String className = record.getSourceClassName() != null ? record.getSourceClassName() : "UnknownClass";
                String methodName = record.getSourceMethodName() != null ? record.getSourceMethodName() : "UnknownMethod";

                return String.format("%s[%s] [%s] [%s#%s] %s\u001B[0m%n",
                        color,
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(record.getMillis())),
                        record.getLevel().getName(),
                        className,
                        methodName,
                        record.getMessage());
            }
        };
    }


    public Logger getLogger() {
        return logger;
    }

    protected abstract void interpretMessage(String message);

    /**
     * Sends a message to the server.
     *
     * @param message the message to send.
     */
    public void sendMessageToServer(String message) {
        logger.info("Sent message: " + message);
        out.println(message);
    }

    /**
     * Sends a chat message to the server.
     *
     * @param messageText the chat message text.
     * @param to          the recipient ID, or -1 (to all) for broadcast.
     */
    public void sendChatMessage(String messageText, int to) {
        SendChatMessage message = new SendChatMessage(messageText, to);
        String json = JsonHandler.toJson(message);
        sendMessageToServer(json);
    }

    /**
     * Shuts down the client by closing the input and output streams, the socket,
     * and setting the done flag to true to stop the InputHandler thread.
     */
    public void shutdown() {
        try {
            logger.info("Closing connection to server.");
            // Close resources
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed() && socket != null) socket.close();
        } catch (IOException e) {
            logger.warning("Failed to close connection to server." + e.getMessage());
        }
    }

    protected abstract void listenForMessages();

    public void setClientID(int clientId) {
        this.clientId = clientId;
    }

    public int getClientID() {
        return clientId;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName(){
        return clientName;
    }

    protected void initializeRobotMap(){
        robotNameToNumberMap.put("ZoomBot", 0);
        robotNameToNumberMap.put("HammerBot", 1);
        robotNameToNumberMap.put("HulkX90", 2);
        robotNameToNumberMap.put("SmashBot", 3);
        robotNameToNumberMap.put("SpinBot", 4);
        robotNameToNumberMap.put("Twonky", 5);
    }

    /**
     * Sends the client's readiness status to the server.
     *
     * @param status {@code true} if ready; {@code false} otherwise.
     */
    public void sendSetStatus(boolean status) {
        this.isReady = status;
        SetStatusMessage ssm = new SetStatusMessage(status);
        String json = JsonHandler.toJson(ssm);
        sendMessageToServer(json);
    }

    public GameDataBridge getGameDataBridge() {
        return gameDataBridge;
    }
    public ChatDataBridge getChatDataBridge() {
        return chatDataBridge;
    }
    public LobbyDataBridge getLobbyDataBridge() {return lobbyDataBridge;}

    /**
     * Sends a message indicating that a specific card has been played.
     *
     * @param cardName The name of the card that was played.
     */
    public void sendPlayCard(String cardName) {
        PlayCardMessage pcm = new PlayCardMessage(cardName);
        String pcmJson = JsonHandler.toJson(pcm);
        sendMessageToServer(pcmJson);
    }

    /**
     * Sends a message indicating the chosen starting point.
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     */
    public void sendSetStartingPoint(int x, int y){
        SetStartingPointMessage sspm = new SetStartingPointMessage(x,y);
        String json = JsonHandler.toJson(sspm);
        sendMessageToServer(json);
    }

    /**
     * Sends a message indicating the selected programming card and its register position.
     *
     * @param card     the name of the card.
     * @param register the register position.
     */
    public void sendSelectedCard(String card, int register){
        SelectedCardMessage scm = new SelectedCardMessage(card, register);
        String json = JsonHandler.toJson(scm);
        sendMessageToServer(json);
    }

    /**
     * Sends a message indicating the chosen reboot direction.
     *
     * @param direction the chosen direction.
     */
    public void sendRebootDirection(String direction){
        RebootDirectionMessage rdm = new RebootDirectionMessage(direction);
        String json = JsonHandler.toJson(rdm);
        sendMessageToServer(json);
    }

    /**
     * Sends a connection update message to the server.
     *
     * @param clientID  the client ID.
     * @param connected {@code true} if connected; {@code false} otherwise.
     * @param action    a string describing the action.
     */
    public void sendConnectionUpdate(int clientID, boolean connected, String action) {
        ConnectionUpdateMessage cum = new ConnectionUpdateMessage(clientID, connected, action);
        String json = JsonHandler.toJson(cum);
        sendMessageToServer(json);
    }

    /**
     * Sends a message with the selected damage cards.
     *
     * @param cards the list of selected damage card names.
     */
    public void sendSelectedDamage(List<String> cards) {
        SelectedDamageMessage sdm = new SelectedDamageMessage(cards);
        String json = JsonHandler.toJson(sdm);
        sendMessageToServer(json);
    }

    /**
     * Sends a message indicating whether the client is buying an upgrade and which card.
     *
     * @param isBuying the buying status.
     * @param cardName the upgrade card name.
     */
    public void sendBuyUpgrade(boolean isBuying,String cardName){
        BuyUpgradeMessage bum = new BuyUpgradeMessage(isBuying, cardName);
        String json = JsonHandler.toJson(bum);
        sendMessageToServer(json);
        logger.info("after send to server");
    }

    /**
     * Sends a message to choose a register.
     *
     * @param register the register number chosen.
     */
    public void sendChooseRegister(int register) {
        ChooseRegisterMessage crm = new ChooseRegisterMessage(register);
        String json = JsonHandler.toJson(crm);
        sendMessageToServer(json);
    }

    public String getHost (){
        return host;
    }

    public boolean isReady() {
        return isReady;
    }

    public boolean isAI(){
        return isAI;
    }
    public String getGroup(){
        return group;
    }
}