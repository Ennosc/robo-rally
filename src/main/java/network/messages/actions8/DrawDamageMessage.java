package network.messages.actions8;

import network.messages.Message;
import java.util.List;

public class DrawDamageMessage extends Message {

    private final MessageBody messageBody;

    public DrawDamageMessage(int clientID, List<String> cards) {
        super("DrawDamage");
        this.messageBody = new MessageBody(clientID, cards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final List<String> cards;

        public MessageBody(int clientID, List<String> cards) {
            this.clientID = clientID;
            this.cards = cards;
        }

        public int getClientID() {
            return clientID;
        }

        public List<String> getCards() {
            return cards;
        }
    }
}