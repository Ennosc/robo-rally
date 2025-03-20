package model.game;

import model.game.board.Board;
import model.game.board.Direction;
import model.game.board.robots.Robot;
import model.game.board.tiles.StartPointTile;
import model.game.board.tiles.Tile;
import model.game.cards.*;
import model.game.maps.MapParser;
import model.server_client.ConnectionHandler;
import model.server_client.Server;
import network.messages.actions8.*;
import network.messages.cards6.CardPlayedMessage;
import network.messages.lobby3.GameStartedMessage;
import network.JsonHandler;
import network.messages.phases7.ActivePhaseMessage;
import network.messages.phases7.CurrentPlayerMessage;
import network.messages.phases7.activation.CurrentCardsMessage;
import network.messages.phases7.activation.ReplaceCardMessage;
import network.messages.phases7.programming.*;
import network.messages.phases7.setup.StartingPointTakenMessage;
import network.messages.phases7.upgrade.ExchangeShopMessage;
import network.messages.phases7.upgrade.RefillShopMessage;
import network.messages.phases7.upgrade.UpgradeBoughtMessage;
import network.messages.specialMessage5.ErrorMessage;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

import model.game.cards.PersonalProgrammingDeck;

/**
 * Main game class that manages players, the board, turns, phases, and card decks.
 * <p>
 * It handles game state changes, notifications, and interactions between game components.
 * </p>
 */
public class Game {
    private static Game instance;
    private int numberPlayers;
    private int currentRound;
    private Board board;
    private final List<Player> players;
    private Player currentPlayer;
    private String boardName;
    private ScheduledExecutorService timerService;
    private boolean isTimerRunning = false;
    private GameState gameState;
    private DamageCardsDeck spamDeck;
    private DamageCardsDeck trojanDeck;
    private DamageCardsDeck virusDeck;
    private DamageCardsDeck wormDeck;
    private UpgradeCardsDeck upgradeDeck;
    private List<Player> priorityOrder;
    private final UpgradeShop upgradeShop;
    private int checkpointsToWin;
    public Set<Integer> availableFiguresSet = new HashSet<>();//key = 0-5(figure) val = 0-1(frequency)
    private final Map<Integer, Robot> robotMap;
    private List<Player> readyPlayers;
    private Server server;
    private final List<int[]> availableStartingPoints = new ArrayList<>();
    private final int minPlayers;
    private int currentRegisterIndex;
    private int currentPlayerIndex;
    private Logger logger;


    private final HashMap<Integer, Integer> adminPrivilegePlayers = new HashMap<>();

    /**
     * Main game class that manages players, the board, turns, phases, and card decks.
     * It handles game state changes, notifications, and interactions between game components.
     */
    public Game() {
        this.timerService = Executors.newScheduledThreadPool(1);
        this.players = new ArrayList<>();
        this.priorityOrder = new ArrayList<>();
        this.numberPlayers = 0;
        this.robotMap = new HashMap<>();
        this.minPlayers = 2;
        this.upgradeShop = new UpgradeShop(new UpgradeCardsDeck());
        populateRobotMap();
        populateAvailableFiguresSet();
        initializeDeck();
    }

    /**
     * Returns the singleton instance of the game.
     *
     * @return the current Game instance.
     */
    public static Game getInstance() {
        if (instance == null) {
            instance = new Game();
        }
        return instance;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setLogger(Logger loggerFromServer) {
        this.logger = loggerFromServer;
    }

    /**
     * Initializes all card decks used in the game.
     */
    public void initializeDeck() {
        this.spamDeck = new DamageCardsDeck(DamageCardType.SPAM);
        this.trojanDeck = new DamageCardsDeck(DamageCardType.TROJAN);
        this.virusDeck = new DamageCardsDeck(DamageCardType.VIRUS);
        this.wormDeck = new DamageCardsDeck(DamageCardType.WORM);
        this.upgradeDeck = new UpgradeCardsDeck();

        spamDeck.initializeDeck();
        trojanDeck.initializeDeck();
        virusDeck.initializeDeck();
        wormDeck.initializeDeck();
        upgradeDeck.initializeDeck();
    }

    /**
     * Starts the game by building the board, setting checkpoints, and starting the setup.
     */
    public void startGame() {
        buildBoard();
        logger.info("Game: setCHeckPoints");
        setCheckpointsToWin(board);
        logger.info("Number of checkpoints to win: " + checkpointsToWin);
        gatherStartingPoints();
        startSetUpPhase();
        this.numberPlayers = players.size();
    }

    /**
     * Builds the game board from a JSON map and notifies clients that the game has started.
     */
    public void buildBoard() {
        logger.info("Building board");
        boardName = (server.getSelectedMap());
        boardName = boardName.replaceAll("\\s+", "").toLowerCase();
        // Lade die Ressource mit dem Klassenlader
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("maps/" + boardName + ".json");


        logger.info("Resource found and loaded successfully: maps/" + boardName + ".json");

        MapParser parser = new MapParser(inputStream);
        List<List<List<Tile>>> tileBoard = parser.getBoard();

        int rows = tileBoard.size();
        int cols = tileBoard.getFirst().size();
        Board board = new Board(rows, cols);

        board.initializeBoard(tileBoard);
        logger.info("Game: this.board");
        this.board = board;
        board.setLogger(logger);

        logger.info("Game: fieldMap");
        List<List<List<GameStartedMessage.Field>>> fieldMap = parser.convertTilesToFields(tileBoard);
        logger.info("Game: fieldMap printer:" + fieldMap.toString());
        logger.info("Game: notifyGameStarted");
        int startingEnergy = 5;
        notifyGameStarted(startingEnergy, fieldMap);
    }


    private void notifyGameStarted(int startingEnergy, List<List<List<GameStartedMessage.Field>>> fieldMap) {
        GameStartedMessage gsm = new GameStartedMessage(startingEnergy, fieldMap);
        String gsmJson = JsonHandler.toJson(gsm);
        server.broadcastToAll(gsmJson);
        server.isGameRunning = true;
    }


    /**
     * Adds a new player to the game.
     *
     * @param player the player to add.
     */
    public void addPlayer(Player player) {
        players.add(player);
        numberPlayers++;
    }

    /**
     * Removes a player from the game.
     *
     * @param player the player to remove.
     */
    public void removePlayer(Player player) {
        players.remove(player);
        numberPlayers--;
        if (player.getPlayerId() != -1) {
            int figureId = player.getRobot().getId();
            makeFigureAvailable(figureId);
        }
    }

    /**
     * Removes a player by their client ID.
     *
     * @param clientID the ID of the player to remove.
     */
    public void removePlayerByID(int clientID) {
        players.remove(getPlayerById(clientID));
        numberPlayers--;
    }

    /**
     * Starts the setup phase by setting the game state and initializing the first players turn.
     */
    public void startSetUpPhase() {
        setGameState(0);
        currentRound = 1;
        setCurrentPlayer(players.getFirst());
        refillShop();
    }

    /**
     * Refills the upgrade shop and broadcasts the shop message to all players.
     */
    private void refillShop() {
        String mesJson;
        if(upgradeShop.checkForShopReset(numberPlayers)){
            List<UpgradeCardType> uPCards = upgradeShop.exchangeShop(numberPlayers);
            List<String> upgradeNames = uPCards.stream()
                    .map(UpgradeCardType::getName)
                    .toList();
            ExchangeShopMessage mes = new ExchangeShopMessage(upgradeNames);
            mesJson = JsonHandler.toJson(mes);

        } else {
            List<UpgradeCardType> uPCards = upgradeShop.refillShop(numberPlayers);
            List<String> upgradeNames = uPCards.stream()
                    .map(UpgradeCardType::getName)
                    .toList();
            RefillShopMessage mes = new RefillShopMessage(upgradeNames);
            mesJson = JsonHandler.toJson(mes);
        }
        server.broadcastToPlayers(mesJson, players);

    }

    /**
     * Sets up the player's turn by selecting their robots starting position.
     *
     * @param player           the player taking their turn
     * @param startingPosition the chosen starting position for the players robot
     */
    public void takeTurnSetUp(Player player, int[] startingPosition) {
        if (gameState == GameState.SETUP) {
            if (!player.hasChosen()) {
                if (isCurrentPlayer(player)) {
                    if (!isStartingPointAvailable(startingPosition)) {
                        ErrorMessage startingPointTakenError = new ErrorMessage("StartingPointTaken");
                        String errorJson = JsonHandler.toJson(startingPointTakenError);
                        server.getConnectionHandlerById(player.getPlayerId()).sendMessage(errorJson);
                    } else {
                        player.getRobot().selectStartingPosition(startingPosition);

                        board.placeRobot(player.getRobot(), startingPosition[0], startingPosition[1]);
                        availableStartingPoints.removeIf(pos -> pos[0] == startingPosition[0]
                                && pos[1] == startingPosition[1]);

                        player.setHasChosen(true);
                        if (boardName.equalsIgnoreCase("deathtrap")) {
                            player.getRobot().setDirection(Direction.LEFT);
                        }
                        logger.info(player.getRobot().getDirection() + " direction of " + player.getName());

                        StartingPointTakenMessage stm = new StartingPointTakenMessage(
                                startingPosition[0], startingPosition[1],
                                player.getRobot().getDirection().toLowercaseString(), player.getPlayerId());
                        String stmJson = JsonHandler.toJson(stm);
                        server.broadcastToPlayers(stmJson, players);

                        if (!haveAllPlayersChosen()) {
                            moveToNextPlayer();
                        } else {
                            priorityOrder = board.determinePriority();
                            startUpgradePhase();
                        }
                    }
                } else {
                    ErrorMessage notYourTurnError = new ErrorMessage("NotYourTurn");
                    String errorJson = JsonHandler.toJson(notYourTurnError);
                    server.getConnectionHandlerById(player.getPlayerId()).sendMessage(errorJson);
                }
            }
        } else {
            ErrorMessage wrongPhaseError = new ErrorMessage(
                    "You can only do this during SetUp.");
            String errorJson = JsonHandler.toJson(wrongPhaseError);
            server.getConnectionHandlerById(player.getPlayerId()).sendMessage(errorJson);
        }
    }


    /**
     * Starts the upgrade phase.
     * Resets players chosen-flags and moves to the next phase based on priority.
     */
    public void startUpgradePhase() {
        setGameState(1);
        moveToNextPlayer();
        logger.info("Priority order" + priorityOrder);

        for (Player player : players) {
            player.resetRobot();
            player.setHasChosen(false);
        }
    }

    /**
     * Processes the purchase of an upgrade card.
     *
     * @param player   the player buying an upgrade.
     * @param upCard   the name of the upgrade card.
     * @param isBuying {@code true} if the player is buying, {@code false} otherwise.
     */
    public void buyUpgrade(Player player, String upCard, boolean isBuying) {
        if ((gameState != GameState.UPGRADE_PHASE)) {
            logger.severe("Not an upgrade phase");
            UpgradeBoughtMessage ubm = new UpgradeBoughtMessage(player.getPlayerId(), upCard);
            String ubmJson = JsonHandler.toJson(ubm);
            server.broadcastToPlayers(ubmJson, players);

            player.setHasChosen(true);

            if (!haveAllPlayersChosen()) {
                moveToNextPlayer();
            } else {
                handleUpgradePhaseEnd();
            }
            return;
        } else if (currentPlayer != player) {
            logger.severe("Player " + player.getName() + " is not currently in game");
            UpgradeBoughtMessage ubm = new UpgradeBoughtMessage(player.getPlayerId(), upCard);
            String ubmJson = JsonHandler.toJson(ubm);
            server.broadcastToPlayers(ubmJson, players);

            player.setHasChosen(true);

            if (!haveAllPlayersChosen()) {
                moveToNextPlayer();
            } else {
                handleUpgradePhaseEnd();
            }
            return;
        }
        if(player.hasChosen()){
            return;
        }
        if (!(isBuying)) {
            player.setHasChosen(true);
            if (!haveAllPlayersChosen()) {
                moveToNextPlayer();
            } else {
                handleUpgradePhaseEnd();
            }
            return;
        }
        UpgradeCardType upgradeCard = upgradeShop.getUpgradeByName(upCard);
        if(upgradeCard == null) {
            logger.severe("No upgrade found for " + upCard);
            UpgradeBoughtMessage ubm = new UpgradeBoughtMessage(player.getPlayerId(), upCard);
            String ubmJson = JsonHandler.toJson(ubm);
            server.broadcastToPlayers(ubmJson, players);

            player.setHasChosen(true);

            if (!haveAllPlayersChosen()) {
                moveToNextPlayer();
            } else {
                handleUpgradePhaseEnd();
            }
            return;
        }
        if (player.getEnergyCube() < upgradeCard.getEnergyCost()) {
            logger.severe("Player " + player.getName() + " does not have enough energy cube.");
            UpgradeBoughtMessage ubm = new UpgradeBoughtMessage(player.getPlayerId(), upCard);
            String ubmJson = JsonHandler.toJson(ubm);
            server.broadcastToPlayers(ubmJson, players);

            player.setHasChosen(true);

            if (!haveAllPlayersChosen()) {
                moveToNextPlayer();
            } else {
                handleUpgradePhaseEnd();
            }
            return;
        }
        // If everything is passes the checks:
        int newEnergyCount = player.getEnergyCube() - upgradeCard.getEnergyCost();
        player.setEngeryCubes(newEnergyCount);
        notifyEnergyValues(player, "UpgradeShop");

        upgradeShop.removeUpgrade(upgradeCard);

        UpgradeBoughtMessage ubm = new UpgradeBoughtMessage(player.getPlayerId(), upCard);
        String ubmJson = JsonHandler.toJson(ubm);
        server.broadcastToPlayers(ubmJson, players);

        player.addUpgradeCard(upgradeCard);
        player.setHasChosen(true);

        if (!haveAllPlayersChosen()) {
            moveToNextPlayer();
        } else {
            handleUpgradePhaseEnd();
        }
    }


    /**
     * Starts the programming phase of the game.
     * Deals hands to players and resets each player's robot and choice status.
     * Sets the game state to PROGRAMMING_PHASE.
     */
    private void startProgrammingPhase() {
        setGameState(2);
        dealPlayerHands();
        for (Player player : players) {
            player.setHasChosen(false);
            player.setAdminPrivilegeUsed(0);
        }
        adminPrivilegePlayers.clear();
    }

    /**
     * Allows a player to select a programming card for their register during the programming phase.
     *
     * @param player   the player taking their turn
     * @param cardName the name of the programming card to select
     * @param position the register slot where the card will be placed
     */
    public void takeTurnProgramming(Player player, String cardName, int position) {
        if (gameState == GameState.PROGRAMMING_PHASE) {
            logger.info(player.getRobotMat().getRegisters() + " before filling registers");
            if (!player.isRegisterFilled()) {
                player.selectCardForRegister(cardName, position);
            }
            logger.info(player.getRobotMat().getRegisters() + " AFTER filling registers");
            if (player.isRegisterFilled()) {
                logger.info(player.getName() + " register is filled with" + player.getRobotMat().getRegisters());
                selectionFinished(player);
            }
        } else {
            logger.severe("This action can not be performed right now.");
        }
    }

    /**
     * Begins the activation phase.
     * Fills empty registers, resets reboot status, and processes registers.
     */
    private void startActivationPhase() {
        logger.info("starting activation phase");
        for (Player player : players) {
            if (!player.isRegisterFilled()) {
                logger.info("filling empty register of " + player.getName() + " " + player.getRobotMat().getRegisters());
                fillEmptyRegisters(player);
                logger.info(" register of " + player.getName() + " is now: " + player.getRobotMat().getRegisters());
            }
        }
        setGameState(3);
        for (Player player : players) {
            player.setHasChosen(false);
        }
        currentRegisterIndex = 0;

        // Resetting rebooting robots
        for (Player player : players) {
            logger.info("Resetting rebooted robots.");
            player.getRobot().setIsRebooting(false);
        }
        processRegister();
    }

    /**
     * Processes the current register by notifying current cards and determining priority.
     */
    private void processRegister() {
        if (currentRegisterIndex >= 5) {
            currentRound += 1;
            clearRegisters();
            startUpgradePhase();
            return;
        }
        logger.info("Processing register: " + currentRegisterIndex);
        notifyCurrentCards(currentRegisterIndex);
        priorityOrder = board.determinePriority();
        priorityOrder = adjustPriorityOrder(priorityOrder, adminPrivilegePlayers,currentRegisterIndex);
        currentPlayerIndex = 0;
        processNextPlayer();
    }

    /**
     * Processes the next player in priority for the current register.
     * After all players have acted, activates board tiles.
     */
    private void processNextPlayer() {
        if (currentPlayerIndex >= priorityOrder.size()) {
            logger.info("Activating board tiles.");
            board.activateTiles(currentRegisterIndex);
            timerService.schedule(() -> {
                // Check if the game has been won after activating tiles
                if (hasGameBeenWon()) {
                    logger.info("Game is done");
                    endGame();
                    return;
                }

                // Proceed to the next register after activating tiles
                currentRegisterIndex++;
                processRegister();
            }, 3300, TimeUnit.MILLISECONDS);
            return;
        }
        Player player = priorityOrder.get(currentPlayerIndex);
        setCurrentPlayer(player);

        timerService.schedule(() -> {
            currentPlayerIndex++;
            processNextPlayer();
        }, 2, TimeUnit.SECONDS);
    }

    /**
     * Sets the game state and broadcasts the active phase to all players.
     *
     * @param phase 0 = setup, 1 = upgrade, 2 = programming, 3 = activation.
     */
    private void setGameState(int phase) {
        ActivePhaseMessage apm = new ActivePhaseMessage(0);
        switch (phase) {
            case 0:
                logger.info("Starting setup phase.");
                gameState = GameState.SETUP;
                apm = new ActivePhaseMessage(0);
                break;
            case 1:
                logger.info("Starting upgrade phase.");
                gameState = GameState.UPGRADE_PHASE;
                apm = new ActivePhaseMessage(1);
                break;
            case 2:
                logger.info("Starting programming phase.");
                gameState = GameState.PROGRAMMING_PHASE;
                apm = new ActivePhaseMessage(2);
                break;
            case 3:
                logger.info("Starting activation phase.");
                gameState = GameState.ACTIVATION_PHASE;
                apm = new ActivePhaseMessage(3);
                break;
            default:
                logger.severe("Invalid game state: " + phase);
        }

        String apmJson = JsonHandler.toJson(apm);
        server.broadcastToPlayers(apmJson, players);
    }

    /**
     * Notifies all players of the current cards for a register.
     *
     * @param currentRegister the register index (0–4).
     */
    private void notifyCurrentCards(int currentRegister) {
        List<CurrentCardsMessage.ActiveCard> activeCards = getActiveCards(currentRegister);
        CurrentCardsMessage currentCardsMessage = new CurrentCardsMessage(activeCards);
        String currentCardsJson = JsonHandler.toJson(currentCardsMessage);
        server.broadcastToPlayers(currentCardsJson, players);
    }


    /**
     * Retrieves the active card for each player for the current register.
     *
     * @param currentRegister the register index.
     * @return a list of active cards.
     */
    private List<CurrentCardsMessage.ActiveCard> getActiveCards(int currentRegister) {
        List<CurrentCardsMessage.ActiveCard> activeCards = new ArrayList<>();
        for (Player player : players) {
            Card card = player.getRobotMat().getRegisterCard(currentRegister);
            activeCards.add(new CurrentCardsMessage.ActiveCard(player.getPlayerId(), card.type().getName()));
        }
        return activeCards;
    }

    /**
     * Deals a hand of 9 cards to each player from their programming deck.
     * If a player's programming deck is empty, it is reset by moving discarded cards back into it.
     */
    private void dealPlayerHands() {
        for (Player player : players) {
            PersonalProgrammingDeck programmingDeck = player.getProgrammingDeck();
            logger.info("Dealing player hands.");
            ArrayList<Card> hand = player.getHand();

            for (int i = 0; i < 9; i++) {
                if (programmingDeck.isEmpty()) {
                    resetProgrammingDeck(player);
                }
                Card card = programmingDeck.drawCard();
                hand.add(card);
            }

            List<String> cardNames = new ArrayList<>();
            for (Card card : hand) {
                cardNames.add(card.type().getName());
            }
            //you
            YourCardsMessage ycm = new YourCardsMessage(cardNames);
            String ycmJson = JsonHandler.toJson(ycm);
            ConnectionHandler playerHandler = server.getConnectionHandlerById(player.getPlayerId());

            if (playerHandler != null) {
                playerHandler.sendMessage(ycmJson);
            }

            //others
            NotYourCardsMessage nycm = new NotYourCardsMessage(player.getPlayerId(), hand.size());
            String nycmJson = JsonHandler.toJson(nycm);

            if (playerHandler != null) {
                server.broadcastToPlayersExceptSelf(nycmJson, playerHandler, players);
            }
        }
    }

    /**
     * Resets a player's programming deck by moving cards from their discard deck back into it,
     * shuffling the deck for a random order.
     *
     * @param player the player whose programming deck will be reset
     */
    public void resetProgrammingDeck(Player player) {
        logger.info("Resetting programming decks.");
        ArrayList<Card> discardedCards = player.getDiscardDeck().getCards();
        player.getProgrammingDeck().addCards(discardedCards);
        player.getDiscardDeck().clearDeck();
        player.getProgrammingDeck().shuffle();
        ShuffleCodingMessage scm = new ShuffleCodingMessage(player.getPlayerId());
        String scmJson = JsonHandler.toJson(scm);
        server.broadcastToPlayers(scmJson, players);
    }

    /**
     * Notifies all players that a player has finished selecting their cards.
     *
     * @param playerId the player who has selected a card for their register.
     * @param registerPosition the register which has been filled.
     * @param filled true if a register has been filled, false otherwise.
     */
    public void notifyCardSelection(int playerId, int registerPosition, boolean filled) {
        CardSelectedMessage csm = new CardSelectedMessage(playerId, registerPosition, filled);
        String csmJson = JsonHandler.toJson(csm);
        server.broadcastToPlayers(csmJson, players);
    }

    /**
     * Notifies all players that a player has finished selecting their cards.
     *
     * @param player the player who has finished selection.
     */
    private void notifyCardSelectionFinished(Player player) {
        logger.info("notifyselecitonfinished");
        SelectionFinishedMessage sfm = new SelectionFinishedMessage(player.getPlayerId());
        String sfmJson = JsonHandler.toJson(sfm);
        server.broadcastToPlayers(sfmJson, players);
    }

    /**
     * Ends the player's selection phase and starts a timer if not all have chosen.
     *
     * @param player the player who just finished their selection.
     */
    private void selectionFinished(Player player) {
        logger.info("selection finished");
        player.discardHand();
        player.setHasChosen(true);
        notifyCardSelectionFinished(player);

        if (!isTimerRunning) {
            logger.info("starting timer");
            startTimer();
            return;
        }
        if (haveAllPlayersChosen()) {
            if (isTimerRunning) {
                cancelTimer();
            }
            logger.info("all players have chosen.");
            startActivationPhase();
        }
    }


    /**
     * Returns a list of player IDs who have not yet filled their registers.
     *
     * @return a list of client IDs.
     */
    private List<Integer> getClientsWithUnfilledRegisters() {
        List<Integer> clientsWithUnfilledRegisters = new ArrayList<>();
        for (Player player : players) {
            if (!player.isRegisterFilled()) {
                clientsWithUnfilledRegisters.add(player.getPlayerId());
            }
        }
        return clientsWithUnfilledRegisters;
    }

    /**
     * Starts a timer for the programming phase.
     * The timer runs for 30 seconds and fills any empty registers for players who have not filled their register.
     */
    private void startTimer() {
        isTimerRunning = true;
        int timerRound = currentRound;

        logger.info("Timer started.");
        TimerStartedMessage tsm = new TimerStartedMessage();
        String tsmJson = JsonHandler.toJson(tsm);
        server.broadcastToPlayers(tsmJson, players);

        timerService.schedule(() -> {
            logger.info("Timer ended.");
            TimerEndedMessage tem = new TimerEndedMessage(getClientsWithUnfilledRegisters());
            String temJson = JsonHandler.toJson(tem);
            server.broadcastToPlayers(temJson, players);
            isTimerRunning = false;

            if (getGameState() == GameState.PROGRAMMING_PHASE && timerRound == currentRound) {
                startActivationPhase();
            }

        }, 30, TimeUnit.SECONDS);
    }

    /**
     * Cancels the programming timer and resets the timer service.
     */
    private void cancelTimer() {
        if (isTimerRunning) {
            logger.info("Timer cancelled as all players have chosen.");
            timerService.shutdownNow();
            timerService = Executors.newScheduledThreadPool(1); // Timer-Dienst neu initialisieren
            isTimerRunning = false;
        }
    }


    /**
     * Fills empty registers of a player who did not finish in time with cards from their deck.
     * The player is marked as having filled their register, and the selection is considered finished.
     *
     * @param player the player whose register is being filled
     */
    private void fillEmptyRegisters(Player player) {
        player.discardHand();
        ArrayList<Card> registers = player.getRobotMat().getRegisters();
        List<Integer> emptyRegisterPosition = new ArrayList<>();
        for (int i = 0; i < registers.size(); i++) {
            if (registers.get(i) == null) {
                emptyRegisterPosition.add(i);
            }
        }

        logger.info("Filling empty registers.");
        List<String> cardsToRegister = new ArrayList<>();
        for (int registerIndex : emptyRegisterPosition) {
            if (player.getProgrammingDeck().isEmpty()) {
                resetProgrammingDeck(player);
                ShuffleCodingMessage scm = new ShuffleCodingMessage(player.getPlayerId());
                String scmJson = JsonHandler.toJson(scm);
                server.broadcastToPlayers(scmJson, players);
            }

            Card drawnCard = player.getProgrammingDeck().drawCard();
            if (registerIndex == 0) {
                while (drawnCard.type().getName().equalsIgnoreCase("Again")) {
                    Deck discardDeck = player.getDiscardDeck();
                    discardDeck.addCard(drawnCard);
                    if (player.getProgrammingDeck().isEmpty()) {
                        resetProgrammingDeck(player);
                        ShuffleCodingMessage scm = new ShuffleCodingMessage(player.getPlayerId());
                        String scmJson = JsonHandler.toJson(scm);
                        server.broadcastToPlayers(scmJson, players);
                    }
                    drawnCard = player.getProgrammingDeck().drawCard();

                }
            }
            if (drawnCard != null) {
                registers.set(registerIndex, drawnCard);
                cardsToRegister.add(drawnCard.type().getName());
            }
        }
        CardsYouGotNowMessage cygnm = new CardsYouGotNowMessage(cardsToRegister);
        String cygnmJson = JsonHandler.toJson(cygnm);
        server.getConnectionHandlerById(player.getPlayerId()).sendMessage(cygnmJson);
    }


    /**
     * Clears all registers for every player, moving cards to appropriate decks based on their type.
     * Damage cards are added to their respective damage decks, while programming cards are moved to the discard deck.
     * The registers are then reset to null.
     */
    private void clearRegisters() {
        for (Player player : players) {
            ArrayList<Card> register = player.getRobotMat().getRegisters();

            for (Card card : register) {
                if (card.type() instanceof DamageCardType damageCardType) {
                    switch (damageCardType) {
                        case SPAM -> spamDeck.addCard(card);
                        case TROJAN -> trojanDeck.addCard(card);
                        case VIRUS -> virusDeck.addCard(card);
                        case WORM -> wormDeck.addCard(card);
                    }
                } else if (card.type() instanceof ProgrammingCardType) {
                    player.getDiscardDeck().addCard(card);
                }
            }

            for (int i = 0; i < 5; i++) {
                register.set(i, null);
            }
            player.setRegisterFilled(false);
        }
    }


    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isCurrentPlayer(Player player) {
        return currentPlayer == player;
    }

    /**
     * Sets the current player and notifies all clients.
     *
     * @param player the player to set as current.
     */
    public void setCurrentPlayer(Player player) {
        currentPlayer = player;
        logger.info("current player is " + player.getName());
        CurrentPlayerMessage cpm = new CurrentPlayerMessage(player.getPlayerId());
        String cpmJson = JsonHandler.toJson(cpm);
        server.broadcastToPlayers(cpmJson, players);
    }

    /**
     * Advances the turn to the next player in line.
     */
    private void moveToNextPlayer() {
        if (gameState == GameState.UPGRADE_PHASE) {
            int currentPlayerIndex = priorityOrder.indexOf(currentPlayer);
            int nextPlayerIndex = (currentPlayerIndex + 1) % priorityOrder.size();
            setCurrentPlayer(priorityOrder.get(nextPlayerIndex));
        } else {
            int currentPlayerIndex = players.indexOf(currentPlayer);
            int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
            setCurrentPlayer(players.get(nextPlayerIndex));
        }
    }

    /**
     * Ends the upgrade phase by either refilling the shop or exchanging upgrades,
     * then starts the programming phase.
     */
    private void handleUpgradePhaseEnd() {
        if (upgradeShop.checkForShopReset(numberPlayers)) {
            List<String> newUpgradeCards = upgradeShop.exchangeShop(numberPlayers)
                    .stream()
                    .map(UpgradeCardType::getName)
                    .toList();
            logger.info("Upgrade cards " + newUpgradeCards);
            ExchangeShopMessage esm = new ExchangeShopMessage(newUpgradeCards);
            String esmJson = JsonHandler.toJson(esm);
            server.broadcastToPlayers(esmJson, players);
        } else {
            refillShop();
        }
        startProgrammingPhase();
    }


    /**
     * Checks if all players have completed their current selection.
     *
     * @return {@code true} if every player has chosen; {@code false} otherwise.
     */
    private boolean haveAllPlayersChosen() {
        for (Player player : players) {
            if (!player.hasChosen()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if any player has reached the winning number of checkpoints.
     *
     * @return {@code true} if a player has won; {@code false} otherwise.
     */
    private boolean hasGameBeenWon() {
        for (Player player : players) {
            if (player.getCheckpoints() == checkpointsToWin) {
                notifyGameFinished(getWinner());
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the winning player, if there is one.
     *
     * @return the winning Player, or {@code null} if none.
     */
    private Player getWinner() {
        for (Player player : players) {
            if (player.getCheckpoints() == checkpointsToWin) {
                return player;
            }
        }
        return null;
    }

    private void endGame() {
        logger.info("Game over!");
        getWinner();

    }

    /**
     * Sets the number of checkpoints required to win based on the board configuration.
     *
     * @param board the game board from which to retrieve the number of checkpoints.
     */
    public void setCheckpointsToWin(Board board) {
        checkpointsToWin = board.getCheckpoints();
    }

    /**
     * Populates the set of available figure IDs using the keys from the robot map.
     */
    private void populateAvailableFiguresSet() {
        availableFiguresSet.addAll(robotMap.keySet());
    }

    /**
     * Populates the robot map with robot instances corresponding to each figure.
     */
    private void populateRobotMap() {
        robotMap.put(0, createRobot(0, "ZoomBot"));
        robotMap.put(1, createRobot(1, "HammerBot"));
        robotMap.put(2, createRobot(2, "HulkX90"));
        robotMap.put(3, createRobot(3, "SmashBot"));
        robotMap.put(4, createRobot(4, "SpinBot"));
        robotMap.put(5, createRobot(5, "Twonky"));
    }

    /**
     * Creates a new robot with the specified ID and name.
     *
     * @param id   the robot ID.
     * @param name the robot name.
     * @return a new Robot instance with the given attributes and default direction RIGHT.
     */
    private Robot createRobot(int id, String name) {
        Robot robot = new Robot();
        robot.setId(id);
        robot.setName(name);
        robot.setDirection(Direction.RIGHT);
        return robot;
    }

    /**
     * Assigns a random available robot to a player.
     *
     * @return the assigned robot ID, or -1 if no robots are available.
     */
    public synchronized int assignRandomFigureId() {
        List<Integer> availableList = new ArrayList<>(availableFiguresSet);
        Random random = new Random();
        int randomIndex = random.nextInt(availableList.size());
        int figureId = availableList.get(randomIndex);
        availableFiguresSet.remove(figureId);
        return figureId;
    }

    public synchronized String getRobotNameById(int robotId) {
        Robot robot = robotMap.get(robotId);
        if (robot != null) {
            return robot.getName();
        } else return null;
    }

    public synchronized Robot getRobotById(int robotId) {
        return robotMap.get(robotId);
    }

    public synchronized boolean isFigureAvailable(int figureId) {
        return availableFiguresSet.contains(figureId);

    }

    /**
     * Assigns the specified figure to a player.
     * <p>
     * If the figureId is valid (not -1), it removes the figure from the available set,
     * assigns the corresponding robot to the player, and sets the player's robot.
     * </p>
     *
     * @param player   the player to assign the robot to.
     * @param figureId the ID of the figure (robot) to assign.
     */
    public synchronized void assignFigure(Player player, int figureId) {
        if (figureId != -1) {
            availableFiguresSet.remove(figureId);
            Robot robot = robotMap.get(figureId);
            robot.setPlayer(player);
            player.setRobot(robot);
        }
        // INFO von enno/kevin gelöscht? falls etwas nicht funktionert, wieder einfügen:
        // players.add(player);
    }

    /**
     * Marks a figure as available by adding its ID back to the available set if not already present.
     *
     * @param figureId the ID of the figure to make available.
     */
    public synchronized void makeFigureAvailable(int figureId) {
        if (!availableFiguresSet.contains(figureId)) {
            availableFiguresSet.add(figureId);
            logger.info("Figure " + figureId + " is now available.");
        }
    }


    /**
     * Plays a specified card for a given player, applying its effects.
     * <p>
     * If the card type is "AGAIN," the effect of the last played card is reapplied.
     * Otherwise, the effect of the specified card is applied. Invalid card names
     * result in an error message.
     * </p>
     *
     * @param player   The {@link Player} who is playing the card.
     * @param cardName The name of the card to be played.
     */
    public void playCard(Player player, String cardName) {
        int playerId = player.getPlayerId();
        CardType card = getCardTypeByName(cardName);
        // Activate Upgrade
        //TODO Should upgrade cards activate if player is rebooting?
        if (card != null) {
            if(card instanceof UpgradeCardType){
                card.applyEffect(player.getRobot());
                // Remove temporary upgrade cards
                if(!((UpgradeCardType) card).getIsPermanent()){
                    ArrayList<UpgradeCardType> upgrades = player.getUpgradeCards();
                    for (int i = 0; i < upgrades.size(); i++) {
                        if (upgrades.get(i) == card) {
                            upgrades.remove(i);
                            break;
                        }
                    }
                }
                notifyCardPlayed(playerId, cardName);
                return;
            }
        } else {
            logger.severe("Invalid upgrade card type.");
            return;
        }

        if (!player.getRobot().getIsRebooting()) {
            if (card != null) {
                if (card == ProgrammingCardType.AGAIN) {
                    CardType lastPlayedCard = getCardTypeByName(player.getLastPlayedCard());
                    lastPlayedCard.applyEffect(player.getRobot());
                } else {
                    player.setLastPlayedCard(cardName);
                    card.applyEffect(player.getRobot());
                }
                notifyCardPlayed(playerId, cardName);
            } else {
                logger.severe("Invalid card type.");
            }
        } else {
            logger.info("Player is rebooting. Skipping");
        }
    }

    /**
     * Broadcasts a message to all players indicating that a card has been played.
     *
     * @param playerId the ID of the player who played the card.
     * @param cardName the name of the played card.
     */
    public void notifyCardPlayed(int playerId, String cardName) {
        CardPlayedMessage cpm = new CardPlayedMessage(playerId, cardName);
        String cpmJson = JsonHandler.toJson(cpm);
        server.broadcastToPlayers(cpmJson, players);
    }

    /**
     * Retrieves a {@link CardType} by its name.
     * <p>
     * This method searches through available {@link ProgrammingCardType} and
     * {@link DamageCardType} enums to find a matching card by name. The comparison
     * is case-insensitive.
     * </p>
     *
     * @param cardName The name of the card to look up.
     * @return The {@link CardType} that matches the name, or {@code null} if no match is found.
     */
    public CardType getCardTypeByName(String cardName) {
        for (ProgrammingCardType cardType : ProgrammingCardType.values()) {
            if (cardType.getName().equalsIgnoreCase(cardName)) {
                return cardType;
            }
        }

        for (DamageCardType cardType : DamageCardType.values()) {
            if (cardType.getName().equalsIgnoreCase(cardName)) {
                return cardType;
            }
        }

        for (UpgradeCardType cardType : UpgradeCardType.values()) {
            if (cardType.getName().equalsIgnoreCase(cardName)   ) {
                return cardType;
            }
        }
        return null;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.getPlayerId() == id) {
                return p;
            }
        }
        return null;
    }

    /**
     * Notifies all players that a particular client reached a specific checkpoint on the board.
     *
     * @param clientID         the ID of the client who reached the checkpoint
     * @param checkpointNumber the number of the checkpoint reached
     */
    public void notifyCheckpointReached(int clientID, int checkpointNumber) {
        CheckPointReachedMessage crm = new CheckPointReachedMessage(clientID, checkpointNumber);
        String crmJson = JsonHandler.toJson(crm);
        server.broadcastToPlayers(crmJson, players);
    }

    /**
     * Notifies all players about updated energy values for a specific player.
     *
     * @param player the {@link Player} whose energy value changed
     * @param source a short descriptor indicating the source of the energy gain (e.g. "EnergySpace")
     */
    public void notifyEnergyValues(Player player, String source) {
        EnergyMessage em = new EnergyMessage(player.getPlayerId(), player.getEnergyCube(), source);
        String emJson = JsonHandler.toJson(em);
        server.broadcastToPlayers(emJson, players);
    }

    /**
     * Notifies all players about a movement event, indicating that a player moved to a new position.
     *
     * @param playerId the ID of the player whose robot has moved
     * @param x        the new x-coordinate of the robot
     * @param y        the new y-coordinate of the robot
     */
    public void notifyMovement(int playerId, int x, int y) {
        MovementMessage mov = new MovementMessage(playerId, x, y);
        String movJson = JsonHandler.toJson(mov);
        server.broadcastToPlayers(movJson, players);
    }

    /**
     * Notifies all players that a player's robot has turned in a specified rotation.
     *
     * @param playerId the ID of the player whose robot is turning
     * @param rotation the rotation direction (e.g., "clockwise" or "counterclockwise")
     */
    public synchronized void notifyTurning(int playerId, String rotation) {
        PlayerTurningMessage ptm = new PlayerTurningMessage(playerId, rotation);
        String ptmJson = JsonHandler.toJson(ptm);
        server.broadcastToPlayers(ptmJson, players);
    }

    /**
     * Notifies all players that a particular player's robot is rebooting.
     *
     * @param playerId the ID of the player whose robot is rebooting
     */
    public void notifyReboot(int playerId) {
        RebootMessage rm = new RebootMessage(playerId);
        String rmJson = JsonHandler.toJson(rm);
        server.broadcastToPlayers(rmJson, players);
    }

    /**
     * Notifies all players that a specific animation type should be triggered.
     *
     * @param type a string representing the type of animation (e.g., a key or identifier)
     */
    public void notifyAnimation(String type) {
        AnimationMessage anm = new AnimationMessage(type);
        String anmJson = JsonHandler.toJson(anm);
        server.broadcastToPlayers(anmJson, players);
    }

    /**
     * Notifies all players that a player's card in a given register has been replaced with a new card.
     *
     * @param register the register index where the card replacement occurred
     * @param newCard  the name or type of the new card placed in the register
     * @param playerId the ID of the player whose card was replaced
     */
    public void notifyReplaceCard(int register, String newCard, int playerId) {
        ReplaceCardMessage rcm = new ReplaceCardMessage(register, newCard, playerId);
        String rcmJson = JsonHandler.toJson(rcm);
        server.broadcastToPlayers(rcmJson, players);
    }

    /**
     * Notifies all players that the game has finished and broadcasts the winner's ID.
     * Also sets the server state to indicate that the game is no longer running.
     *
     * @param player the winning {@link Player}
     */
    public void notifyGameFinished(Player player) {
        GameFinishedMessage gfm = new GameFinishedMessage(player.getPlayerId());
        String gfmJson = JsonHandler.toJson(gfm);
        server.broadcastToAll(gfmJson);
        server.isGameRunning = false;
    }

    public void setBoardName(String board) {
        this.boardName = board;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Draws a specified count of damage cards from the appropriate deck for a player.
     * Adds these cards to the player's personal discard deck and notifies other players.
     * If the deck is empty before drawing the needed cards, prompts the player to pick damage from available piles.
     *
     * @param player the {@link Player} receiving the damage cards
     * @param card   the type of damage card (e.g., SPAM, VIRUS, TROJAN, WORM)
     * @param count  how many cards should be drawn
     */
    public void drawDamageCard(Player player, DamageCardType card, int count) {
        DamageCardsDeck deck;
        switch (card) {
            case SPAM -> deck = spamDeck;
            case TROJAN -> deck = trojanDeck;
            case VIRUS -> deck = virusDeck;
            case WORM -> deck = wormDeck;
            default -> throw new IllegalArgumentException("Invalid DamageCardType");
        }

        List<String> drawnCards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card drawnCard = deck.drawCard();
            if (drawnCard != null) {
                player.getDiscardDeck().addCard(drawnCard);
                drawnCards.add(drawnCard.type().getName());
            } else {
                logger.warning(card.getName() + " deck is empty.");
                notifyPickDamage(player, count - i, getAvailablePiles());
                return;
            }
        }
        notifyDrawDamage(player, drawnCards);
    }

    private List<String> getAvailablePiles() {
        List<String> availablePiles = new ArrayList<>();
        if (!spamDeck.isEmpty()) availablePiles.add("Spam");
        if (!wormDeck.isEmpty()) availablePiles.add("Worm");
        if (!trojanDeck.isEmpty()) availablePiles.add("Trojan");
        if (!virusDeck.isEmpty()) availablePiles.add("Virus");
        return availablePiles;
    }

    /**
     * Notifies a specific player that they must pick a certain number of damage cards
     * from the provided available piles.
     *
     * @param player         the {@link Player} who must pick the damage
     * @param count          the number of damage cards the player needs to pick
     * @param availablePiles a list of available damage card piles (e.g., "Spam", "Trojan")
     */
    public void notifyPickDamage(Player player, int count, List<String> availablePiles) {
        PickDamageMessage pdm = new PickDamageMessage(count, availablePiles);
        String pdmJson = JsonHandler.toJson(pdm);
        server.getConnectionHandlerById(player.getPlayerId()).sendMessage(pdmJson);
    }

    /**
     * Notifies all players about damage cards that a specific player has drawn.
     *
     * @param player the {@link Player} who drew the damage cards
     * @param cards  a list of names/types of the drawn damage cards
     */
    public void notifyDrawDamage(Player player, List<String> cards) {
        DrawDamageMessage ddm = new DrawDamageMessage(player.getPlayerId(), cards);
        String ddmJson = JsonHandler.toJson(ddm);
        server.broadcastToPlayers(ddmJson, players);
    }

    /**
     * Returns a {@link Player} instance associated with the specified robot.
     *
     * @param robot the {@link Robot} whose owning player is being queried
     * @return the corresponding {@link Player}, or {@code null} if not found
     */
    public Player getPlayerByRobot(Robot robot) {
        for (Player player : players) {
            if (player.getRobot().equals(robot)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Selects the requested damage card types for a player and adds them to the player's discard deck.
     *
     * @param player        the {@link Player} who is selecting the damage cards
     * @param selectedCards a list of damage card names/types (e.g., "Spam", "Virus")
     */
    public void setSelectedDamage(Player player, List<String> selectedCards) {
        for (String card : selectedCards) {
            switch (card) {
                case "Spam":
                    player.getDiscardDeck().addCard(spamDeck.drawCard());
                case "Virus":
                    player.getDiscardDeck().addCard(virusDeck.drawCard());
                case "Worm":
                    player.getDiscardDeck().addCard(wormDeck.drawCard());
                case "Trojan":
                    player.getDiscardDeck().addCard(trojanDeck.drawCard());
            }
        }
    }

    private void gatherStartingPoints() {
        List<List<List<Tile>>> boardMap = board.getMap();
        for (int x = 0; x < boardMap.size(); x++) {
            for (int y = 0; y < boardMap.get(x).size(); y++) {
                List<Tile> tileList = boardMap.get(x).get(y);
                for (Tile tile : tileList) {
                    if (tile instanceof StartPointTile) {
                        availableStartingPoints.add(new int[]{x, y});
                    }
                }
            }
        }
    }

    private boolean isStartingPointAvailable(int[] position) {
        for (int[] sp : availableStartingPoints) {
            if (sp[0] == position[0] && sp[1] == position[1]) {
                logger.info("starting point is available");
                return true;
            }
        }
        logger.info("starting point is NOT available");
        return false;
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getMinPlayers() {
        return minPlayers;
    }


    /**
     * Adjusts the priority order based on players using priority upgrade cards.
     * The most recent user of the priority upgrade gets the highest priority.
     *
     * @param originalOrder The default priority order determined by the board.
     * @param adminPrivilegePlayers A map tracking when each player last used the priority upgrade card.
     *                         Key: Player ID, Value: The register index in which they used the upgrade.
     * @param currentRegisterIndex The current register being processed.
     * @return A new list with adjusted priority order.
     */
    private List<Player> adjustPriorityOrder(List<Player> originalOrder,
                                             Map<Integer, Integer> adminPrivilegePlayers,
                                             int currentRegisterIndex) {
        // Separate players who used the priority upgrade in this register
        List<Player> newPriorityOrder = new ArrayList<>();
        List<Player> remainingPlayers = new ArrayList<>(originalOrder);
        for (int i = originalOrder.size() - 1; i >= 0; i--) {
            Player player = originalOrder.get(i);
            int playerId = player.getPlayerId();
            if (adminPrivilegePlayers.containsKey(playerId) && (adminPrivilegePlayers.get(playerId) == currentRegisterIndex)) {
                // Add player that used it last, first
                newPriorityOrder.add(player);
                remainingPlayers.remove(player);
            }
        }
        newPriorityOrder.addAll(remainingPlayers);
        return newPriorityOrder;
    }

    /**
     * Adds a player to the admin privilege mapping.
     *
     * @param clientID the client ID.
     * @param register the register index.
     */
    public void addPlayerToAdminPrivilege(int clientID, int register) {
        adminPrivilegePlayers.put(clientID, register);
    }

    public Map<Integer, Integer> getPlayerToAdminPrivilege() {
        return adminPrivilegePlayers;
    }

    public int getCurrentRegisterIndex(){
        return currentRegisterIndex;
    }

    /**
     * Notifies that a checkpoint has moved.
     *
     * @param x          the new x-coordinate.
     * @param y          the new y-coordinate.
     * @param checkpointID the checkpoint ID.
     */
    public void notifyCheckpointMoved(int x, int y, int checkpointID) {
        CheckpointMovedMessage cmm = new CheckpointMovedMessage(checkpointID, x, y);
        String cmmJson = JsonHandler.toJson(cmm);
        server.broadcastToPlayers(cmmJson, players);
    }

    /**
     * Handles memory swap by drawing three new cards for a player.
     *
     * @param robot the robot belonging to the player.
     */
    public void handleMemorySwap(Robot robot){
        Player player = robot.getPlayer();
        PersonalProgrammingDeck programmingDeck = player.getProgrammingDeck();
        ArrayList<Card> hand = player.getHand();

        if(gameState != GameState.PROGRAMMING_PHASE){
            ErrorMessage wrongPhaseError = new ErrorMessage(
                    "Card can only be used during the programming phase.");
            String errorJson = JsonHandler.toJson(wrongPhaseError);
            server.getConnectionHandlerById(player.getPlayerId()).sendMessage(errorJson);
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (programmingDeck.isEmpty()) {
                resetProgrammingDeck(player);
            }
            Card card = programmingDeck.drawCard();
            hand.add(card);
        }
        List<String> cardNames = new ArrayList<>();
        for (Card card : hand) {
            cardNames.add(card.type().getName());
        }
        //
        YourCardsMessage ycm = new YourCardsMessage(cardNames);
        String ycmJson = JsonHandler.toJson(ycm);
        ConnectionHandler playerHandler = server.getConnectionHandlerById(player.getPlayerId());

        if (playerHandler != null) {
            playerHandler.sendMessage(ycmJson);
        }
    }

    /**
     * Handles the effect of the SpamBlocker card.
     *
     * @param robot the robot belonging to the player.
     */
    public void handleSpamBlocker(Robot robot) {

        Player player = robot.getPlayer();

        PersonalProgrammingDeck programmingDeck = player.getProgrammingDeck();
        ArrayList<Card> hand = player.getHand();
        ArrayList<String> newCards = new ArrayList<>(); // Statt Spam


        if (gameState != GameState.PROGRAMMING_PHASE) {

            ErrorMessage wrongPhaseError = new ErrorMessage(
                    "Card can only be used during the programming phase.");
            String errorJson = JsonHandler.toJson(wrongPhaseError);
            server.getConnectionHandlerById(player.getPlayerId()).sendMessage(errorJson);
            return;
        }

        Iterator<Card> iterator = hand.iterator();
        while (iterator.hasNext()) {
            Card card = iterator.next();
            if (card.type() == DamageCardType.SPAM) {
                spamDeck.addCard(card);
                iterator.remove();
                if (programmingDeck.isEmpty()) {
                    resetProgrammingDeck(player);
                }
                Card newCard = programmingDeck.drawCard();
                newCards.add(newCard.type().getName());
            }
        }

        YourCardsMessage ycm = new YourCardsMessage(newCards);
        String ycmJson = JsonHandler.toJson(ycm);
        ConnectionHandler playerHandler = server.getConnectionHandlerById(player.getPlayerId());
        if (playerHandler != null) {
            playerHandler.sendMessage(ycmJson);
        }
    }
}
