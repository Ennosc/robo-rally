package network.messages;

/**
 * Abstract base class for all messages exchanged between client and server.
 * <p>
 * Each message has a specific message type and a message body. Subclasses must implement
 * {@link #getMessageBody()} to return the body of the message.
 * </p>
 */
public abstract class Message {
    private final String messageType;

    protected Message(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public abstract Object getMessageBody();
}
