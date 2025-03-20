package network.messages.phases7.upgrade;

import network.messages.Message;

import java.util.List;

/**
 * Message indicating that the upgrade shop is being exchanged (reset).
 */
public class ExchangeShopMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an ExchangeShopMessage.
     *
     * @param cards a list of upgrade card names for the new shop.
     */
    public ExchangeShopMessage(List<String> cards) {
        super("ExchangeShop");
        this.messageBody = new MessageBody(cards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of an ExchangeShopMessage.
     */
    public static class MessageBody {
        private final List<String> cards;


        /**
         * Constructs a MessageBody with the specified list of card names.
         *
         * @param cards the list of upgrade card names.
         */
        public MessageBody(List<String> cards) {
            this.cards = cards;
        }

        public List<String> getCards() {
            return cards;
        }
    }
}
