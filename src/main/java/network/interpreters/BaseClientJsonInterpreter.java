package network.interpreters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import helpers.RobotModel;
import javafx.application.Platform;
import model.game.Player;
import model.game.board.Direction;
import model.game.board.Position;
import model.server_client.BaseClient;
import network.JsonHandler;
import network.messages.actions8.*;
import network.messages.cards6.CardPlayedMessage;
import network.messages.chat4.ReceivedChatMessage;
import network.messages.connection2.AliveMessage;
import network.messages.connection2.HelloServerMessage;
import network.messages.connection2.WelcomeMessage;
import network.messages.lobby3.*;
import network.messages.phases7.ActivePhaseMessage;
import network.messages.phases7.CurrentPlayerMessage;
import network.messages.phases7.activation.CurrentCardsMessage;
import network.messages.phases7.activation.ReplaceCardMessage;
import network.messages.phases7.programming.*;
import network.messages.phases7.setup.StartingPointTakenMessage;
import network.messages.phases7.upgrade.ExchangeShopMessage;
import network.messages.phases7.upgrade.RefillShopMessage;
import network.messages.phases7.upgrade.UpgradeBoughtMessage;
import network.messages.specialMessage5.ConnectionUpdateMessage;
import helpers.SoundFX;
import viewmodel.MapParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;


/**
 * An abstract base client-side JSON interpreter responsible for handling incoming
 * JSON messages from the server. Specific client implementations can extend
 * this class and modify or add handlers as needed.
 */
public abstract class BaseClientJsonInterpreter implements JsonInterpreter {
    protected BaseClient client;
    protected Logger logger;
    protected Map<String, Consumer<JsonObject>> handlerMap = new HashMap<>();

    /**
     * Constructs a BaseClientJsonInterpreter with the specified client and logger.
     * Initializes the default set of handlers by calling {@link #initializeHandlers()}.
     *
     * @param client the {@link BaseClient} instance that will send and receive messages
     * @param logger a {@link Logger} for logging
     */
    public BaseClientJsonInterpreter(BaseClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
        initializeHandlers();
    }

    /**
     * Interprets an incoming JSON message by looking up its messageType in the {@link #handlerMap}.
     * If a matching handler is found, it is executed. Otherwise, logs a warning about
     * an unknown message type.
     *
     * @param jsonMessage the raw JSON string received from the server
     */
    @Override
    public synchronized void interpretMessage(String jsonMessage) {
        try {
            logger.info("Received message: " + jsonMessage);
            JsonObject jsonObj = JsonParser.parseString(jsonMessage).getAsJsonObject();
            String messageType = jsonObj.get("messageType").getAsString();
            Consumer<JsonObject> handler = handlerMap.get(messageType);
            if (handler != null) {
                handler.accept(jsonObj);
            } else {
                logger.warning("Unknown message type: " + messageType);
            }
        } catch (JsonSyntaxException exception) {
            logger.severe("JSON Error: " + exception.getMessage());
        }
    }


    /**
     * Registers the common handlers.
     * Subclasses may override or add additional handlers as needed.
     */
    protected void initializeHandlers() {
        handlerMap.put("HelloClient", _ -> {
            HelloServerMessage helloServerMessage = new HelloServerMessage(
                    client.getGroup(), client.isAI(), client.getProtocolVersion(), client.getClientID());
            client.sendMessageToServer(JsonHandler.toJson(helloServerMessage));
            if(!client.isAI()){
                SoundFX.playBackgroundMusic("theme.wav");
            }
        });

        handlerMap.put("Welcome", jsonObj -> {
            WelcomeMessage welcomeMessage = JsonHandler.fromJson(jsonObj.toString(), WelcomeMessage.class);
            client.setClientID(welcomeMessage.getMessageBody().getClientID());
        });

        handlerMap.put("Alive", _ -> {
            AliveMessage aliveMessage = new AliveMessage();
            client.sendMessageToServer(JsonHandler.toJson(aliveMessage));
        });

        handlerMap.put("ReceivedChat", jsonObj -> {
            ReceivedChatMessage rcm = JsonHandler.fromJson(jsonObj.toString(), ReceivedChatMessage.class);
            String messageContent = rcm.getMessageBody().getMessage();
            boolean isPrivate = rcm.getMessageBody().getIsPrivate();
            client.getChatDataBridge().addMessage(messageContent, isPrivate);
        });

        handlerMap.put("PlayerAdded", jsonObj -> {
            PlayerAddedMessage pam = JsonHandler.fromJson(jsonObj.toString(), PlayerAddedMessage.class);
            int addedClientId = pam.getMessageBody().getClientID();
            String addedName = pam.getMessageBody().getName();
            int figureNumber = pam.getMessageBody().getFigure();
            Platform.runLater(() -> {
                client.getLobbyDataBridge().addOrUpdatePlayer(addedClientId, addedName, figureNumber, false);
                client.getLobbyDataBridge().addClientIDToFigure(addedClientId, figureNumber);
            });
            if (addedClientId != client.getClientID()) {
                Player player = new Player(addedClientId, addedName, null);
                client.getGameDataBridge().addPlayer(player.getName(), player.getPlayerId());
            }
            if (figureNumber != -1) {
                client.getGameDataBridge().addClientIDToFigure(addedClientId, figureNumber);
                client.getLobbyDataBridge().addClientIDToFigure(addedClientId, figureNumber);
            }
            if (!client.getGameDataBridge().getIdToPlayerNameMap().containsKey(addedClientId)) {
                client.getGameDataBridge().getIdToPlayerNameMap().put(addedClientId, addedName);
            }
            if(addedClientId == client.getClientID()){
                client.getLobbyDataBridge().setValidChoice(true);
            }

            Platform.runLater(() -> {
            if(addedClientId != client.getClientID()){
                client.getChatDataBridge().addUserToList(addedClientId, addedName);
                }
            });
        });

        handlerMap.put("PlayerStatus", jsonObj -> {
            PlayerStatusMessage psm = JsonHandler.fromJson(jsonObj.toString(), PlayerStatusMessage.class);
            int setterId = psm.getMessageBody().getClientID();
            boolean status = psm.getMessageBody().isReady();
            Platform.runLater(() -> {
                client.getLobbyDataBridge().setPlayerReady(setterId, status);
                if (!status) {
                    client.getLobbyDataBridge().addClientIDToMapSelection(setterId, false);
                }
            });
        });



        handlerMap.put("ActivePhase", jsonObj -> {
            ActivePhaseMessage apm = JsonHandler.fromJson(jsonObj.toString(), ActivePhaseMessage.class);
            int phase = apm.getMessageBody().getPhase();
            client.getGameDataBridge().setPhase(phase);
            logger.info("Current Phase: " + phase);
            Platform.runLater(() -> client.getGameDataBridge().setTimerValue(-1));
            if(phase == 1){
                client.getGameDataBridge().setActiveRegisterSlot(-1);
            }
            if (phase == 2) {
                client.getGameDataBridge().setDragDisabled(false);
                client.getGameDataBridge().resetRebootedClientIDs();

            }
            if (phase == 3 && !client.isAI()) {
                SoundFX.playBackgroundMusic("activation.wav");
                //client.getGameDataBridge().setActiveRegisterSlot(0);
            } else {
                SoundFX.stopBackgroundMusic();
            }

        });

        handlerMap.put("CardPlayed", jsonObj -> {
            CardPlayedMessage pcm = JsonHandler.fromJson(jsonObj.toString(), CardPlayedMessage.class);
            int playerID = pcm.getMessageBody().getClientID();
            String card = pcm.getMessageBody().getCard();
            client.getGameDataBridge().addPlayedCard(playerID, card);
            if ("MemorySwap".equals(card) || "SpamBlocker".equals(card)) {
                client.getGameDataBridge().removeUpgradeCardForClient(playerID, card);
            }
        });


        handlerMap.put("ConnectionUpdate", jsonObj -> {
            ConnectionUpdateMessage cum = JsonHandler.fromJson(jsonObj.toString(), ConnectionUpdateMessage.class);
            int removedID = cum.getMessageBody().getClientID();
            client.getGameDataBridge().setErrorMessage("LEFT THE GAME", removedID);
            client.getGameDataBridge().removeClientIDToRoboModelToRobotModel(removedID);
            client.getGameDataBridge().addDeadRobot(removedID);
        });


        handlerMap.put("CurrentPlayer", jsonObj -> {
            CurrentPlayerMessage cpm = JsonHandler.fromJson(jsonObj.toString(), CurrentPlayerMessage.class);
            int currentPlayerId = cpm.getMessageBody().getClientID();
            client.getGameDataBridge().setCurrentPlayerID(-1);
            client.getGameDataBridge().setCurrentPlayerID(currentPlayerId);

            if (client.getGameDataBridge().getPhase().getValue() == 3 && (currentPlayerId == client.getClientID())) {
                for (CurrentCardsMessage.ActiveCard activeCard : client.getGameDataBridge().getCurrentCards()) {
                    if (activeCard.getClientID() == currentPlayerId) {
                        String currentCard = activeCard.getCard();
                        logger.info("Sending play card message: " + currentCard);
                        client.sendPlayCard(currentCard);
                    }
                }
            }
        });


        handlerMap.put("StartingPointTaken", jsonObj -> {
            StartingPointTakenMessage sptm = JsonHandler.fromJson(jsonObj.toString(), StartingPointTakenMessage.class);
            int clientID = sptm.getMessageBody().getClientID();
            int takenX = sptm.getMessageBody().getX();
            int takenY = sptm.getMessageBody().getY();
            Direction dir = Direction.fromString(sptm.getMessageBody().getDirection());
            client.getGameDataBridge().updateRobotPosition(clientID, takenX, takenY);
            while (dir != (client.getGameDataBridge().getClientIDToRoboModelToRobotModel().get(clientID).getDirection())) {
                client.getGameDataBridge().rotateRobotModel(clientID, "clockwise");
                logger.info(client.getGameDataBridge().getClientIDToRoboModelToRobotModel().get(clientID).getDirection().toString());
            }
            Platform.runLater(() -> MapParser.removeStartingPoint(takenX, takenY, client, clientID));
        });


        handlerMap.put("YourCards", jsonObj -> {


            YourCardsMessage ycm = JsonHandler.fromJson(jsonObj.toString(), YourCardsMessage.class);
            List<String> cards = ycm.getMessageBody().getCardsInHand();
            List<String> cardsOld = client.getGameDataBridge().getCardsInHand();
            List<String> newCards = new ArrayList<>();



            if(cards.size() < 9){
                for(String card : cardsOld){
                   if(!("Spam".equals(card))){
                       newCards.add(card);
                   }
                }
                newCards.addAll(cards);
                client.getGameDataBridge().setCardsInHand(newCards);
            }else{
                client.getGameDataBridge().setCardsInHand(ycm.getMessageBody().getCardsInHand());
            }
            Platform.runLater(() -> client.getGameDataBridge().setDoneButtonDisabled(false));
        });



        handlerMap.put("CurrentCards", jsonObj -> {
            CurrentCardsMessage ccm = JsonHandler.fromJson(jsonObj.toString(), CurrentCardsMessage.class);
            List<CurrentCardsMessage.ActiveCard> activeCards = ccm.getMessageBody().getActiveCards();
            client.getGameDataBridge().setCurrentCards(activeCards);
            int slot = client.getGameDataBridge().getActiveRegisterSlot();
            slot += 1;

            client.getGameDataBridge().setActiveRegisterSlot(slot);
        });

        handlerMap.put("ReplaceCard", jsonObj -> {
            ReplaceCardMessage replaceCaMe = JsonHandler.fromJson(jsonObj.toString(), ReplaceCardMessage.class);
            int registerIndex = replaceCaMe.getMessageBody().getRegister();
            String newCard = replaceCaMe.getMessageBody().getNewCard();
            int playerId = replaceCaMe.getMessageBody().getClientID();
            logger.warning("got the message & updating game data bridge");
            client.getGameDataBridge().updateReplacedCard(playerId, registerIndex, newCard);
        });


        handlerMap.put("RefillShop", jsonObj -> {
            RefillShopMessage rsm = JsonHandler.fromJson(jsonObj.toString(), RefillShopMessage.class);
            List<String> upgradeCardsShop = rsm.getMessageBody().getCards();
            Platform.runLater(() -> client.getGameDataBridge().addAvailableUpgradeCard(upgradeCardsShop));
        });

        handlerMap.put("ExchangeShop", jsonObj -> {
            ExchangeShopMessage eam = JsonHandler.fromJson(jsonObj.toString(), ExchangeShopMessage.class);
            List<String> upgradeCardsExchange = eam.getMessageBody().getCards();
            Platform.runLater(() -> client.getGameDataBridge().setAvailableUpgradeCards(upgradeCardsExchange));
        });

        handlerMap.put("UpgradeBought", jsonObj -> {
            UpgradeBoughtMessage ubm = JsonHandler.fromJson(jsonObj.toString(), UpgradeBoughtMessage.class);
            int clientIDUpgrade = ubm.getMessageBody().getClientID();
            String upgradeCardBought = ubm.getMessageBody().getCard();
            client.getGameDataBridge().addUpgradeCardForClient(clientIDUpgrade, upgradeCardBought);
            client.getGameDataBridge().removeAvailableUpgradeCard(upgradeCardBought);
            if ("RearLaser".equals(upgradeCardBought)) {
                client.getGameDataBridge().addActivatedUpgrade(clientIDUpgrade, upgradeCardBought);
            }
            if(clientIDUpgrade == client.getClientID() && !client.isAI()){
                SoundFX.playSoundEffect("shop.wav");
            }
        });

        handlerMap.put("RegisterChosen", _ -> {
        });

        handlerMap.put("Movement", jsonObj -> {
            MovementMessage mm = JsonHandler.fromJson(jsonObj.toString(), MovementMessage.class);
            int x = mm.getMessageBody().getX();
            int y = mm.getMessageBody().getY();
            int clientIDMovement = mm.getMessageBody().getClientID();
            Platform.runLater(() -> client.getGameDataBridge().updateRobotPosition(clientIDMovement, x, y));
        });


        handlerMap.put("PlayerTurning", jsonObj -> {
            PlayerTurningMessage ptm = JsonHandler.fromJson(jsonObj.toString(), PlayerTurningMessage.class);
            int clientIDTurning = ptm.getMessageBody().getClientID();
            String dirTurning = ptm.getMessageBody().getRotation();
            logger.info("direction of turning: " + dirTurning);
            Platform.runLater(() -> client.getGameDataBridge().rotateRobotModel(clientIDTurning, dirTurning));
        });


        handlerMap.put("Reboot", jsonObj -> {
            RebootMessage rm = JsonHandler.fromJson(jsonObj.toString(), RebootMessage.class);
            int clientIDReboot = rm.getMessageBody().getClientID();
            client.getGameDataBridge().addRebootedClientID(clientIDReboot);
            if(!client.isAI()) {
                SoundFX.playSoundEffect("scream1.wav");
            }
        });


        handlerMap.put("Energy", jsonObj -> {
            EnergyMessage em = JsonHandler.fromJson(jsonObj.toString(), EnergyMessage.class);
            int clientIDForEnergy = em.getMessageBody().getClientID();
            int energy = em.getMessageBody().getCount();
            client.getGameDataBridge().setEnergy(clientIDForEnergy, energy);
            String source = em.getMessageBody().getSource();

            if (source.equals("EnergySpace")) {
                RobotModel robotModel =
                        client.getGameDataBridge().getClientIDToRoboModelToRobotModel().get(clientIDForEnergy);
                if (robotModel != null) {
                    int energyX = robotModel.getX();
                    int energyY = robotModel.getY();
                    Platform.runLater(() -> MapParser.updateEnergySpace(
                            client.getGameDataBridge().getGameMapGridPane(), energyX, energyY));
                    if (clientIDForEnergy == client.getClientID() && !client.isAI()) {
                        SoundFX.playSoundEffect("charging.wav");
                    }
                } else if (clientIDForEnergy == client.getClientID() && !client.isAI()) {
                    SoundFX.playSoundEffect("charging.wav");
                } else {
                    logger.warning("RobotModel not found for" + clientIDForEnergy);
                }
            }
        });


        handlerMap.put("CheckPointReached", jsonObj -> {
            CheckPointReachedMessage cprm = JsonHandler.fromJson(jsonObj.toString(), CheckPointReachedMessage.class);
            client.getGameDataBridge().updateCurrentCheckpoint(cprm.getMessageBody().getClientID(),
                    cprm.getMessageBody().getNumber());
            if(cprm.getMessageBody().getClientID()==client.getClientID() && !client.isAI()) {
                SoundFX.playSoundEffect("checkpoint.wav");
            }
        });


        handlerMap.put("DrawDamage", jsonObj -> {
            DrawDamageMessage ddm = JsonHandler.fromJson(jsonObj.toString(), DrawDamageMessage.class);
            int damagedPlayer = ddm.getMessageBody().getClientID();
            List<String> cards = ddm.getMessageBody().getCards();
            client.getGameDataBridge().addDamageCard(damagedPlayer, cards);
            if(damagedPlayer==client.getClientID() && !client.isAI()) {
                SoundFX.playSoundEffect("damage.wav");
            }
        });


        handlerMap.put("PickDamage", jsonObj -> {
            PickDamageMessage pdm = JsonHandler.fromJson(jsonObj.toString(), PickDamageMessage.class);
            List<String> availablePiles = pdm.getMessageBody().getAvailablePiles();
            int count = pdm.getMessageBody().getCount();

            Platform.runLater(() -> {
                client.getGameDataBridge().setPickDamageCount(count);
                client.getGameDataBridge().setAvailableDamageCards(availablePiles);
                client.getGameDataBridge().setPickDamageTriggered(true);
            });
        });


        handlerMap.put("CheckpointMoved", jsonObj -> {
            CheckpointMovedMessage cmm = JsonHandler.fromJson(jsonObj.toString(), CheckpointMovedMessage.class);
            int checkpointID = cmm.getMessageBody().getCheckpointID();
            int checkpointX = cmm.getMessageBody().getX();
            int checkpointY = cmm.getMessageBody().getY();
            Platform.runLater(() -> {
                MapParser.moveCheckpoints(client.getGameDataBridge().getGameMapGridPane(),client.getGameDataBridge().getGameMap(),checkpointID,checkpointX,checkpointY);
            });
        });


        handlerMap.put("GameStarted", jsonObj -> {
            GameStartedMessage gsm = JsonHandler.fromJson(jsonObj.toString(), GameStartedMessage.class);
            List<List<List<GameStartedMessage.Field>>> gameMap = gsm.getMessageBody().getGameMap();
            client.getGameDataBridge().addCurrentCheckpointToMap(client.getClientID(), 0);
            MapParser.updateWallLaserMap(gameMap, client.getGameDataBridge());
            logger.info("GameStarted: " + gameMap);

            // Extract starting points (using Position)
            List<Position> startingPoints = new ArrayList<>();
            for (int a = 0; a < gameMap.size(); a++) {
                List<List<GameStartedMessage.Field>> row = gameMap.get(a);
                for (int b = 0; b < row.size(); b++) {
                    List<GameStartedMessage.Field> cellFields = row.get(b);
                    if (cellFields == null) continue;
                    for (GameStartedMessage.Field field : cellFields) {
                        if (field != null && "StartPoint".equalsIgnoreCase(field.getType())) {
                            startingPoints.add(new Position(a, b));
                        }
                    }
                }
            }
            Platform.runLater(() -> {
                client.getGameDataBridge().setAvailableStartingPoints(startingPoints);
                client.getLobbyDataBridge().setStart(true);
            });
            client.getGameDataBridge().setGameMap(gameMap);
            logger.info("Game map loaded: " + gameMap);
        });


        handlerMap.put("GameFinished", jsonObj -> {
            GameFinishedMessage gfm = JsonHandler.fromJson(jsonObj.toString(), GameFinishedMessage.class);
            int winnerId = gfm.getMessageBody().getClientID();

            Platform.runLater(() -> {
                client.getGameDataBridge().setWinnerID(winnerId);
                client.getGameDataBridge().setGameOver(true);
            });
        });
    }

    protected abstract void modifyHandlers();
}