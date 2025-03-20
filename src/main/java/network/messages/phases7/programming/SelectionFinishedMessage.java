package network.messages.phases7.programming;

import network.messages.Message;

/**
 * Message indicating that a player has finished selecting cards for their register.
 */
public class SelectionFinishedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a SelectionFinishedMessage.
     *
     * @param clientID the ID of the player who has finished selection.
     */
    public SelectionFinishedMessage(int clientID) {
        super("SelectionFinished");
        this.messageBody = new MessageBody(clientID);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a SelectionFinishedMessage.
     */
    public static class MessageBody {
        private final int clientID;

        /**
         * Constructs a MessageBody with the given client ID.
         *
         * @param clientID the client ID.
         */
        public MessageBody(int clientID) {
            this.clientID = clientID;
        }

        public int getClientID() {
            return clientID;
        }
    }
}