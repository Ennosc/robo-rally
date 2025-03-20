package network.messages.phases7.programming;

import network.messages.Message;

/**
 * Message sent by a client to indicate which programming card they have selected for a register.
 */
public class SelectedCardMessage extends Message {

    private final MessageBody messageBody;

    /**
     * Constructs a SelectedCardMessage.
     *
     * @param card     the name of the selected card.
     * @param register the register position.
     */
    public SelectedCardMessage(String card, int register) {
        super("SelectedCard");
        this.messageBody = new MessageBody(card, register);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }


    /**
     * The body of a SelectedCardMessage.
     */
    public static class MessageBody {
        private final String card;
        private final int register;

        /**
         * Constructs a MessageBody with the specified card and register.
         *
         * @param card     the card name.
         * @param register the register position.
         */
        public MessageBody(String card, int register) {
            this.card = card;
            this.register = register;
        }

        public String getCard() {
            return card;
        }

        public int getRegister() {
            return register;
        }
    }
}