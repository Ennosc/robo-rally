package model.game.AI;

import model.game.board.Direction;
import model.game.board.Position;
import model.server_client.BaseClient;
import network.interpreters.AIJsonInterpreter;
import network.JsonHandler;
import network.messages.actions8.RebootDirectionMessage;
import network.messages.cards6.PlayCardMessage;
import network.messages.lobby3.PlayerValuesMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class simulates an automated player. It extends
 * BaseClient and makes decisions based on the current game phase.
 */
public class AIClient extends BaseClient {
    private int energy;
    private int currentPhase;
    private List<String> hand;
    private List<Position> availableStartingPoints;
    private String cardInCurrentRegister;
    private Position position;
    private Direction direction;
    private Pathfinder pathfinder;
    private int phase;
    private final List<Integer> availableFigures = new ArrayList<>();
    private final int cleverness;
    private AIJsonInterpreter aiJsonInterpreter;
    private final Map<Integer, List<String>> availableAINames = new HashMap<>();


    /**
     * Constructs an AIClient with a given cleverness.
     *
     * @param cleverness the AI's cleverness level (0 = basic, 1 = medium, 2 = smart)
     */
    public AIClient(int cleverness) {
        super();
        this.cleverness = cleverness;
        initialize();
    }

    /**
     * Constructs an AIClient connecting to a specified host and port with a given cleverness.
     *
     * @param host       the server host
     * @param port       the server port
     * @param cleverness the AI's cleverness level
     */
    public AIClient(String host, int port, int cleverness) {
        super(host, port);
        this.cleverness = cleverness;
        initialize();
    }

    /**
     * Initializes the AI clients state at startUp.
     */
    private void initialize() {
        this.hand = new ArrayList<>();
        this.availableStartingPoints = new ArrayList<>();
        this.pathfinder = new Pathfinder();
        this.aiJsonInterpreter = new AIJsonInterpreter(this, logger);
        this.isAI = true;
        initializeLogger();
        for (int figureIndex = 0; figureIndex < 6; figureIndex++) {
            this.availableFigures.add(figureIndex);
        }
        availableAINames.put(0, new ArrayList<>(Arrays.asList(
                "MediocreMike",
                "WeakWilhelm",
                "BadBetty",
                "SlowSiggi",
                "NewNina",
                "ClumsyCarl"
        )));
        availableAINames.put(1, new ArrayList<>(Arrays.asList(
                "CasualCorinna",
                "MidMartin",
                "AverageAndy",
                "ModestMolly",
                "StandardSteve",
                "OrdinaryOlivia"
        )));
        availableAINames.put(2, new ArrayList<>(Arrays.asList(
                "ExceptionalElif",
                "ExcellentEnno",
                "KrasseKim",
                "KnockoutKevin",
                "FabulousFelix",
                "LegendaryLouis"
        )));
    }

    public void setPosition(Position pos) {
        this.position = pos;
    }

    /**
     * Handles the backend handover of rotational information.
     *
     * @param rotation "clockwise" or "counterclockwise"
     */
    public void rotate(String rotation) {
        this.direction = direction.rotate(rotation);
        pathfinder.setDirection(direction);
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
        pathfinder.setDirection(this.direction);
    }

    @Override
    protected void listenForMessages() {
        new Thread(() -> {
            try {
                String messageFromServer;
                while ((messageFromServer = in.readLine()) != null) {
                    logger.info("Received message: " + messageFromServer);
                    interpretMessage(messageFromServer);
                }
            } catch (IOException e) {
                logger.severe("Something went wrong: " + e.getMessage());
            }
        }).start();
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getEnergy() {
        return energy;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(int newPhase) {
        currentPhase = newPhase;
    }

    /**
     * This is the main action class of the AIClient. It determines and
     * performs actions according to the current phase:
     * 0 - set starting point, 1 - buy upgrade, 2 - select cards, 3 - play card.
     */
    public void takeAction() {
        int currentPhase = getCurrentPhase();
        switch (currentPhase) {
            case 0:
                setStartingPoint();
                break;
            case 1:
                selectUpgrade();
                break;
            case 2:
                playUpgrade();
                selectProgrammingCards();
                break;
            case 3:
                sendPlayCard();
                break;
            default:
                logger.warning("Unknown phase " + currentPhase);
        }
    }

    /**
     * Determines a reboot direction using the pathfinder and sends it to the server.
     */
    public void chooseRebootDirection(){
        Direction rebootDirection = getPathfinder().findBestDirectionToGoal(position,
                getPathfinder().getCheckpointPosition(getPathfinder().getCurrentCheckpointIndex()));
        sendRebootDirection(rebootDirection.toLowercaseString());
    }

    /**
     * Chooses a random starting point from the available options and notifies the server.
     */
    public void setStartingPoint() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            List<Position> startingPointsList = getAvailableStartingPoints();
            int randomIndex = (int) (Math.random() * startingPointsList.size());
            Position selectedPoint = startingPointsList.get(randomIndex);
            sendSetStartingPoint(selectedPoint.x(), selectedPoint.y());
            getPathfinder().setStartingPoint(new int[]{selectedPoint.x(), selectedPoint.y()});
            scheduler.shutdown();
        }, 3, TimeUnit.SECONDS);

    }


    /**
     * Attempts to buy available upgrade cards if the player has enough energy.
     * Prioritizes purchasing "RearLaser" or "SpamBlocker" if the player has at least 2 energy.
     * If no upgrade is selected, a "no purchase" request is sent.
     */
    public void selectUpgrade() {
        sendBuyUpgrade(false, null);
    }


    public void setHand(List<String> newHand) {
        this.hand.clear();
        this.hand.addAll(newHand);
    }

    public synchronized void setAvailableStartingPoints(List<Position> startingPoints) {
        this.availableStartingPoints.clear();
        this.availableStartingPoints.addAll(startingPoints);
    }

    public synchronized List<Position> getAvailableStartingPoints() {
        return new ArrayList<>(availableStartingPoints);
    }

    public synchronized void removeStartingPoint(Position pos) {
        availableStartingPoints.removeIf(position -> position.equals(pos));
    }

    /**
     * Sends a play card message using the card currently in the register.
     */
    public void sendPlayCard() {
        PlayCardMessage pcm = new PlayCardMessage(cardInCurrentRegister);
        String json = JsonHandler.toJson(pcm);
        sendMessageToServer(json);
        setCardInCurrentRegister(null);
    }

    public void setCardInCurrentRegister(String cardInCurrentRegister) {
        this.cardInCurrentRegister = cardInCurrentRegister;
    }

    public void setStartingDirection(Direction direction) {
        this.direction = direction;
        pathfinder.setDirection(direction);
    }

    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public void removeFigureIdFromList(int figureId) {
        synchronized (availableFigures) {
            this.availableFigures.remove(Integer.valueOf(figureId));
        }

    }

    /**
     * Sends player values (name/figure) to the server after selecting them randomly.
     */
    public List<String> getHand() {
        return gameDataBridge.getCardsInHand();
    }

    public void sendPlayerValues() {
        synchronized (availableAINames) {
            List<String> namesForCleverness = availableAINames.get(this.cleverness);
            if (namesForCleverness != null && !namesForCleverness.isEmpty()) {
                int randomIndex = new Random().nextInt(namesForCleverness.size());
                this.clientName = namesForCleverness.get(randomIndex);
                namesForCleverness.remove(randomIndex);
            }
        }

        if (availableFigures.isEmpty()) {
            logger.warning("No available figures left!");
            return;
        }

        Random random = new Random();
        int randomIndexFigure = random.nextInt(availableFigures.size());
        int selectedFigureId = availableFigures.get(randomIndexFigure);
        PlayerValuesMessage pvm = new PlayerValuesMessage(clientName, selectedFigureId);
        String json = JsonHandler.toJson(pvm);
        sendMessageToServer(json);
    }

    @Override
    protected void interpretMessage(String message){
        aiJsonInterpreter.interpretMessage(message);
    }

    public void sendRebootDirection(String rebootDirection){
        RebootDirectionMessage rdm = new RebootDirectionMessage(rebootDirection);
        String json = JsonHandler.toJson(rdm);
        sendMessageToServer(json);
        pathfinder.setDirection(Direction.valueOf(rebootDirection.toUpperCase()));
    }


    public void playUpgrade() {
        List<String> personalUpgradeCards = gameDataBridge.getClientIDToBoughtUpgradeCards().get(clientId);
        if (personalUpgradeCards == null) {
            personalUpgradeCards = new ArrayList<>();
        }
        for (String upgradeCard : personalUpgradeCards) {
            if(hand.contains("Spam")) {
                if((upgradeCard != null) && upgradeCard.equals("SpamBlocker") && gameDataBridge.getClientIDToEnergy().get(clientId) >= 3) {
                    logger.info("play SpamBlocker");
                    sendPlayCard("SpamBlocker");
                }
            }
        }
    }

    public void selectProgrammingCards() {
        int cleverness = this.cleverness;
        Logger logger = Logger.getLogger(AIClient.class.getName());
        Pathfinder pathfinder = getPathfinder();  // Get the unique Pathfinder for this client
        logger.info("SmartAIClient Hand: " + hand);
        logger.info("Position: " + position + " Direction: " + pathfinder.getDirection());

        // calculates the best possible sequence for a given hand, limited by cleverness of the AI.
        List<String> bestCards = pathfinder.bruteForceBestSequence(position, hand, cleverness);

        new Thread(() -> {
            Random random = new Random();
            for (int i = 0; i < bestCards.size(); i++) {
                try {
                    // waits between 2 and 4 seconds to send the next card to simulate thinking
                    long delay = 2000 + random.nextInt(2000);
                    Thread.sleep(delay);
                    sendSelectedCard(bestCards.get(i), i);
                } catch (InterruptedException e) {
                    logger.warning("Card sending thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }

            }
            //play Upgrade Card if available
        }).start();
    }


    public void removeNameFromAvailableNames(String name) {
        synchronized (availableAINames) {
            for (List<String> nameList : availableAINames.values()) {
                nameList.remove(name);
            }
        }
    }
}