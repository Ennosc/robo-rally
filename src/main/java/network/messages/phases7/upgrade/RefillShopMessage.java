package network.messages.phases7.upgrade;

import network.messages.Message;
import java.util.List;

/**
 * Message indicating that the upgrade shop has been refilled.
 */
public class RefillShopMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a RefillShopMessage.
     *
     * @param cards a list of upgrade card names now available in the shop.
     */
    public RefillShopMessage(List<String> cards) {
        super("RefillShop");
        this.messageBody = new MessageBody(cards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a RefillShopMessage.
     */
    public static class MessageBody {
        private final List<String> cards;

        /**
         * Constructs a MessageBody with the given list of card names.
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
