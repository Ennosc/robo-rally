package network.messages.actions8;

import network.messages.Message;

public class CheckPointReachedMessage extends Message {

    private final MessageBody messageBody;

    public CheckPointReachedMessage(int clientID, int number) {
        super("CheckPointReached");
        this.messageBody = new MessageBody(clientID, number);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final int number;

        public MessageBody(int clientID, int number) {
            this.clientID = clientID;
            this.number = number;
        }

        public int getClientID() {
            return clientID;
        }

        public int getNumber() {
            return number;
        }
    }
}