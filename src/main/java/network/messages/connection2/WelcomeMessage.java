package network.messages.connection2;

import network.messages.Message;

public class WelcomeMessage extends Message {

    private final MessageBody messageBody;

    public WelcomeMessage(int clientID) {
        super("Welcome");
        this.messageBody = new MessageBody(clientID);
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

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}
