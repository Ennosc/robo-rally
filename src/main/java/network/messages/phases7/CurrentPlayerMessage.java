package network.messages.phases7;

import network.messages.Message;

/**
 * Message indicating which client is currently taking their turn.
 */
public class CurrentPlayerMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a CurrentPlayerMessage.
     *
     * @param clientID the ID of the current player.
     */
    public CurrentPlayerMessage(int clientID) {
        super("CurrentPlayer");
        this.messageBody = new MessageBody(clientID);
    }


    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a CurrentPlayerMessage.
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