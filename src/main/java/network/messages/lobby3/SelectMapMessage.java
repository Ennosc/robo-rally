package network.messages.lobby3;

import network.messages.Message;
import java.util.List;

/**
 * Message sent to prompt a client to select a map.
 * <p>
 * Contains a list of available map names.
 * </p>
 */
public class SelectMapMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a SelectMapMessage with the specified list of available maps.
     *
     * @param availableMaps a list of map names.
     */
    public SelectMapMessage(List<String> availableMaps) {
        super("SelectMap");
        this.messageBody = new MessageBody(availableMaps);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a SelectMapMessage.
     */
    public static class MessageBody {
        private final List<String> availableMaps;

        /**
         * Constructs a MessageBody with the specified list of available maps.
         *
         * @param availableMaps the list of map names.
         */
        public MessageBody(List<String> availableMaps) {
            this.availableMaps = availableMaps;
        }

        public List<String> getAvailableMaps() {
            return availableMaps;
        }
    }
}