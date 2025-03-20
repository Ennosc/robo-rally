package network.messages.phases7.programming;

import network.messages.Message;

/**
 * Message indicating that the programming deck has been shuffled.
 */
public class ShuffleCodingMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a ShuffleCodingMessage.
     *
     * @param clientID the ID of the client whose deck was shuffled.
     */
    public ShuffleCodingMessage(int clientID) {
        super("ShuffleCoding");
        this.messageBody = new MessageBody(clientID);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a ShuffleCodingMessage.
     */
    public static class MessageBody {
        private final int clientID;

        /**
         * Constructs a MessageBody with the specified client ID.
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