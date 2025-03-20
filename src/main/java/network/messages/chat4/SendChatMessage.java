package network.messages.chat4;

import network.messages.Message;

public class SendChatMessage extends Message {

    private final MessageBody messageBody;

    public SendChatMessage(String message, int to) {
        super("SendChat");
        this.messageBody = new MessageBody(message, to);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final String message;
        private final int to;

        public MessageBody(String message, int to) {
            this.message = message;
            this.to = to;
        }

        public String getMessage() {
            return message;
        }

        public int getTo() {
            return to;
        }
    }
}
