package viewmodel;

import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.ScrollPane;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;

import java.util.HashMap;

import model.server_client.Client;

/**
 * Controls the chatUI, handling user messages and updates.
 * This class integrates with the ChatDataBridge to manage the chat messages.
 */
public class ChatViewController {

    @FXML
    private VBox vbox_messages;
    @FXML
    private ScrollPane sp_main;
    @FXML
    private TextField tf_message;
    @FXML
    private ComboBox<String> comboBox_users;
    @FXML
    private Button button_send;

    private static ChatViewController instance;

    private Client client;

    /**
     * Initializes the chat UI and sets up event listeners.
     *
     * @param client The client instance managing chat communication.
     */
    public void initialize(Client client) {
        this.client = client;
        client.setChatViewController(this);

        if (client.getNameBox() != null && !client.getNameBox().isEmpty()) {
            updateUserList();
        }


        client.getChatDataBridge().getChatMessages().addListener((ListChangeListener<String>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (String newMessage : change.getAddedSubList()) {
                        boolean isPrivate = client.getChatDataBridge().isPrivate();
                        Platform.runLater(() -> addMessage(newMessage, false, isPrivate));
                    }
                }
            }
        });

        client.getChatDataBridge().getConnectedUsers().addListener((MapChangeListener<Integer, String>) change -> {
            Platform.runLater(() -> updateUserList());
        });

        button_send.setOnAction(event -> sendMessage());

    // Set up the TextField to send the message on Enter key press
        tf_message.setOnKeyPressed(event -> {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    });
}

/**
 * Called by your Lobby or Game controllers to share the same Client instance.
 */
public void setClient(Client client) {
    this.client = client;
    updateUserList();
}

/**
 * Button handler: send message to server
 */
private void sendMessage() {
    if (client == null) return;

    String messageToSend = tf_message.getText().trim();
    if (messageToSend.isEmpty()) return;
    String selectedUser = comboBox_users.getValue();



    boolean isPrivate = false;
    int recipientId = -1;

    if (selectedUser != null && !selectedUser.equals("All")) {
        for (var entry : client.getChatDataBridge().getConnectedUsers().entrySet()) {
            if (entry.getValue().equals(selectedUser)) {
                recipientId = entry.getKey();
                isPrivate = true;
                break;
            }
        }
        client.sendChatMessage(client.getClientName() + ": " + messageToSend, recipientId);
    } else {
        client.sendChatMessage(client.getClientName() + ": " + messageToSend, -1);

    }
    addMessage(messageToSend, true, isPrivate);

    tf_message.clear();
}

/**
 * Display a new message.
 *
 * @param message   The message content.
 * @param isSender  True if the message is sent by the client itself.
 * @param isPrivate True if the message is a private message.
 */
public void addMessage(String message, boolean isSender, boolean isPrivate) {
    Platform.runLater(() -> {
        HBox hbox = new HBox();
        hbox.setAlignment(isSender ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(5, 5, 5, 10));

        String displayMessage = message;
        Text text = new Text(displayMessage);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setPadding(new Insets(5, 10, 5, 10));

        if (isSender) {
            textFlow.setStyle("-fx-background-color: #96AAB8; -fx-background-radius: 0px;");
            text.setFill(Color.BLACK);
        } else if (isPrivate) {
            textFlow.setStyle("-fx-background-color: #667C8D; -fx-background-radius: 0px;");
            text.setFill(Color.BLACK);
        } else {
            textFlow.setStyle("-fx-background-color: #7D98A8; -fx-background-radius: 0px;");
            text.setFill(Color.BLACK);
        }

        hbox.getChildren().add(textFlow);
        vbox_messages.getChildren().add(hbox);
    });
}

/**
 * Called whenever the client’s list of connected users changes
 */

public synchronized void updateUserList() {
    Platform.runLater(() -> {
        comboBox_users.getItems().clear();
        comboBox_users.getItems().add("All"); // Standard-Option für Broadcast
        comboBox_users.getItems().addAll(client.getChatDataBridge().getConnectedUsers().values());
    });
}
}
