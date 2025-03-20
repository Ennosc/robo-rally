package network.messages.phases7;

import network.messages.Message;

/**
 * Message indicating the current active phase of the game.
 */
public class ActivePhaseMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an ActivePhaseMessage.
     *
     * @param phase the phase number (e.g., 0 for setup, 1 for upgrade, etc.).
     */
    public ActivePhaseMessage(int phase) {
        super("ActivePhase");
        this.messageBody = new MessageBody(phase);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int phase;

        /**
         * Constructs a MessageBody with the given phase.
         *
         * @param phase the phase number.
         */
        public MessageBody(int phase) {
            this.phase = phase;
        }

        public int getPhase() {
            return phase;
        }
    }
}