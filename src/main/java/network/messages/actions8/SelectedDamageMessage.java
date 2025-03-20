package network.messages.actions8;

import network.messages.Message;
import java.util.List;

public class SelectedDamageMessage extends Message {

    private final MessageBody messageBody;

    public SelectedDamageMessage(List<String> cards) {
        super("SelectedDamage");
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