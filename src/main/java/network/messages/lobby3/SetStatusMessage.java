package network.messages.lobby3;

import network.messages.Message;

/**
 * Message sent to indicate a change in a player's status (ready or not ready).
 */
public class SetStatusMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a SetStatusMessage with the specified readiness status.
     *
     * @param ready {@code true} if the player is ready; {@code false} otherwise.
     */
    public SetStatusMessage(boolean ready) {
        super("SetStatus");
        this.messageBody = new MessageBody(ready);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a SetStatusMessage.
     */
    public static class MessageBody {
        private final boolean ready;


        /**
         * Constructs a MessageBody with the given readiness status.
         *
         * @param ready the readiness flag.
         */
        public MessageBody(boolean ready) {
            this.ready = ready;
        }

        public boolean isReady() {
            return ready;
        }
    }
}