package network.messages.phases7.programming;

import network.messages.Message;

/**
 * Message sent when a card is selected by a client during the programming phase.
 * <p>
 * It contains the client ID, the register index where the card is selected,
 * and a flag indicating whether the register is filled.
 * </p>
 */
public class CardSelectedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a CardSelectedMessage with the specified client ID, register, and filled status.
     *
     * @param clientID the ID of the client selecting the card.
     * @param register the register index where the card is placed.
     * @param filled   {@code true} if the register is filled; {@code false} otherwise.
     */
    public CardSelectedMessage(int clientID, int register, boolean filled) {
        super("CardSelected");
        this.messageBody = new MessageBody(clientID, register, filled);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a CardSelectedMessage.
     */
    public static class MessageBody {
        private final int clientID;
        private final int register;
        private final boolean filled;

        /**
         * Constructs a MessageBody with the specified client ID, register, and filled flag.
         *
         * @param clientID the client ID.
         * @param register the register index.
         * @param filled   whether the register is filled.
         */
        public MessageBody(int clientID, int register, boolean filled) {
            this.clientID = clientID;
            this.register = register;
            this.filled = filled;
        }

        public int getClientID() {
            return clientID;
        }

        public int getRegister() {
            return register;
        }

        public boolean isFilled() {
            return filled;
        }
    }
}