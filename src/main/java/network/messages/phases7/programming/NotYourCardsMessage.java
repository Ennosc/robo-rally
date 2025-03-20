package network.messages.phases7.programming;

import network.messages.Message;


/**
 * Message sent to clients indicating the number of cards in a player's hand,
 * typically used to notify other players of a change in hand size.
 */
public class NotYourCardsMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a NotYourCardsMessage.
     *
     * @param clientID    the ID of the player.
     * @param cardsInHand the number of cards in the player's hand.
     */
    public NotYourCardsMessage(int clientID, int cardsInHand) {
        super("NotYourCards");
        this.messageBody = new MessageBody(clientID, cardsInHand);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a NotYourCardsMessage.
     */
    public static class MessageBody {
        private final int clientID;
        private final int cardsInHand;

        /**
         * Constructs a MessageBody with the given parameters.
         *
         * @param clientID    the player ID.
         * @param cardsInHand the number of cards in hand.
         */
        public MessageBody(int clientID, int cardsInHand) {
            this.clientID = clientID;
            this.cardsInHand = cardsInHand;
        }

        public int getClientID() {
            return clientID;
        }

        public int getCardsInHand() {
            return cardsInHand;
        }
    }
}