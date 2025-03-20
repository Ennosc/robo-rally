package network.messages.actions8;

import network.messages.Message;

public class ChooseRegisterMessage extends Message {

    private final MessageBody messageBody;

    public ChooseRegisterMessage(int register) {
        super("ChooseRegister");
        this.messageBody = new MessageBody(register);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int register;

        public MessageBody(int register) {
            this.register = register;
        }

        public int getRegister() {
            return register;
        }
    }
}