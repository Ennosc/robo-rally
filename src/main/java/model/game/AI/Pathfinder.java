package model.game.AI;

import model.game.Game;
import model.game.board.Board;
import model.game.board.Direction;
import model.game.board.Position;
import model.game.board.robots.Robot;
import model.game.board.tiles.Tile;
import model.game.cards.CardType;
import model.game.maps.MapParser;

import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import java.util.logging.Formatter;

import static model.game.cards.ProgrammingCardType.*;


/**
 * The Pathfinder class computes the optimal path for the AI by simulating card sequences.
 * It evaluates moves, distances, and fields to decide the best course toward a checkpoint.
 */
public class Pathfinder {
    private Board board;
    private Direction direction;
    private Position position;
    private final Map<Integer,Position> checkpoints = new HashMap<>();
    private int currentCheckpointIndex;
    private static final Logger logger = Logger.getLogger(Pathfinder.class.getName());
    private int[] startingPoint;

    /**
     * Constructs a Pathfinder and initializes its logger.
     */
    public Pathfinder(){
        initializeLogger();
    }

    /**
     * Returns ONE list of 5-card sequences (limited by cleverness) that maximize the score to reach the next checkpoint.
     *
     * @param start       the starting position
     * @param cardsInHand the list of available cards in hand
     * @param cleverness  the AIs cleverness level; higher means more permutations are considered
     * @return the best found card sequence
     */
    public List<String> bruteForceBestSequence(Position start,
                                               List<String> cardsInHand, int cleverness) {
        List<List<String>> allPermutations = new ArrayList<>();

        // Number of permutations per cleverness level of the AI
        if(cleverness == 0){
            allPermutations = generatePermutationsLimited(cardsInHand, 125);
        } else if(cleverness == 1){
            allPermutations = generatePermutationsLimited(cardsInHand, 500);
        } else if(cleverness == 2){
            allPermutations = generatePermutationsLimited(cardsInHand, Integer.MAX_VALUE);
        }

        List<String> bestSequence = null;
        int bestScore = 0;

        // Iterate over permutations of cards on hand
        for (List<String> sequence : allPermutations) {
            Robot tempRobot = new Robot();
            tempRobot.setBoard(board);
            tempRobot.setDirection(direction);
            tempRobot.selectStartingPosition(startingPoint);

            // simulate the card sequence for the given robot on the current board
            Map<Position, Integer> simulationResult = simulateCardSequence(start, tempRobot, sequence);
            Map.Entry<Position, Integer> entry = simulationResult.entrySet().iterator().next();
            int score = entry.getValue();

            // Only keep the best permutation / the highest score
            if (score > bestScore) {
                bestScore = score;
                bestSequence = sequence;
            }
            // Remove testRobots
            board.clearRobotPositions();
        }
        logger.info("Best Sequence to reach the CP:"+bestSequence);
        return bestSequence;
    }

    /**
     * Generates all permutations of 5 cards from the hand, up to a maximum number.
     *
     * @param cardsInHand   the available cards
     * @param maxPermutations the maximum number of permutations to generate
     * @return a list of valid 5-card sequences
     */
    private List<List<String>> generatePermutationsLimited(List<String> cardsInHand, int maxPermutations) {
        List<List<String>> result = new ArrayList<>();
        boolean[] used = new boolean[cardsInHand.size()];
        generateLimitedSequences(cardsInHand, used, new ArrayList<>(), result, maxPermutations);
        return result;
    }

    /**
     * Recursively builds 5-card sequences and stops when the limit is reached.
     *
     * @param cardsInHand             available cards
     * @param used                    boolean array marking used cards
     * @param currentRegisterCards    current sequence being built
     * @param registerCardPermutations collection of valid sequences
     * @param maxPermutations         maximum sequences allowed
     */
    private void generateLimitedSequences(
            List<String> cardsInHand,
            boolean[] used,
            List<String> currentRegisterCards,
            List<List<String>> registerCardPermutations,
            int maxPermutations) {

        // Return when limit is reached
        if (registerCardPermutations.size() >= maxPermutations) {
            return;
        }

        // Add if a valid 5 card sequence was reached
        if (currentRegisterCards.size() == 5) {
            if (!currentRegisterCards.getFirst().equalsIgnoreCase("Again")) {
                registerCardPermutations.add(new ArrayList<>(currentRegisterCards));
            }
            return;
        }

        // recursive iterations
        for (int i = 0; i < cardsInHand.size(); i++) {
            if (!used[i]) {
                used[i] = true;
                currentRegisterCards.add(cardsInHand.get(i));

                generateLimitedSequences(cardsInHand, used, currentRegisterCards,
                        registerCardPermutations, maxPermutations);

                currentRegisterCards.removeLast();
                used[i] = false;
            }
        }
    }


    /**
     * Simulates executing a sequence of cards from a start position.
     *
     * @param start        The starting position of the robot.
     * @param testRobot    A robot instance to simulate moves.
     * @param cardSequence The sequence of cards to simulate.
     * @return A map entry with the final position (after an extra move) as key and its score as value.
     */
    private Map<Position, Integer> simulateCardSequence(Position start, Robot testRobot, List<String> cardSequence) {
        int temporaryCheckpointIndex = currentCheckpointIndex;
        board.placeRobot(testRobot, start.x(), start.y());
        int totalPenalty = 0;

        // Simulate card/tile effects per register
        for (int register = 0; register < cardSequence.size(); register++) {
            int currentRegisterIndex = register;
            String cardName = cardSequence.get(register);

            // Penalty for playing damage cards, higher for earlier registers
            if (DAMAGE_CARDS.contains(cardName)) {
                totalPenalty+= (int)((6-register) * Math.pow(10,temporaryCheckpointIndex));
                continue;
            }

            // Handling of again card logic
            while(cardName.equalsIgnoreCase("Again")){
                currentRegisterIndex--;
                cardName = cardSequence.get(currentRegisterIndex);
            }
            CardType type = Game.getInstance().getCardTypeByName(cardName);

            simulateMove(register,testRobot,type);

            // Bonus if final Checkpoint is reached in an earlier register
            Position currentPos = board.getRobotPositions(testRobot);
            if (currentPos.equals(getCheckpointPosition(temporaryCheckpointIndex))) {
                if (temporaryCheckpointIndex < checkpoints.size()) {
                    if (temporaryCheckpointIndex == checkpoints.size()-1) {
                        totalPenalty -= (int) Math.pow(10, temporaryCheckpointIndex+1) * (5 - register);
                    }
                    temporaryCheckpointIndex++;
                }
            }
            totalPenalty += evaluateTilePenalty(currentPos, temporaryCheckpointIndex);

            // If robot is rebooting, do not simulate additional cards
            if (testRobot.getIsRebooting()) {
                break;
            }
        }
        Position finalPosition = board.getRobotPositions(testRobot);

        // Simulation of an additional straight move (MoveI / MoveII)
        Position posAfterMove;
        if(!testRobot.getIsRebooting()){
            posAfterMove = simulateExtraMove(finalPosition, testRobot, temporaryCheckpointIndex);
            // Penalty if the robot is facing a wall (-> worse for next round)
            if(posAfterMove.equals(finalPosition)){
                totalPenalty += (int) Math.pow(10, temporaryCheckpointIndex);
            }
        } else {
            posAfterMove = new Position(finalPosition.x(), finalPosition.y());
        }

        // Calculate a score based on distance to next CheckPoint and collected penalties
        int score = evaluatePosition(posAfterMove, testRobot, temporaryCheckpointIndex)
                    - totalPenalty;

        Map<Position, Integer> simulationResult = new HashMap<>();
        simulationResult.put(finalPosition, score);
        return simulationResult;
    }


    /**
     * Chooses the best position after simulating extra steps with MOVE_1 and MOVE_2 cards. This way
     * we do not just account for the current position, but have the robot take the best possible position
     * for the next round as well.
     *
     * @param startPosition            The starting position for extra move simulation.
     * @param robot                    The robot used for simulation.
     * @param temporaryCheckpointIndex The index of the current checkpoint target.
     * @return The best new position after applying additional moves.
     */
    private Position simulateExtraMove(Position startPosition, Robot robot, int temporaryCheckpointIndex) {
        List<CardType> cardTypesToPlay = new ArrayList<>(Arrays.asList(MOVE_1, MOVE_2));

        if(startPosition.equals(getCheckpointPosition(temporaryCheckpointIndex))) {
            return startPosition;
        } else {
            Position bestPosition = null;
            int bestEval = Integer.MIN_VALUE;
            for(CardType cardType : cardTypesToPlay) {
                int tempCPIndexCopy = temporaryCheckpointIndex;
                board.placeRobot(robot, startPosition.x(), startPosition.y());
                simulateMove(0,robot,cardType);
                Position newPosition = board.getRobotPositions(robot);
                if(board.isCheckpointTile(newPosition) && newPosition.equals(getCheckpointPosition(tempCPIndexCopy)) &&
                   tempCPIndexCopy < checkpoints.size()) {
                    tempCPIndexCopy++;
                }
                int currentEval = evaluatePosition(newPosition, robot, tempCPIndexCopy);
                if(bestPosition == null || currentEval > bestEval) {
                    bestPosition = newPosition;
                    bestEval = currentEval;
                }
            }
            return bestPosition;
        }
    }


    /**
     * Calculates a score for a given position relative to the checkpoint, considering
     * distance weighted by checkpoint number and reboot penalties.
     *
     * @param pos            The position to evaluate.
     * @param robot          The robot at the position.
     * @param checkpointIndex The index of the current checkpoint.
     * @return The evaluated score for the position.
     */
    private int evaluatePosition(Position pos, Robot robot, int checkpointIndex) {
        int distance = evaluateDistance(pos, getCheckpointPosition(checkpointIndex));
        int score = adjustDistanceForCheckpoint(distance, checkpointIndex);
        if(robot.getIsRebooting()) {
            score -= (int) (5 * Math.pow(10, checkpointIndex));
        }
        return score;
    }

    /**
     * Evaluates penalties for landing on specific tile types.
     *
     * @param pos             The position of the robot.
     * @param checkpointIndex The index of the current checkpoint.
     * @return The penalty score for the tile at the given position.
     */
    private int evaluateTilePenalty(Position pos, int checkpointIndex) {
        int penalty = 0;
        // Penalty for LaserTiles
        if (board.isLaserTileAt(pos)) {
            penalty += (int) Math.pow(10, checkpointIndex);
        }
            // Bonus for EnergyTiles
        if (board.isEnergySpaceTile(pos)) {
            penalty -= (int) (0.1 * Math.pow(10, checkpointIndex));
        }
        return penalty;
    }



    /**
     * Calculates the absolute distance between two positions.
     *
     * @param pos  The first position.
     * @param goal The target position.
     * @return The Manhattan distance between pos and goal.
     */
    private int evaluateDistance(Position pos, Position goal) {
        return Math.abs(pos.x() - goal.x()) + Math.abs(pos.y() - goal.y());
    }

    /**
     * Adjusts the distance score by checkpoint priority. By heavily increasing scores for higher
     * checkpoints, robots are motivated to reach checkpoints as quickly as possible.
     *
     * @param distance          The distance to the goal.
     * @param checkPointIndex   The index of the current checkpoint.
     * @return A score reflecting proximity to checkpoint, prioritizing higher checkpoints.
     */
    public int adjustDistanceForCheckpoint(int distance, int checkPointIndex) {
        int bonusForCheckpoint = 0;
        int baseValue = (int) Math.pow(10, (checkPointIndex + 2));
        int distanceFactor = (int) (distance * Math.pow(10, checkPointIndex));
        int distanceScore = baseValue - distanceFactor;

        if (distanceScore == 0) {
            bonusForCheckpoint = (int) (baseValue * 0.1);
        }
        return distanceScore + bonusForCheckpoint;
    }


    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    private static final Set<String> DAMAGE_CARDS = new HashSet<>(Arrays.asList(
            "Spam",
            "Worm",
            "Trojan",
            "Virus"
    ));


    public void setCheckpoints(Map<Integer, Position> checkpointsMap) {
        checkpoints.clear();
        checkpoints.putAll(checkpointsMap);
        currentCheckpointIndex = 1;
    }

    public Position getCheckpointPosition(int checkpointIndex) {
        return checkpoints.get(checkpointIndex);
    }

    /**
     * Increments the current checkpoint index if there are more checkpoints.
     */
    public void moveToNextCheckpoint() {
        if (currentCheckpointIndex < checkpoints.size()) {
            currentCheckpointIndex++;
        }
    }

    public int getCurrentCheckpointIndex() {
        return currentCheckpointIndex;
    }

    /**
     * Loads a board from a JSON file and initializes it.
     *
     * @param boardName the board file name (without ".json")
     */
    public void initializeBoard(String boardName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("maps/" + boardName + ".json");
        MapParser parser = new MapParser(inputStream);
        List<List<List<Tile>>> tileBoard = parser.getBoard();

        int rows = tileBoard.size();
        int cols = tileBoard.getFirst().size();
        Board newBoard = new Board(rows, cols);

        newBoard.initializeBoard(tileBoard);
        this.board = newBoard;
    }

    /**
     * Helps the AI to select a suitable direction after rebooting.
     * For this, distances for x and y are calculated and a MoveI card
     * is simulated to ensure the robot is not facing a wall.
     *
     * @param currentPosition   The current position of the robot.
     * @param targetPosition    The position of the next target tile.
     * @return                  The best direction to choose when rebooting.
     */
    public Direction findBestDirectionToGoal(Position currentPosition, Position targetPosition) {
        List<Direction> bestDirections = new ArrayList<>();
        // Identifies the direction(s) of the checkpoint relative to the current position
        if (currentPosition.x() < targetPosition.x()) {
            bestDirections.add(Direction.RIGHT);
        }
        if (currentPosition.x() > targetPosition.x()) {
            bestDirections.add(Direction.LEFT);
        }
        if (currentPosition.y() < targetPosition.y()) {
            bestDirections.add(Direction.BOTTOM);
        }
        if (currentPosition.y() > targetPosition.y()) {
            bestDirections.add(Direction.TOP);
        }

        // Selects a random order for the list of bestDirections
        Collections.shuffle(bestDirections, new Random());

        Robot tempRobot = new Robot();
        tempRobot.setBoard(board);
        tempRobot.selectStartingPosition(startingPoint);

        // Simulates a Move1 to prevent facing a wall
        for (Direction direction : bestDirections) {
            tempRobot.setDirection(direction);
            board.placeRobot(tempRobot, currentPosition.x(), currentPosition.y());

            simulateMove(0, tempRobot, MOVE_1);
            Position newPosition = board.getRobotPositions(tempRobot);

            // Checks if the robot has moved to a new position
            if (!newPosition.equals(currentPosition)) {
                logger.info("Selected Direction: " + direction + "for restarting.");
                board.clearRobotPositions();
                return direction;
            } else {
                logger.info("Direction: " + direction + " is blocked by a wall.");
            }
            board.clearRobotPositions();
        }

        logger.warning("No valid direction found towards the goal. Defaulting to UP.");
        return Direction.TOP;
    }


    public static void initializeLogger() {
        logger.setUseParentHandlers(false);

        // Create console handler with a custom formatter
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatLogger());

        // Add console handler
        logger.addHandler(consoleHandler);
    }

    public static java.util.logging.Formatter formatLogger() {
        return new Formatter() {
            @Override
            public String format(LogRecord record) {
                String color = switch (record.getLevel().getName()) {
                    case "SEVERE" -> "\u001B[31m"; // Red
                    case "WARNING" -> "\u001B[33m"; // Yellow
                    case "INFO" -> "\u001B[37m"; // White
                    default -> "\u001B[0m"; // Reset
                };

                String className = record.getSourceClassName() != null ? record.getSourceClassName() : "UnknownClass";
                String methodName = record.getSourceMethodName() != null ? record.getSourceMethodName() : "UnknownMethod";

                return String.format("%s[%s] [%s] [%s#%s] %s\u001B[0m%n",
                        color,
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())),
                        record.getLevel().getName(),
                        className,
                        methodName,
                        record.getMessage());
            }
        };
    }

    /**
     * Applies the effect of a card on a robot and activates board elements.
     *
     * @param register the register index for the move
     * @param robot    the robot to move
     * @param cardType the card effect to apply
     */

    private void simulateMove(int register, Robot robot, CardType cardType) {
        if(!(DAMAGE_CARDS.contains(cardType.toString()))){
            cardType.applyEffect(robot);
        }
        board.activateConveyorBelts();
        board.activatePushPanels(register);
        board.activateGears();
    }

    public void setStartingPoint(int[] position) {
        this.startingPoint = position;
    }
}