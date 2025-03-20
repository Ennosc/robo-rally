package model.server_client;

import model.game.Game;
import model.game.Player;
import network.JsonHandler;
import network.messages.actions8.GameFinishedMessage;
import network.messages.connection2.AliveMessage;
import network.messages.connection2.HelloClientMessage;
import network.messages.lobby3.PlayerAddedMessage;
import network.messages.lobby3.PlayerStatusMessage;
import network.messages.specialMessage5.ConnectionUpdateMessage;
import network.messages.specialMessage5.ErrorMessage;
import network.messages.lobby3.SelectMapMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * Represents the game server, handling client connections
 * and communication between players.
 */
public class Server implements Runnable {
    private static final Game game = Game.getInstance();
    /**
     * Map of all connected clients, where the key is the player's id and the value is the connection handler.
     */
    public ConcurrentHashMap<String, ConnectionHandler> connectionsMap = new ConcurrentHashMap<>();
    private ServerSocket serverSocket; //server socket listen for incoming connections
    private boolean done;
    private ExecutorService threadPool;
    public final Set<String> nicknamesSet = Collections.synchronizedSet(new HashSet<>());
    public boolean isGameCreated = false;
    public boolean isGameRunning = false;
    private ScheduledExecutorService scheduler;
    private final String protocolVersion = "Version 2.0";
    private int clientID = 0;
    public ConcurrentHashMap<Integer, ConnectionHandler> clientIdMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> clientIdToNameMap = new ConcurrentHashMap<>();
    private List<ConnectionHandler> readyPlayers = new ArrayList<>();
    private List<String> availableMaps = new ArrayList<>();
    private String selectedMap;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static int port = 8080;

    /**
     * Initializes a new Server instance.
     */
    public Server(int port) {
        //this.serverController = serverController;
        this.port = port;
        availableMaps.add("Dizzy Highway");
        availableMaps.add("Extra Crispy");
        availableMaps.add("Lost Bearings");
        availableMaps.add("Death Trap");
        done = false;
    }

    public static void main(String[] args) {
        initializeLogger();
        logger.info("Starting Server");
        Server server = new Server(port);
        logger.info(port + " port");
        server.run();
    }


    /**
     * Runs the server, accepting client connections, and managing the client handlers.
     */
    @Override
    public void run() {
        try {

            serverSocket = new ServerSocket(port);
            threadPool = Executors.newCachedThreadPool();
            startAliveMessages(new AliveMessage());
            startAliveCheck();


            game.setServer(this);
            game.setLogger(logger);

            while (!done) {
                Socket clientSocket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(clientSocket, this, clientID);
                clientIdMap.put(clientID, handler);
                logger.info(clientID + " clientIDmap");
                clientID++;
                threadPool.execute(handler);
            }
        } catch (Exception e) {

        }
    }

    /**
     * Initializes the server logger with a custom formatter and file handler.
     */
    public static void initializeLogger() {
        logger.setUseParentHandlers(false);

        // Create console handler with a custom formatter
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatLogger());

        //for beta test
        try {
            // Create file handler
            FileHandler fileHandler = new FileHandler("Server_Log_File.txt", true);
            fileHandler.setFormatter(formatLogger());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create file handler for logger", e);
        }

        // Add console handler
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
                String color;
                switch (record.getLevel().getName()) {
                    case "SEVERE":
                        color = "\u001B[31m"; // Red
                        break;
                    case "WARNING":
                        color = "\u001B[33m"; // Yellow
                        break;
                    case "INFO":
                        color = "\u001B[37m"; // White
                        break;
                    default:
                        color = "\u001B[0m"; // Reset
                }

                String className = record.getSourceClassName() != null ? record.getSourceClassName() : "UnknownClass";
                String methodName = record.getSourceMethodName() != null ? record.getSourceMethodName() : "UnknownMethod";

                return String.format("%s[%s] [%s] [%s#%s] %s\u001B[0m%n",
                        color,
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())),
                        record.getLevel().getName(),
                        className,
                        methodName,
                        record.getMessage());
            }
        };
    }


    /**
     * Shuts down the server, closing the socket and shutting down the thread pool.
     */
    public void shutdownServer() {
        try {
            done = true;
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); //close serverSocket
            }
        } catch (IOException e) {
            logger.warning("Failed to close server socket.");
        }
        //cannot handle/ ignore it
    }

    /**
     * Removes a nickname from the set of active nicknames.
     *
     * @param nickname the nickname to remove
     */
    public void removeNickname(String nickname) {
        nicknamesSet.remove(nickname);
    }

    /**
     * Retrieves the map of all connected clients.
     *
     * @return connectionsMap
     */
    public ConcurrentHashMap<String, ConnectionHandler> getConnectionsMap() {
        return connectionsMap;
    }

    /**
     * Sends a HelloClient message to the specified connection handler.
     *
     * @param handler the connection handler to which the HelloClient message is sent.
     */
    public void helloClient(ConnectionHandler handler) {
        try {
            HelloClientMessage hcm = new HelloClientMessage(getProtocolVersion());
            String helloJson = JsonHandler.toJson(hcm);
            handler.sendMessage(helloJson);
        } catch (Exception e) {
            logger.severe("Failed to send HelloClient message to client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends the current players' information to a given connection handler.
     *
     * @param handler the connection handler that will receive the players' info.
     */
    public void sendPlayersInfo(ConnectionHandler handler) {
        List<Player> existingPlayers = game.getPlayers();
        for (Player player : existingPlayers) {
            int clientId = player.getPlayerId();
            String name = player.getName();
            int figureId = (player.getRobot() == null) ? -1 : player.getRobot().getId();
            PlayerAddedMessage pam = new PlayerAddedMessage(clientId, name, figureId);
            String pamJson = JsonHandler.toJson(pam);
            handler.sendMessage(pamJson);

            boolean playerStatus = player.isPlayerReady();
            PlayerStatusMessage psm = new PlayerStatusMessage(clientId, playerStatus);
            String psmJson = JsonHandler.toJson(psm);

            handler.sendMessage(psmJson);
        }
    }

    public String getSelectedMap() {
        return selectedMap;
    }

    public void setSelectedMap(String selectedMap) {
        this.selectedMap = selectedMap;
    }

    /**
     * Updates the list of ready players based on a client's readiness status.
     *
     * @param handler the connection handler of the client.
     * @param isReady the readiness status of the client.
     */
    public void updateReadyPlayers(ConnectionHandler handler, boolean isReady) {
        if (!isReady) {
            readyPlayers.remove(handler);
        } else if (!readyPlayers.contains(handler)) {
            readyPlayers.add(handler);
        }
        handleFirstReadyPlayer();
    }

    /**
     * Notifies the first ready non-AI player to select a map.
     */
    public void handleFirstReadyPlayer() {
        for (ConnectionHandler handler : readyPlayers) {
            Player player = game.getPlayerById(handler.getClientId());
            if (!player.isAI()) {
                SelectMapMessage smm = new SelectMapMessage(availableMaps);
                String smmJson = JsonHandler.toJson(smm);
                handler.sendMessage(smmJson);
                return;
            }
        }
    }

    public List<ConnectionHandler> getReadyPlayers() {
        return readyPlayers;
    }

    /**
     * Removes a client connection from the server.
     *
     * @param nickname The nickname of the client.
     */
    public void removeConnection(String nickname) {
        connectionsMap.remove(nickname);
        updateAllClients();
    }

    /**
     * Updates all clients with the current list of connected players.
     */
    public void updateAllClients() {
        String[] users = connectionsMap.keySet().toArray(new String[0]);
        connectionsMap.values().forEach(handler -> handler.updateUserList(users));
    }

    /**
     * Sends a message to all clients except the sender.
     *
     * @param message The message to broadcast.
     * @param handler The connectionHandler of the sender.
     */
    public void broadcastToAllExceptSelf(String message, ConnectionHandler handler) {
        connectionsMap.forEach((nickname, h) -> {
            if (h != handler) {
                h.sendMessage(message);
            }
        });
    }

    /**
     * Sends a message to all connected clients.
     *
     * @param jsonMessage The message to broadcast.
     */
    public void broadcastToAll(String jsonMessage) {
        connectionsMap.values().forEach(handler -> handler.sendMessage(jsonMessage));
    }

    /**
     * Broadcasts a message to a specified list of players.
     *
     * @param jsonMessage the message to broadcast in JSON format.
     * @param players     the list of players to which the message should be sent.
     */
    public void broadcastToPlayers(String jsonMessage, List<Player> players){
        for (Player player : players) {
            int id = player.getPlayerId();
            clientIdMap.get(id).sendMessage(jsonMessage);
        }
    }

    /**
     * Broadcasts a message to a specified list of players except for the sender.
     *
     * @param jsonMessage the message to broadcast in JSON format.
     * @param handler     the connection handler of the sender.
     * @param players     the list of players to which the message should be sent.
     */
    public void broadcastToPlayersExceptSelf(String jsonMessage,  ConnectionHandler handler, List<Player> players){
        for (Player player : players) {
            int id = player.getPlayerId();
            ConnectionHandler h = clientIdMap.get(id);
            if(h != handler){
                h.sendMessage(jsonMessage);
            }
        }
    }

    private void startAliveMessages(AliveMessage aliveMessage) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            connectionsMap.forEach((nickname, handler) -> {
                try {
                    String aliveJson = JsonHandler.toJson(aliveMessage);
                    handler.sendMessage(aliveJson);
                } catch (Exception e) {
                    logger.warning("Error sending the alive message " + nickname + ": " + e.getMessage());
                }
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Starts checking periodically if clients are alive.
     */
    public void startAliveCheck() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            connectionsMap.forEach((nickname, handler) -> {
                try {
                    checkAlive(handler);
                } catch (Exception e) {
                    logger.warning("Error sending the alive message " + nickname + ": " + e.getMessage());
                }
            });
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Checks if the specified client is alive based on the last alive timestamp.
     * <p>
     * If the client is not alive, it removes the client, broadcasts a connection update, and, if only one player
     * remains, sends a GameFinished message.
     * </p>
     *
     * @param handler the ConnectionHandler for the client to check.
     */
    public void checkAlive(ConnectionHandler handler) {
       long timeSinceLastAlive = System.currentTimeMillis() - handler.getLastAliveTime();
        if (timeSinceLastAlive > 11000) {
            //If client not alive anymore -> Ignore
            ConnectionUpdateMessage cum = new ConnectionUpdateMessage(handler.getClientId(), false, "Ignore");
            String json = JsonHandler.toJson(cum);
            try {
                connectionsMap.remove(handler.getNickname());
                clientIdMap.remove(handler.getClientId());
                clientIdToNameMap.remove(handler.getClientId());
                Game.getInstance().removePlayerByID(handler.getClientId());
                handler.shutdownClient();
            } catch (Exception e) {
                logger.warning("Error while shutting down Client " + e.getMessage());
            }
            if(Game.getInstance().getPlayers().size() == 1) {
                GameFinishedMessage fm = new GameFinishedMessage(Game.getInstance().getPlayers().get(0).getPlayerId());
                String json2 = JsonHandler.toJson(fm);
                broadcastToAll(json2);
            }
            //for broadcasting that a client left
            broadcastToAll(json);

        }
    }

    /**
     * Sends an error message to a specified client.
     *
     * @param error   the error message to send.
     * @param handler the ConnectionHandler of the client to receive the error.
     */
    public void sendError(String error, ConnectionHandler handler) {
        ErrorMessage errorMessage = new ErrorMessage(error);
        String json = JsonHandler.toJson(errorMessage);
        handler.sendMessage(json);
    }

    /**
     * Retrieves the ConnectionHandler for a given client ID.
     *
     * @param id the client ID.
     * @return the ConnectionHandler associated with the given ID.
     */
    public ConnectionHandler getConnectionHandlerById(int id) {
        return clientIdMap.get(id);
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Associates a client ID with a client name.
     *
     * @param clientId the client ID.
     * @param name     the client's name.
     */
    public void addClientName(int clientId, String name) {
        clientIdToNameMap.put(clientId, name);
    }

    public String getClientName(int clientId) {
        return clientIdToNameMap.get(clientId);
    }

    /**
     * Removes a client name from the mapping and updates internal lists.
     *
     * @param clientId the client ID whose name is to be removed.
     */
    public void removeClientName(int clientId) {
        clientIdToNameMap.remove(clientId);
        readyPlayers.remove(clientId);
        clientIdMap.remove(clientId);
        clientIdMap.remove(clientId);
    }

    public Logger getLogger() {
        return logger;
    }
}