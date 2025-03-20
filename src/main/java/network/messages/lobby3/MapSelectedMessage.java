package network.messages.lobby3;

import network.messages.Message;

/**
 * Message sent to inform clients which map has been selected.
 */
public class MapSelectedMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a MapSelectedMessage with the specified map name.
     *
     * @param map the name of the selected map.
     */
    public MapSelectedMessage(String map) {
        super("MapSelected");
        this.messageBody = new MessageBody(map);
    }

    /**
     * The body of a MapSelectedMessage.
     */
    public static class MessageBody {
        private final String map;

        /**
         * Constructs a MessageBody with the given map name.
         *
         * @param map the selected map name.
         */
        public MessageBody(String map) {
            this.map = map;
        }

        public String getMap() {
            return map;
        }
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }
}
