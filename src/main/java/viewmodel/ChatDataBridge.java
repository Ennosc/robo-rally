package viewmodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import java.util.function.Consumer;


/**
 * Manages chat data and provides a bridge between the client and the UI.
 * This class handles chat messages, connected users, and user list updates.
 */
public class ChatDataBridge {
    private Consumer<String> onNewMessage;
    private ObservableList<String> chatMessages = FXCollections.observableArrayList(); // ObservableList für automatische Updates
    private Consumer<ObservableMap<Integer, String>> onUserListUpdate;
    private ObservableMap<Integer, String> connectedUsers = FXCollections.observableHashMap();
    private boolean isPrivate = false;



    /**
     * Adds a new message to the chat.
     *
     * @param message   The message text.
     * @param isPrivate If true, the message is private.
     */
    public void addMessage(String message, boolean isPrivate) {
        String formattedMessage = (isPrivate ? "[Private] " : "") + message;
        if (isPrivate) {
            this.isPrivate = true;
        } else {
            this.isPrivate = false;
        }
        chatMessages.add(formattedMessage);
        if (onNewMessage != null) {
            onNewMessage.accept(formattedMessage);
        }
    }

    /**
     * Gets the list of connected users.
     *
     * @return A map of user IDs to usernames.
     */
    public ObservableMap<Integer, String> getConnectedUsers() {
        return connectedUsers;
    }


    public ObservableList<String> getChatMessages() {
        return chatMessages;
    }

   /**
     * Adds a single user to the connected user list.
     *
     * @param clientId The player's unique ID.
     * @param name     The player's name.
     */
    public synchronized void addUserToList(int clientId, String name) {
        this.connectedUsers.put(clientId, name);

        if (onUserListUpdate != null) {
            onUserListUpdate.accept(this.connectedUsers);
        }
    }

    public boolean isPrivate() {
        return isPrivate;
    }
}
