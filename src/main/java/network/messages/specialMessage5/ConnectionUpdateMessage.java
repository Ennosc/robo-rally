package network.messages.specialMessage5;

import network.messages.Message;

/**
 * Represents a message that updates the connection status of a client.
 */
public class ConnectionUpdateMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a ConnectionUpdateMessage.
     *
     * @param clientID    the client's ID.
     * @param isConnected {@code true} if the client is connected; {@code false} otherwise.
     * @param action      a string describing the action (e.g., "remove", "Ignore").
     */
    public ConnectionUpdateMessage(int clientID, boolean isConnected, String action) {
        super("ConnectionUpdate");
        this.messageBody = new MessageBody(clientID, isConnected, action);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a ConnectionUpdateMessage.
     */
    public static class MessageBody {
        private final int clientID;
        private final boolean isConnected;
        private final String action;

        /**
         * Constructs a MessageBody with the specified parameters.
         *
         * @param clientID    the client's ID.
         * @param isConnected the connection status.
         * @param action      the action description.
         */
        public MessageBody(int clientID, boolean isConnected, String action) {
            this.clientID = clientID;
            this.isConnected = isConnected;
            this.action = action;
        }

        public int getClientID() {
            return clientID;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public String getAction() {
            return action;
        }
    }
}
