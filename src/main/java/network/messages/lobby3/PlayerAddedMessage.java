package network.messages.lobby3;

import network.messages.Message;

/**
 * Message sent to inform clients that a new player has been added.
 * <p>
 * Contains the client ID, player's name, and chosen figure.
 * </p>
 */
public class PlayerAddedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a PlayerAddedMessage with the specified client ID, name, and figure.
     *
     * @param clientID the new player's ID.
     * @param name     the player's name.
     * @param figure   the chosen figure number.
     */
    public PlayerAddedMessage(int clientID, String name, int figure) {
        super("PlayerAdded");
        this.messageBody = new MessageBody(clientID, name, figure);
    }

    /**
     * The body of a PlayerAddedMessage.
     */
    public static class MessageBody {
        private final int clientID;
        private final String name;
        private final int figure;

        /**
         * Constructs a MessageBody with the specified client ID, name, and figure.
         *
         * @param clientID the player's ID.
         * @param name     the player's name.
         * @param figure   the chosen figure number.
         */
        public MessageBody(int clientID, String name, int figure) {
            this.clientID = clientID;
            this.name = name;
            this.figure = figure;
        }

        public int getClientID() {
            return clientID;
        }

        public String getName() {
            return name;
        }

        public int getFigure() {
            return figure;
        }
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}