package network.messages.phases7.programming;

import network.messages.Message;
import java.util.List;


/**
 * Message sent to a client indicating which cards they have received (typically as a result
 * of a forced action like timer expiration).
 */
public class CardsYouGotNowMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a CardsYouGotNowMessage.
     *
     * @param cards a list of card names that the client has received.
     */
    public CardsYouGotNowMessage(List<String> cards) {
        super("CardsYouGotNow");
        this.messageBody = new MessageBody(cards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a CardsYouGotNowMessage.
     */
    public static class MessageBody {
        private final List<String> cards;

        /**
         * Constructs a MessageBody with the specified list of cards.
         *
         * @param cards the list of card names.
         */
        public MessageBody(List<String> cards) {
            this.cards = cards;
        }

        public List<String> getCards() {
            return cards;
        }
    }
}