package network.messages.phases7.programming;

import network.messages.Message;
import java.util.List;

/**
 * Message sent to a player to indicate the cards currently in their hand.
 */
public class YourCardsMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a YourCardsMessage.
     *
     * @param cardsInHand a list of card names in the player's hand.
     */
    public YourCardsMessage(List<String> cardsInHand) {
        super("YourCards");
        this.messageBody = new MessageBody(cardsInHand);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a YourCardsMessage.
     */
    public static class MessageBody {
        private final List<String> cardsInHand;

        /**
         * Constructs a MessageBody with the given list of card names.
         *
         * @param cardsInHand the list of card names.
         */
        public MessageBody(List<String> cardsInHand) {
            this.cardsInHand = cardsInHand;
        }

        public List<String> getCardsInHand() {
            return cardsInHand;
        }
    }
}
