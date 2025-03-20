package network.messages.phases7.activation;

import network.messages.Message;

/**
 * Message sent to notify that a card in a register is being replaced.
 * <p>
 * Contains the register index, the new card's name, and the client ID.
 * </p>
 */
public class ReplaceCardMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a ReplaceCardMessage with the specified register, new card, and client ID.
     *
     * @param register the register index where the card is replaced.
     * @param newCard  the name of the new card.
     * @param clientID the ID of the client whose register is updated.
     */
    public ReplaceCardMessage(int register, String newCard, int clientID) {
        super("ReplaceCard");
        this.messageBody = new MessageBody(register, newCard, clientID);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a ReplaceCardMessage.
     */
    public static class MessageBody {
        private final int register;
        private final String newCard;
        private final int clientID;

        /**
         * Constructs a MessageBody with the specified register, new card, and client ID.
         *
         * @param register the register index.
         * @param newCard  the new card name.
         * @param clientID the client ID.
         */
        public MessageBody(int register, String newCard, int clientID) {
            this.register = register;
            this.newCard = newCard;
            this.clientID = clientID;
        }

        public int getRegister() {
            return register;
        }

        public String getNewCard() {
            return newCard;
        }

        public int getClientID() {
            return clientID;
        }
    }
}