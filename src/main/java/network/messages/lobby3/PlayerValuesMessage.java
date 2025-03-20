package network.messages.lobby3;

import network.messages.Message;

/**
 * Message sent by a client to communicate its player values (name and chosen figure).
 */
public class PlayerValuesMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a PlayerValuesMessage with the specified player name and figure number.
     *
     * @param name   the player's name.
     * @param figure the chosen figure number.
     */
    public PlayerValuesMessage(String name, int figure) {
        super("PlayerValues");
        this.messageBody = new MessageBody(name, figure);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    /**
     * The body of a PlayerValuesMessage.
     */
    public static class MessageBody {
        private final String name;
        private final int figure;

        /**
         * Constructs a MessageBody with the specified name and figure.
         *
         * @param name   the player's name.
         * @param figure the chosen figure number.
         */
        public MessageBody(String name, int figure) {
            this.name = name;
            this.figure = figure;
        }

        public String getName() {
            return name;
        }

        public int getFigure() {
            return figure;
        }
    }
}