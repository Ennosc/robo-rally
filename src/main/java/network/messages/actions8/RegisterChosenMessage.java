package network.messages.actions8;

import network.messages.Message;

public class RegisterChosenMessage extends Message {

    private final MessageBody messageBody;

    public RegisterChosenMessage(int clientID, int register) {
        super("RegisterChosen");
        this.messageBody = new MessageBody(clientID, register);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final int register;

        public MessageBody(int clientID, int register) {
            this.clientID = clientID;
            this.register = register;
        }

        public int getClientID() {
            return clientID;
        }

        public int getRegister() {
            return register;
        }
    }
}