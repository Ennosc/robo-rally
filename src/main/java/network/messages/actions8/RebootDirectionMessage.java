package network.messages.actions8;

import network.messages.Message;

public class RebootDirectionMessage extends Message {

    private final MessageBody messageBody;

    public RebootDirectionMessage(String direction) {
        super("RebootDirection");
        this.messageBody = new MessageBody(direction);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final String direction;

        public MessageBody(String direction) {
            this.direction = direction;
        }

        public String getDirection() {
            return direction;
        }
    }
}