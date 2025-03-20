package network.messages.phases7.upgrade;

import network.messages.Message;

/**
 * Message indicating that a player has attempted to buy (or not buy) an upgrade.
 */
public class BuyUpgradeMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a BuyUpgradeMessage.
     *
     * @param isBuying {@code true} if the player is buying the upgrade; {@code false} otherwise.
     * @param card     the name of the upgrade card.
     */
    public BuyUpgradeMessage(boolean isBuying, String card) {
        super("BuyUpgrade");
        this.messageBody = new MessageBody(isBuying,card);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a BuyUpgradeMessage.
     */
    public static class MessageBody {
        private final boolean isBuying;
        private final String card;

        /**
         * Constructs a MessageBody with the given parameters.
         *
         * @param isBuying whether the player is buying.
         * @param card     the upgrade card name.
         */
        public MessageBody(boolean isBuying,String card) {
            this.isBuying = isBuying;
            this.card = card;
        }
        public boolean isBuying() {
            return isBuying;
        }
        public String getCard() {
            return card;
        }
    }


}
