package network.messages.phases7.setup;

import network.messages.Message;

/**
 * Message used by a client to set a starting point during the setup phase.
 */
public class SetStartingPointMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a SetStartingPointMessage.
     *
     * @param x the x-coordinate of the chosen starting point.
     * @param y the y-coordinate of the chosen starting point.
     */
    public SetStartingPointMessage(int x, int y) {
        super("SetStartingPoint");
        this.messageBody = new MessageBody(x, y);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a SetStartingPointMessage.
     */
    public static class MessageBody {
        private final int x;
        private final int y;

        /**
         * Constructs a MessageBody with the given coordinates.
         *
         * @param x the x-coordinate.
         * @param y the y-coordinate.
         */
        public MessageBody(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}