package network.messages.actions8;

import network.messages.Message;

public class GameFinishedMessage extends Message {

    private final MessageBody messageBody;

    public GameFinishedMessage(int clientID) {
        super("GameFinished");
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