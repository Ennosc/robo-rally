package network.messages.actions8;

import network.messages.Message;

public class EnergyMessage extends Message {

    private final MessageBody messageBody;

    public EnergyMessage(int clientID, int count, String source) {
        super("Energy");
        this.messageBody = new MessageBody(clientID, count, source);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final int count;
        private final String source;

        public MessageBody(int clientID, int count, String source) {
            this.clientID = clientID;
            this.count = count;
            this.source = source;
        }

        public int getClientID() {
            return clientID;
        }

        public int getCount() {
            return count;
        }

        public String getSource() {
            return source;
        }
    }
}