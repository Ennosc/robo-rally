package network.messages.actions8;

import network.messages.Message;

public class MovementMessage extends Message {

    private final MessageBody messageBody;

    public MovementMessage(int clientID, int x, int y) {
        super("Movement");
        this.messageBody = new MessageBody(clientID, x, y);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final int x;
        private final int y;

        public MessageBody(int clientID, int x, int y) {
            this.clientID = clientID;
            this.x = x;
            this.y = y;
        }

        public int getClientID() {
            return clientID;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}