package network.messages.lobby3;

import network.messages.Message;
import java.util.List;

/**
 * Message sent to indicate that the game has started.
 * <p>
 * Contains the starting energy value and a representation of the game map.
 * </p>
 */
public class GameStartedMessage extends Message {

    private final MessageBody messageBody;
    
    /**
     * Constructs a GameStartedMessage with the specified starting energy and game map.
     *
     * @param energy  the starting energy for each player.
     * @param gameMap the game map as a three-dimensional list of {@code Field} objects.
     */
    public GameStartedMessage(int energy, List<List<List<Field>>> gameMap) {
        super("GameStarted");
        this.messageBody = new MessageBody(energy, gameMap);
    }

    @Override
    public MessageBody getMessageBody() {
        return messageBody;
    }

    public static class Field {
        private String type;
        private String isOnBoard;
        private Integer speed;
        private List<String> orientations;
        private List<Integer> registers;
        private Integer count;

        public Field(String type, String isOnBoard, int speed, List<String> orientations,
                     List<Integer> registers, int count) {
            this.type = type;
            this.isOnBoard = isOnBoard;
            this.speed = speed;
            this.orientations = orientations;
            this.registers = registers;
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public String getIsOnBoard() {
            return isOnBoard;
        }

        public Integer getSpeed() {
            return speed;
        }

        public List<String> getOrientations() {
            return orientations;
        }

        public List<Integer> getRegisters() {
            return registers;
        }

        public Integer getCount() {
            return count;
        }
    }

    public static class MessageBody {
        private final int energy;
        private final List<List<List<Field>>> gameMap;

        public MessageBody(int energy, List<List<List<Field>>> gameMap) {
            this.energy = energy;
            this.gameMap = gameMap;
        }

        public int getEnergy() {
            return energy;
        }

        public List<List<List<Field>>> getGameMap() {
            return gameMap;
        }
    }
}