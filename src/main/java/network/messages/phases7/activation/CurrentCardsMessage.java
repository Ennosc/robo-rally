package network.messages.phases7.activation;

import network.messages.Message;
import java.util.List;

public class CurrentCardsMessage extends Message {

    private final MessageBody messageBody;

    public CurrentCardsMessage(List<ActiveCard> activeCards) {
        super("CurrentCards");
        this.messageBody = new MessageBody(activeCards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a CurrentCardsMessage.
     */
    public static class MessageBody {
        private final List<ActiveCard> activeCards;

        /**
         *
         * @param activeCards
         */
        public MessageBody(List<ActiveCard> activeCards) {
            this.activeCards = activeCards;
        }

        public List<ActiveCard> getActiveCards() {
            return activeCards;
        }
    }

    public static class ActiveCard {
        private final int clientID;
        private final String card;

        public ActiveCard(int clientID, String card) {
            this.clientID = clientID;
            this.card = card;
        }

        public int getClientID() {
            return clientID;
        }

        public String getCard() {
            return card;
        }
    }
}