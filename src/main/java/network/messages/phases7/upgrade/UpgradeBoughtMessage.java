package network.messages.phases7.upgrade;

import network.messages.Message;

/**
 * Message indicating that a player has bought an upgrade.
 */
public class UpgradeBoughtMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs an UpgradeBoughtMessage.
     *
     * @param clientID the ID of the player who bought the upgrade.
     * @param card     the name of the upgrade card bought.
     */
    public UpgradeBoughtMessage(int clientID, String card) {
        super("UpgradeBought");
        this.messageBody = new MessageBody(clientID, card);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int clientID;
        private final String card;

        /**
         * Constructs a MessageBody with the specified parameters.
         *
         * @param clientID the player's ID.
         * @param card     the upgrade card name.
         */
        public MessageBody(int clientID, String card) {
            this.clientID = clientID;
            this.card = card;
        }

        public int getClientID() {
            return clientID;
        }

        public String getCard() {
            return card;
        }
    }
}
