package network.interpreters;

import model.game.AI.AIClient;
import model.game.board.Direction;
import model.game.board.Position;
import network.JsonHandler;
import network.messages.actions8.*;
import network.messages.connection2.WelcomeMessage;
import network.messages.lobby3.GameStartedMessage;
import network.messages.lobby3.PlayerAddedMessage;
import network.messages.lobby3.PlayerStatusMessage;
import network.messages.phases7.ActivePhaseMessage;
import network.messages.phases7.CurrentPlayerMessage;
import network.messages.phases7.activation.CurrentCardsMessage;
import network.messages.phases7.programming.YourCardsMessage;
import network.messages.phases7.setup.StartingPointTakenMessage;
import network.messages.lobby3.MapSelectedMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * The AIJsonInterpreter class processes JSON messages received by an AI client.
 * It extends the BaseClientJsonInterpreter and implements the JsonInterpreter interface.
 * This class sets up handlers for various message types to perform the corresponding actions.
 */
public class AIJsonInterpreter extends BaseClientJsonInterpreter implements JsonInterpreter {
    AIClient aiClient;

    /**
     * Constructs a new AIJsonInterpreter for handling JSON messages for the given AI client.
     *
     * @param aiClient the AIClient instance associated with this interpreter.
     * @param logger   the Logger used for logging events and errors.
     */
    public AIJsonInterpreter(AIClient aiClient, Logger logger) {
        super(aiClient, logger);
        this.aiClient = aiClient;  // Ensure it's stored
        modifyHandlers();
    }

    /**
     * Configures the mapping between JSON message types and their corresponding handler functions.
     * <p>
     * This method populates the handlerMap with lambda functions that process each message type.
     * For example, when a "Welcome" message is received, the AI client updates its client ID and
     * subsequently sends its player values. Other handlers update the AI client's state based on
     * game events such as player addition, movement, phase changes, and game start/finish events.
     * </p>
     */
    @Override
    protected void modifyHandlers() {
        handlerMap.put("Welcome", jsonObj -> {
            WelcomeMessage welcomeMessage = JsonHandler.fromJson(jsonObj.toString(), WelcomeMessage.class);
            aiClient.setClientID(welcomeMessage.getMessageBody().getClientID());

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread was interrupted during sleep.");
                }
                aiClient.sendPlayerValues();
            }).start();
        });
        handlerMap.put("PlayerAdded", jsonObj -> {
            PlayerAddedMessage pam = JsonHandler.fromJson(jsonObj.toString(), PlayerAddedMessage.class);
            int figureNumber = pam.getMessageBody().getFigure();
            String playerName = pam.getMessageBody().getName();
            aiClient.removeFigureIdFromList(figureNumber);
            aiClient.removeNameFromAvailableNames(playerName);
        });

        handlerMap.put("CurrentPlayer", jsonObj -> {
        CurrentPlayerMessage cpm = JsonHandler.fromJson(jsonObj.toString(), CurrentPlayerMessage.class);
        int currentPlayerID = cpm.getMessageBody().getClientID();
        if (currentPlayerID == aiClient.getClientID() && (aiClient.getCurrentPhase() != 2)) {
            logger.info("AI is current player");
            aiClient.takeAction();
        }
        });

        handlerMap.put("PlayerStatus", jsonObj -> {
            PlayerStatusMessage psm = JsonHandler.fromJson(jsonObj.toString(), PlayerStatusMessage.class);
            boolean status = psm.getMessageBody().isReady();
            if (status && !aiClient.isReady()) {
                aiClient.sendSetStatus(true);
            } else if (!status && aiClient.isReady()) {
                aiClient.sendSetStatus(false);
            }
        });

        handlerMap.put("ActivePhase", jsonObj -> {
            ActivePhaseMessage apm = JsonHandler.fromJson(jsonObj.toString(), ActivePhaseMessage.class);
            int phase = apm.getMessageBody().getPhase();
            aiClient.setCurrentPhase(phase);
        });



        handlerMap.put("YourCards", jsonObj -> {
            YourCardsMessage ycm = JsonHandler.fromJson(jsonObj.toString(), YourCardsMessage.class);
            List<String> receivedCards = ycm.getMessageBody().getCardsInHand();
            aiClient.setHand(receivedCards);
            if (aiClient.getCurrentPhase() == 2) {
                aiClient.takeAction();
            }
        });

        handlerMap.put("MapSelected", jsonObj -> {
        MapSelectedMessage ms = JsonHandler.fromJson(jsonObj.toString(), MapSelectedMessage.class);
        String map = ms.getMessageBody().getMap().toLowerCase();
        map = map.replaceAll("\\s+", "").toLowerCase();
        aiClient.getPathfinder().initializeBoard(map);
        });

        handlerMap.put("CurrentCards", jsonObj -> {
            CurrentCardsMessage ccm = JsonHandler.fromJson(jsonObj.toString(), CurrentCardsMessage.class);
            for (CurrentCardsMessage.ActiveCard ac : ccm.getMessageBody().getActiveCards()) {
                if (ac.getClientID() == aiClient.getClientID()) {
                    aiClient.setCardInCurrentRegister(ac.getCard());
                }
            }
        });
        handlerMap.put("StartingPointTaken", jsonObj -> {
        StartingPointTakenMessage sptm = JsonHandler.fromJson(jsonObj.toString(), StartingPointTakenMessage.class);
        int clientIDStart = sptm.getMessageBody().getClientID();
        String startingDirection = sptm.getMessageBody().getDirection();
        Position startingPosition = new Position(sptm.getMessageBody().getX(), sptm.getMessageBody().getY());

        aiClient.removeStartingPoint(startingPosition);
        if (clientIDStart == aiClient.getClientID()) {
            aiClient.setPosition(startingPosition);
            aiClient.setStartingDirection(Direction.fromString(startingDirection));
        }
        });

        handlerMap.put("Movement", jsonObj -> {
            MovementMessage mm = JsonHandler.fromJson(jsonObj.toString(), MovementMessage.class);
            Position pos = new Position(mm.getMessageBody().getX(), mm.getMessageBody().getY());
            if (aiClient.getClientID() == mm.getMessageBody().getClientID()) {
                aiClient.setPosition(pos);
            }
        });

        handlerMap.put("PlayerTurning", jsonObj -> {
            PlayerTurningMessage ptm = JsonHandler.fromJson(jsonObj.toString(), PlayerTurningMessage.class);
            String rotation = ptm.getMessageBody().getRotation();
            if (aiClient.getClientID() == ptm.getMessageBody().getClientID()) {
                aiClient.rotate(rotation);
            }
        });

        handlerMap.put("Reboot", jsonObj -> {
            RebootMessage rm = JsonHandler.fromJson(jsonObj.toString(), RebootMessage.class);
            int clientIDReboot = rm.getMessageBody().getClientID();
            if (clientIDReboot == aiClient.getClientID()) {
                aiClient.chooseRebootDirection();
            }
        });

        handlerMap.put("CheckPointReached", jsonObj -> {
            CheckPointReachedMessage cprm = JsonHandler.fromJson(jsonObj.toString(), CheckPointReachedMessage.class);
            int clientIdCheckpoint = cprm.getMessageBody().getClientID();
            int checkpointId = cprm.getMessageBody().getNumber();

            if (clientIdCheckpoint == aiClient.getClientID()) {
                if (checkpointId == aiClient.getPathfinder().getCurrentCheckpointIndex()) {
                    logger.info("Checkpoint reached" + checkpointId);
                    aiClient.getPathfinder().moveToNextCheckpoint();
                }
            }
        });

        handlerMap.put("GameFinished", _ -> aiClient.shutdown());

        handlerMap.put("GameStarted", jsonObj -> {
            GameStartedMessage gsm = JsonHandler.fromJson(jsonObj.toString(), GameStartedMessage.class);
            List<List<List<GameStartedMessage.Field>>> gameMap = gsm.getMessageBody().getGameMap();

            // Extract starting points
            List<Position> startingPoints = new ArrayList<>();
            Map<Integer, Position> checkPoints = new HashMap<>();

            for (int x = 0; x < gameMap.size(); x++) {
                List<List<GameStartedMessage.Field>> row = gameMap.get(x);

                for (int y = 0; y < row.size(); y++) {
                    List<GameStartedMessage.Field> cellFields = row.get(y);

                    if (cellFields == null) continue;

                    // Check all fields in this cell
                    for (GameStartedMessage.Field field : cellFields) {

                        if (field != null && "StartPoint".equalsIgnoreCase(field.getType())) {
                            startingPoints.add(new Position(x, y));
                        }
                        if (field != null && "CheckPoint".equalsIgnoreCase(field.getType())) {
                            int cpNumber = field.getCount();
                            checkPoints.put(cpNumber, new Position(x, y));
                        }
                    }
                }
            }
            logger.info(startingPoints.toString());
            aiClient.setAvailableStartingPoints(startingPoints);
            aiClient.getPathfinder().setCheckpoints(checkPoints);
        });
    }
}