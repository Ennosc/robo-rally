package network.messages.actions8;

import network.messages.Message;
import java.util.List;

public class DiscardSomeMessage extends Message {

    private final MessageBody messageBody;

    public DiscardSomeMessage(List<String> cards) {
        super("DiscardSome");
        this.messageBody = new MessageBody(cards);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final List<String> cards;

        public MessageBody(List<String> cards) {
            this.cards = cards;
        }

        public List<String> getCards() {
            return cards;
        }
    }
}