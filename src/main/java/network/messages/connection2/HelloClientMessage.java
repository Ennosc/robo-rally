package network.messages.connection2;

import network.messages.Message;

public class HelloClientMessage extends Message {

    private final MessageBody messageBody;

    public HelloClientMessage(String protocol) {
        super("HelloClient");
        this.messageBody = new MessageBody(protocol);
    }

    public static class MessageBody {
        private final String protocol;

        public MessageBody(String protocol) {
            this.protocol = protocol;
        }

        public String getProtocol() {
            return protocol;
        }
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}
