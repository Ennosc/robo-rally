package network.messages.connection2;

import network.messages.Message;

public class AliveMessage extends Message {

    private final MessageBody messageBody = new MessageBody();

    public AliveMessage() {
        super("Alive");
    }

    public static class MessageBody {
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}
