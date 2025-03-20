package network.interpreters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import model.game.AI.AIClient;
import model.game.Game;
import model.game.GameState;
import model.game.Player;
import model.game.board.Direction;
import model.game.board.robots.Robot;
import model.game.cards.Card;
import model.game.cards.Deck;
import model.game.cards.UpgradeCardType;
import model.server_client.Server;
import network.JsonHandler;
import network.messages.actions8.*;
import network.messages.cards6.PlayCardMessage;
import network.messages.chat4.ReceivedChatMessage;
import network.messages.chat4.SendChatMessage;
import network.messages.connection2.HelloServerMessage;
import network.messages.connection2.WelcomeMessage;
import network.messages.lobby3.*;
import network.messages.phases7.programming.NotYourCardsMessage;
import network.messages.phases7.programming.SelectedCardMessage;
import network.messages.phases7.programming.YourCardsMessage;
import network.messages.phases7.setup.SetStartingPointMessage;
import model.server_client.ConnectionHandler;
import network.messages.phases7.upgrade.BuyUpgradeMessage;
import network.messages.specialMessage5.ConnectionUpdateMessage;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Interprets JSON messages received from clients and delegates the handling of these messages
 * to appropriate methods based on the message type.
 */
public class ServerJsonInterpreter implements JsonInterpreter {
    private final ConnectionHandler connectionHandler;
    private final Server server;
    private final Logger logger;
    private final Game game = Game.getInstance();

    /**
     * Constructs a new ServerJsonInterpreter.
     *
     * @param connectionHandler The connection handler associated with the client.
     * @param server            The server instance.
     * @param logger            The logger for recording events.
     */
    public ServerJsonInterpreter(ConnectionHandler connectionHandler, Server server, Logger logger) {
        this.connectionHandler = connectionHandler;
        this.server = server;
        this.logger = logger;
    }

    /**
     * Interprets the incoming JSON message and delegates the handling to the appropriate method
     * based on the message type.
     *
     * @param jsonMessage The JSON message received from the client.
     */
    @Override
    public synchronized void interpretMessage(String jsonMessage) {
        try {
            logger.info("Received JSON: " + jsonMessage);

            JsonObject jsonObj = JsonParser.parseString(jsonMessage).getAsJsonObject();
            String messageType = jsonObj.get("messageType").getAsString();

            Server server = connectionHandler.getServer();

            switch (messageType) {
                case "HelloServer":
                    HelloServerMessage hsm = JsonHandler.fromJson(jsonMessage, HelloServerMessage.class);
                    handleHelloServer(hsm, connectionHandler, server);
                    break;

                case "Alive":
                    handleAliveMessage(connectionHandler);
                    break;

                case "SendChat":
                    SendChatMessage scm = JsonHandler.fromJson(jsonMessage, SendChatMessage.class);
                    handleChatMessage(scm, connectionHandler, server);
                    break;

                case "PlayerValues":
                    PlayerValuesMessage pvm = JsonHandler.fromJson(jsonMessage, PlayerValuesMessage.class);
                    handlePlayerValues(pvm, connectionHandler, server);
                    break;

                case "SetStatus":
                    SetStatusMessage ssm = JsonHandler.fromJson(jsonMessage, SetStatusMessage.class);
                    handleSetStatusMessage(ssm, connectionHandler, server);
                    break;

                case "MapSelected":
                    MapSelectedMessage mm = JsonHandler.fromJson(jsonMessage, MapSelectedMessage.class);
                    handleMapSelected(mm, connectionHandler, server);
                    break;

                case "PlayCard":
                    PlayCardMessage pcm = JsonHandler.fromJson(jsonMessage, PlayCardMessage.class);
                    handlePlayCard(pcm, connectionHandler, server);
                    break;

                case "BuyUpgrade":
                    //every time when button "Buy Upgrade" or "Dont Buy Upgrade" gets clicked
                    BuyUpgradeMessage bum = JsonHandler.fromJson(jsonMessage, BuyUpgradeMessage.class);
                    handleBuyUpgrade(bum, connectionHandler);
                    break;

                case "ConnectionUpdate":
                    ConnectionUpdateMessage cum = JsonHandler.fromJson(jsonMessage, ConnectionUpdateMessage.class);
                    handleConnectionUpdate(cum, server);
                    break;
                case "SetStartingPoint":
                    SetStartingPointMessage sspm = JsonHandler.fromJson(jsonMessage, SetStartingPointMessage.class);
                    handleSetStartingPoint(sspm, connectionHandler, server);
                    break;
                case "SelectedCard":
                    SelectedCardMessage selectedCaMe = JsonHandler.fromJson(jsonMessage, SelectedCardMessage.class);
                    handleSelectedCard(selectedCaMe, connectionHandler, server);
                    break;
                case "SelectedDamage":
                    SelectedDamageMessage sdm = JsonHandler.fromJson(jsonMessage, SelectedDamageMessage.class);
                    handleSelectedDamage(sdm, connectionHandler, server);
                    break;
                case "RebootDirection":
                    RebootDirectionMessage rdm = JsonHandler.fromJson(jsonMessage, RebootDirectionMessage.class);
                    handleRebootDirection(rdm, connectionHandler, server);
                    break;
                case "DiscardSome":
                    DiscardSomeMessage dsm = JsonHandler.fromJson(jsonMessage, DiscardSomeMessage.class);
                    handleDiscardSome(dsm, connectionHandler,server);
                    break;
                case "ChooseRegister":
                    ChooseRegisterMessage crm = JsonHandler.fromJson(jsonMessage, ChooseRegisterMessage.class);
                    handleChooseRegister(crm,connectionHandler,server);
                    break;
                default:
                    logger.warning("Unknown message type: " + messageType);
            }

        } catch (JsonSyntaxException exception) {
            logger.severe("Invalid JSON message received: " + jsonMessage);
            logger.severe("JSON error: " + exception.getMessage());
        }
    }

    private void handleHelloServer(HelloServerMessage helloServerMessage,
                                          ConnectionHandler handler, Server server) {

        if (!server.getProtocolVersion().equals(helloServerMessage.getMessageBody().getProtocol())) {
            logger.info("Client doesn't have the needed protocol version. Access denied!");
            handler.shutdownClient();
        } else {
            WelcomeMessage message = new WelcomeMessage(handler.getClientId());
            String json = JsonHandler.toJson(message);
            logger.info("Client has the needed protocol version. Access granted.");
            handler.sendMessage(json);
            server.sendPlayersInfo(handler);
        }
    }

    private static void handleAliveMessage(ConnectionHandler handler) {
        handler.setLastAliveTime(System.currentTimeMillis());
    }

    private static void handleChatMessage(SendChatMessage scm, ConnectionHandler handler, Server server) {
        String chatText = scm.getMessageBody().getMessage();
        int recipientID = scm.getMessageBody().getTo();
        boolean isPrivate = (recipientID != (-1));
        ReceivedChatMessage message = new ReceivedChatMessage(chatText, handler.getClientId(), isPrivate);
        String chatMessageJson = JsonHandler.toJson(message);

        if (isPrivate) {
            ConnectionHandler target = server.getConnectionHandlerById(recipientID);
            if (target != null) {
                target.sendMessage(chatMessageJson);
            }
        }else {
            server.broadcastToAllExceptSelf(chatMessageJson, handler);
        }
    }

    private void handlePlayerValues(PlayerValuesMessage pvm, ConnectionHandler handler, Server server) {
        String name = pvm.getMessageBody().getName();

        handler.setNickname(name);
        logger.info("set nickname " + name);
        int figureID = pvm.getMessageBody().getFigure();
        server.connectionsMap.put(name, handler);
        if(server.isGameRunning){
            //send error message
            server.sendError("A game for group EdleEisbecher is already running", handler);
            return;
        }


        Player player = Game.getInstance().getPlayerById(handler.getClientId());
        if (player == null) {
            player = new Player(handler.getClientId(), name, null);
            Game.getInstance().addPlayer(player);
        }

        if (player.getRobot() != null) {
            int previousFigureId = player.getRobot().getId();
            Game.getInstance().makeFigureAvailable(previousFigureId);
        }

        boolean isFigureAvailable = game.isFigureAvailable(figureID);
        if (isFigureAvailable) {
            Game.getInstance().assignFigure(player, figureID);
            PlayerAddedMessage pam = new PlayerAddedMessage(handler.getClientId(), name, figureID);
            server.broadcastToAll(JsonHandler.toJson(pam));
            server.sendPlayersInfo(handler);
        } else {
            server.sendError("Figure " + figureID + " is already taken.", handler);
        }
    }

    private void handleSetStatusMessage(SetStatusMessage ssm, ConnectionHandler handler, Server server){
        boolean isReady = ssm.getMessageBody().isReady();
        Player player = Game.getInstance().getPlayerById(handler.getClientId());
        player.setPlayerStatus(isReady);

        //send PlayerStatus message to all players
        PlayerStatusMessage psm = new PlayerStatusMessage(handler.getClientId(), isReady);
        String psmJson = JsonHandler.toJson(psm);
        server.broadcastToAll(psmJson);

        server.updateReadyPlayers(handler, isReady);

        if (server.getReadyPlayers().size() == game.getPlayers().size() &&
                server.getReadyPlayers().size() >= game.getMinPlayers() && server.getSelectedMap() != null) {
            logger.info("All players are ready! Starting the game.");
            game.startGame();
        }

    }

    /**
     * Handles the "Play Card" action received from a client.
     * <p>
     * This method handles the current player's card-playing action in the game, applies the card's effects, and
     * broadcasts a CardPlayedMessage to notify all connected clients of the move.
     * </p>
     *
     * @param pcm     The {@link PlayCardMessage} containing details of the played card.
     * @param handler The {@link ConnectionHandler} representing the client that initiated the action.
     * @param server  The {@link Server} instance used to broadcast messages to other clients.
     */
    private void handlePlayCard(PlayCardMessage pcm, ConnectionHandler handler, Server server) {
        Player player = game.getCurrentPlayer();
        String card = pcm.getMessageBody().getCard();
        if(player.getRobot() != null) {
            if (card.equals("SpamBlocker") || card.equals("MemorySwap")) {
                game.playCard(game.getPlayerById(handler.getClientId()), card);
            } else {
                game.playCard(player, card);
            }
        }
    }

    private void handleMapSelected(MapSelectedMessage mm, ConnectionHandler handler, Server server) {
        int totalPlayers = Game.getInstance().getPlayers().size();
        int readyCount   = server.getReadyPlayers().size();
        int minPlayers   = 2;

        if (readyCount != totalPlayers) {
            server.sendError("Cannot select map yet. All players are not ready.", handler);
            return;

        } else if (totalPlayers < minPlayers) {
            server.sendError("Cannot select map yet. There are not enough players.", handler);
            return;
        }


        String selectedMap = mm.getMessageBody().getMap();
        selectedMap = selectedMap.trim();
        server.setSelectedMap(selectedMap);

        MapSelectedMessage msm = new MapSelectedMessage(selectedMap);
        String mmJson = JsonHandler.toJson(msm);
        server.broadcastToAll(mmJson);

        if (readyCount == totalPlayers && totalPlayers >= minPlayers) {
            game.startGame();
        }
    }
    private void handleBuyUpgrade(BuyUpgradeMessage bum, ConnectionHandler handler) {
        String upgradeCardName = bum.getMessageBody().getCard();
        Player player = game.getPlayerById(handler.getClientId());
        boolean isBuying = bum.getMessageBody().isBuying();
        game.buyUpgrade(player, upgradeCardName, isBuying);

    }
    /**
     * Handles the "Set Starting Point" action received from a client.
     * <p>
     * This method processes the starting point coordinates selected by the player during setup.
     * </p>
     *
     * @param sspm    The SetStartingPointMessage containing the X and Y coordinates.
     * @param handler The ConnectionHandler representing the client that sent the message.
     * @param server  The Server instance.
     */
    public void handleSetStartingPoint(SetStartingPointMessage sspm, ConnectionHandler handler, Server server){
        int x = sspm.getMessageBody().getX();
        int y = sspm.getMessageBody().getY();
        int[] position = {x, y};
        game.takeTurnSetUp(game.getPlayerById(handler.getClientId()), position);
    }
    /**
     * Handles the "Selected Card" action received from a client during the programming phase.
     *
     * @param sm      The SelectedCardMessage containing the chosen card and the register position.
     * @param handler The ConnectionHandler of the player.
     * @param server  The Server instance.
     */
    public void handleSelectedCard(SelectedCardMessage sm, ConnectionHandler handler, Server server) {
        int register = sm.getMessageBody().getRegister();
        String cardName = sm.getMessageBody().getCard();
        Player player = game.getPlayerById(handler.getClientId());
        game.takeTurnProgramming(player, cardName, register);
    }
    /**
     * Handles the "Reboot Direction" action received from a client.
     * <p>
     * This method rotates the player's robot to the specified direction.
     * </p>
     *
     * @param rm      The RebootDirectionMessage containing the desired direction.
     * @param handler The ConnectionHandler of the player.
     * @param server  The Server instance.
     */
    public void handleRebootDirection(RebootDirectionMessage rm, ConnectionHandler handler, Server server) {
        Player player = game.getPlayerById(handler.getClientId());
        Robot robot = player.getRobot();
        String directionString = rm.getMessageBody().getDirection();
        Direction directionToRotate = Direction.TOP;

        if (directionString != null) {
            if (directionString.equalsIgnoreCase("top")) {
                directionToRotate = Direction.TOP;
            } else if (directionString.equalsIgnoreCase("bottom")) {
                directionToRotate = Direction.BOTTOM;
            } else if (directionString.equalsIgnoreCase("left")) {
                directionToRotate = Direction.LEFT;
            } else if (directionString.equalsIgnoreCase("right")) {
                directionToRotate = Direction.RIGHT;
            }
            while (robot.getDirection() != directionToRotate) {
                robot.rotateRobot("clockwise");
                logger.info("[TEST] Robot turns for reboot: " + robot.getDirection());
            }
        } else {
            logger.warning("Rebooting direction is null.");
            while(robot.getDirection()!=Direction.TOP){
                robot.rotateRobot("clockwise");
            }
        }
    }

    /**
     * Handles the "Selected Damage" action received from a client.
     * <p>
     * This method processes the damage cards selected by the player.
     * </p>
     *
     * @param sdm     The SelectedDamageMessage containing the list of damage cards chosen.
     * @param handler The ConnectionHandler representing the client.
     * @param server  The Server instance.
     */
    public void handleSelectedDamage(SelectedDamageMessage sdm, ConnectionHandler handler, Server server) {
        Player player = game.getPlayerById(handler.getClientId());
        List<String> cards = sdm.getMessageBody().getCards();
        game.setSelectedDamage(player, cards);
    }
    /**
     * Handles the "Connection Update" action received from a client.
     * <p>
     * This method processes connection updates such as client disconnections or changes in AI control.
     * </p>
     *
     * @param cum    The ConnectionUpdateMessage containing the connection update details.
     * @param server The Server instance.
     */
    public void handleConnectionUpdate(ConnectionUpdateMessage cum, Server server) {
        boolean isConnected = cum.getMessageBody().isConnected();
        if (!isConnected) {
            int clientID = cum.getMessageBody().getClientID();
            String action = cum.getMessageBody().getAction();
            if (action.equalsIgnoreCase("remove")) {
                Player player = game.getPlayerById(clientID);
                game.removePlayer(player);
                server.removeClientName(clientID);
            }
            if(action.equalsIgnoreCase("AIControl")){
                AIClient smartAI = new AIClient(2);
                smartAI.run();
            }
            String json = JsonHandler.toJson(cum);
            server.broadcastToAll(json);
        }
    }


    private void handleChooseRegister(ChooseRegisterMessage crm, ConnectionHandler handler, Server server) {
        int register = crm.getMessageBody().getRegister();
        int clientID = handler.getClientId();
        if(game.getCurrentRegisterIndex() != 5 && register<=game.getCurrentRegisterIndex() && !(game.getCurrentRegisterIndex() == 0 && game.getGameState() != GameState.ACTIVATION_PHASE)){
            server.sendError("Priority in register " + register + " has already been processed.", handler);
            return;
        }
        Player player = game.getPlayerById(clientID);
        // Get the map of players who have already used AdminPrivilege
        Map<Integer, Integer> adminPrivilegePlayers = game.getPlayerToAdminPrivilege();
        // Check if player has already activated AdminPrivilege for this specific register
        if (adminPrivilegePlayers.containsKey(clientID) && adminPrivilegePlayers.get(clientID) == register) {
            server.sendError("You have already activated AdminPrivilege for this register.", handler);
            return;
        }
        int adminPrivilegeCount = 0;
        for (UpgradeCardType card : player.getUpgradeCards()) {
            if (card == UpgradeCardType.ADMIN_PRIVILEGE) {
                adminPrivilegeCount++;
            }
        }
        // Check if the player has already used all available AdminPrivilege cards
        if(player.getAdminPrivilegeUsed()>= adminPrivilegeCount){
            server.sendError("Upgrade card has already been used.", handler);
            return;
        }
        // Put the player and the register in a map
        player.setAdminPrivilegeUsed(player.getAdminPrivilegeUsed() + 1);
        game.addPlayerToAdminPrivilege(clientID, register);
        RegisterChosenMessage rcm = new RegisterChosenMessage(clientID, register);
        String rcmJson = JsonHandler.toJson(rcm);
        server.broadcastToAll(rcmJson);
    }

    /**
     * Handles the "Discard Some" action received from a client.
     * <p>
     * This method processes the cards that the player has chosen to discard from their hand.
     * It updates both the player's hand and their discard deck, then notifies all relevant clients.
     * </p>
     *
     * @param dsm     The DiscardSomeMessage containing the list of cards to discard.
     * @param handler The ConnectionHandler representing the client.
     * @param server  The Server instance.
     */
    public void handleDiscardSome(DiscardSomeMessage dsm, ConnectionHandler handler, Server server) {
        int clientID = handler.getClientId();
        List<String> cardsToDiscard = dsm.getMessageBody().getCards();
        Player player = game.getPlayerById(clientID);
        if (player == null) {
            server.sendError("Player not found.", handler);
            return;
        }
        List<Card> hand = player.getHand();
        Deck discardDeck = player.getDiscardDeck();

        ArrayList<Card> cardsToRemove = new ArrayList<>();
        // Iterate through the hand and find the matching cards
        for (String cardName : cardsToDiscard) {
            Iterator<Card> iterator = hand.iterator();
            while (iterator.hasNext()) {
                Card card = iterator.next();
                if (card.type().getName().equalsIgnoreCase(cardName)) {
                    iterator.remove(); // Entfernt nur das erste gefundene Element
                    break; // Beende die Suche nach dieser Karte
                }
            }
        }
        // Remove found cards from the player's hand and add to discard pile
        if (!cardsToRemove.isEmpty()) {
            hand.removeAll(cardsToRemove);
            // Add removed cards to discard pile
            discardDeck.addCards(cardsToRemove);
        }
        // Notify other players about updated hand size
        List<String> newHand = hand.stream()
                .map(card -> card.type().getName()) // Konvertiere CardType zu String
                .collect(Collectors.toList());

        YourCardsMessage ycm = new YourCardsMessage(newHand);
        String mmJson = JsonHandler.toJson(ycm);
        handler.sendMessage(mmJson);

        NotYourCardsMessage nycm = new NotYourCardsMessage(player.getPlayerId(), hand.size());
        String nycmJson = JsonHandler.toJson(nycm);
        server.broadcastToPlayersExceptSelf(nycmJson, handler, game.getPlayers());
    }

}