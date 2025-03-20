package model.server_client;

import network.interpreters.JsonInterpreter;
import network.interpreters.ServerJsonInterpreter;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Handles individual client connections, managing messages and game actions.
 */
public class ConnectionHandler implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    //private final ServerController serverController;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private final int clientID;// clientid from client
    private long lastAliveTime = System.currentTimeMillis();
    private Logger logger;


    /**
     * Constructs a ConnectionHandler for a specific client.
     *
     * @param clientSocket the client socket
     * @param server the server instance

     * @param id the unique ID assigned to the client
     */
    public ConnectionHandler(Socket clientSocket, Server server, int id) {
        this.clientSocket = clientSocket;
        this.server = server;
        //this.serverController = serverController;
        this.clientID = id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Handles the connection, starting message listeners and managing client communication.
     */
    @Override
    public void run() {
        logger = server.getLogger();
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            server.helloClient(this);
            JsonInterpreter interpreter = new ServerJsonInterpreter(this, server, logger);
            listenForMessages(interpreter);
        } catch (IOException e) {
            logger.severe("Error handling client: " + e.getMessage());
        }
    }


    /**
     * Listens for incoming messages from the client and processes them.
     *
     * @throws IOException if an I/O error occurs
     */
    private synchronized void listenForMessages(JsonInterpreter interpreter) throws IOException {
        String json;
        while ((json = in.readLine()) != null) {
            interpreter.interpretMessage(json);
        }
    }

    /**
     * Sends a message to the client.
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    /**
     * Updates the user list for the client.
     *
     * @param users an array of usernames currently connected to the server
     */
    public void updateUserList(String[] users) {
    }
    /**
     * Shuts down the client connection and cleans up resources.
     */
    public void shutdownClient() {
        try {
            if (nickname != null) {
                server.removeNickname(nickname);
                server.removeConnection(nickname);

//                server.broadcastToAllExceptSelf(nickname + " has left the server", nickname);
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            logger.severe("Error closing client connection: " + e.getMessage());
        }
    }

    public long getLastAliveTime() {
        return lastAliveTime;
    }
    public void setLastAliveTime(long lastAliveTime) {
        this.lastAliveTime = lastAliveTime;
    }


    /**
     * Retrieves the client's nickname.
     *
     * @return the client's nickname
     */
    public String getNickname(){
        return this.nickname;
    }
    /**
     * Retrieves the client's ID.
     *
     * @return the client's ID
     */
    public int getClientId(){
        return this.clientID;
    }

    public Server getServer(){
        return this.server;
    }
}

