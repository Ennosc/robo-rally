package network.messages.lobby3;

import network.messages.Message;

/**
 * Message sent to inform clients about a player's status (ready or not ready).
 */
public class PlayerStatusMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a PlayerStatusMessage with the specified client ID and readiness status.
     *
     * @param clientID the ID of the player.
     * @param ready    {@code true} if the player is ready; {@code false} otherwise.
     */
    public PlayerStatusMessage(int clientID, boolean ready) {
        super("PlayerStatus");
        this.messageBody = new MessageBody(clientID, ready);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a PlayerStatusMessage.
     */
    public static class MessageBody {
        private final int clientID;
        private final boolean ready;

        /**
         * Constructs a MessageBody with the given client ID and readiness status.
         *
         * @param clientID the player's ID.
         * @param ready    the readiness flag.
         */
        public MessageBody(int clientID, boolean ready) {
            this.clientID = clientID;
            this.ready = ready;
        }

        public int getClientID() {
            return clientID;
        }

        public boolean isReady() {
            return ready;
        }
    }
}
