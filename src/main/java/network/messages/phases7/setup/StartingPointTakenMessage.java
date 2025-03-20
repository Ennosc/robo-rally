package network.messages.phases7.setup;

import network.messages.Message;

/**
 * Message indicating that a starting point has been taken by a player.
 */
public class StartingPointTakenMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a StartingPointTakenMessage.
     *
     * @param x         the x-coordinate of the starting point.
     * @param y         the y-coordinate of the starting point.
     * @param direction the robot's starting direction (as a string).
     * @param clientID  the ID of the client who has taken the starting point.
     */
    public StartingPointTakenMessage(int x, int y, String direction, int clientID) {
        super("StartingPointTaken");
        this.messageBody = new MessageBody(x, y, direction, clientID);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a StartingPointTakenMessage.
     */
    public static class MessageBody {
        private final int x;
        private final int y;
        private final String direction;
        private final int clientID;


        /**
         * Constructs a MessageBody with the given parameters.
         *
         * @param x         the x-coordinate.
         * @param y         the y-coordinate.
         * @param direction the direction.
         * @param clientID  the client ID.
         */
        public MessageBody(int x, int y, String direction, int clientID) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.clientID = clientID;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String getDirection() {
            return direction;
        }

        public int getClientID() {
            return clientID;
        }
    }
}