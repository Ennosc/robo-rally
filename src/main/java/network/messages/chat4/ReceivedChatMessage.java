package network.messages.chat4;

import network.messages.Message;

public class ReceivedChatMessage extends Message {

    private final MessageBody messageBody;

    public ReceivedChatMessage(String message, int from, boolean isPrivate) {
        super("ReceivedChat");
        this.messageBody = new MessageBody(message, from, isPrivate);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final String message;
        private final int from;
        private final boolean isPrivate;

        public MessageBody(String message, int from, boolean isPrivate) {
            this.message = message;
            this.from = from;
            this.isPrivate = isPrivate;
        }

        public String getMessage() {
            return message;
        }

        public int getFrom() {
            return from;
        }

        public boolean getIsPrivate() {
            return isPrivate;
        }
    }
}