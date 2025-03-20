package network.messages.actions8;

import network.messages.Message;

/**
 * Represents an animation message that instructs the client to perform a specific animation.
 */
public class AnimationMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an AnimationMessage with the specified animation type.
     *
     * @param type the type of animation to be performed.
     */
    public AnimationMessage(String type) {
        super("Animation");
        this.messageBody = new MessageBody(type);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * Represents the body of the AnimationMessage.
     */
    public static class MessageBody {
        private final String type;

        /**
         * Constructs a MessageBody with the given animation type.
         *
         * @param type the type of animation.
         */
        public MessageBody(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}