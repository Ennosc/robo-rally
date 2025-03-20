package network.messages.specialMessage5;

import network.messages.Message;

/**
 * Represents an error message sent between the server and clients.
 */
public class ErrorMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an ErrorMessage with the specified error message.
     *
     * @param error the error message text.
     */
    public ErrorMessage(String error) {
        super("Error");
        this.messageBody = new MessageBody(error);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final String error;

        /**
         * Constructs a MessageBody with the given error.
         *
         * @param error the error message.
         */
        public MessageBody(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
