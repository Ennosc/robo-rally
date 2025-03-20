package network.messages.actions8;

import network.messages.Message;

/**
 * Constructs a CheckpointMovedMessage with the specified animation type.
 */
public class CheckpointMovedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an CheckpointMovedMessage with the specified animation type.
     */
    public CheckpointMovedMessage(int checkpointID, int x, int y) {
        super("CheckpointMoved");
        this.messageBody = new MessageBody(checkpointID, x, y);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int checkpointID;
        private final int x;
        private final int y;

        public MessageBody(int checkpointID, int x, int y) {
            this.checkpointID = checkpointID;
            this.x = x;
            this.y = y;
        }

        public int getCheckpointID() {
            return checkpointID;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}