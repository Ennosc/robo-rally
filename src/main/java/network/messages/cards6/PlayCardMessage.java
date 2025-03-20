package network.messages.cards6;

import network.messages.Message;

public class PlayCardMessage extends Message {

    private final MessageBody messageBody;

    public PlayCardMessage(String card) {
        super("PlayCard");
        this.messageBody = new MessageBody(card);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final String card;

        public MessageBody(String card) {
            this.card = card;
        }

        public String getCard() {
            return card;
        }
    }
}