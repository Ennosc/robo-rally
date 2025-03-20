package network.messages.connection2;

import network.messages.Message;

public class HelloServerMessage extends Message {

    private final MessageBody messageBody;

    public HelloServerMessage(String group, boolean isAI, String protocol, int clientID) {
        super("HelloServer");
        this.messageBody = new MessageBody(group, isAI, protocol, clientID);
    }

    public static class MessageBody {
        private final String group;
        private final boolean isAI;
        private final String protocol;
        private final int clientID;

        public MessageBody(String group, boolean isAI, String protocol, int clientID) {
            this.group = group;
            this.isAI = isAI;
            this.protocol = protocol;
            this.clientID = clientID;
        }

        public String getGroup() {
            return group;
        }

        public boolean getisAI() {
            return isAI;
        }

        public String getProtocol() {
            return protocol;
        }

        public int getClientID() {
            return clientID;
        }
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}