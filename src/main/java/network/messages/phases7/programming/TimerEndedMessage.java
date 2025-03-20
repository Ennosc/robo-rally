package network.messages.phases7.programming;

import network.messages.Message;
import java.util.List;

/**
 * Message indicating that a timer has ended.
 */
public class TimerEndedMessage extends Message {

    private final MessageBody messageBody;


    /**
     * Constructs a TimerEndedMessage.
     *
     * @param clientIDs the list of client IDs that did not complete their selection.
     */
    public TimerEndedMessage(List<Integer> clientIDs) {
        super("TimerEnded");
        this.messageBody = new MessageBody(clientIDs);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a TimerEndedMessage.
     */
    public static class MessageBody {
        private final List<Integer> clientIDs;

        /**
         * Constructs a MessageBody with the given list of client IDs.
         *
         * @param clientIDs the list of client IDs.
         */
        public MessageBody(List<Integer> clientIDs) {
            this.clientIDs = clientIDs;
        }

        public List<Integer> getClientIDs() {
            return clientIDs;
        }
    }
}