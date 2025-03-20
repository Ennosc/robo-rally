package network.messages.actions8;

import network.messages.Message;

public class RebootMessage extends Message {

    private final MessageBody messageBody;

    public RebootMessage(int clientID) {
        super("Reboot");
        this.messageBody = new MessageBody(clientID);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;

        public MessageBody(int clientID) {
            this.clientID = clientID;
        }

        public int getClientID() {
            return clientID;
        }
    }
}