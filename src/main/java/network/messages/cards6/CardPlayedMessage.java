package network.messages.cards6;

import network.messages.Message;

public class CardPlayedMessage extends Message {

    private final MessageBody messageBody;

    public CardPlayedMessage(int clientID, String card) {
        super("CardPlayed");
        this.messageBody = new MessageBody(clientID, card);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final String card;

        public MessageBody(int clientID, String card) {
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