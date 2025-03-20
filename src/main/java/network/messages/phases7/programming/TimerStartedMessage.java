package network.messages.phases7.programming;

import network.messages.Message;

/**
 * Message indicating that a timer has started.
 */
public class TimerStartedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a TimerStartedMessage.
     */
    public TimerStartedMessage() {
        super("TimerStarted");
        this.messageBody = new MessageBody();
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
    }
}