package network.messages.actions8;

import network.messages.Message;

public class PlayerTurningMessage extends Message {

    private final MessageBody messageBody;

    public PlayerTurningMessage(int clientID, String rotation) {
        super("PlayerTurning");
        this.messageBody = new MessageBody(clientID, rotation);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final String rotation;

        public MessageBody(int clientID, String rotation) {
            this.clientID = clientID;
            this.rotation = rotation;
        }

        public int getClientID() {
            return clientID;
        }

        public String getRotation() {
            return rotation;
        }
    }
}