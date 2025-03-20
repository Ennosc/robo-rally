package network.messages.actions8;

import network.messages.Message;
import java.util.List;

public class PickDamageMessage extends Message {

    private final MessageBody messageBody;

    public PickDamageMessage(int count, List<String> availablePiles) {
        super("PickDamage");
        this.messageBody = new MessageBody(count, availablePiles);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class MessageBody {
        private final int count;
        private final List<String> availablePiles;

        public MessageBody(int count, List<String> availablePiles) {
            this.count = count;
            this.availablePiles = availablePiles;
        }

        public int getCount() {
            return count;
        }

        public List<String> getAvailablePiles() {
            return availablePiles;
        }
    }
}