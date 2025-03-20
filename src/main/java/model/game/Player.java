package model.game;

import model.game.board.robots.Robot;
import model.game.board.robots.RobotMat;
import model.game.cards.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player in the game.
 * <p>
 * The Player class stores information about the player's identity, robot, energy, checkpoints,
 * cards (hand, programming deck, discard deck), upgrades, and game status.
 * </p>
 */
public class Player {
    private String name;
    private int playerId;
    private Robot robot;
    private RobotMat robotMat;

    private int energyCubes;
    private int checkpoints;
    private boolean registerFilled;
    private boolean hasChosen;
    private String lastPlayedCard;
    private boolean playerStatus;
    private boolean isAI;

    private PersonalProgrammingDeck programmingDeck;
    private PersonalDiscardDeck discardDeck;
    private ArrayList<Card> hand;
    private ArrayList<UpgradeCardType> upgrades;

    private Game game;

    private int adminPrivilegeUsed;

    /**
     * Constructs a new Player with the given ID, name, and robot.
     *
     * @param id    the player's ID.
     * @param name  the player's name.
     * @param robot the robot assigned to the player.
     */
    public Player(int id, String name, Robot robot) {
        this.name = name;
        this.playerId = id;
        this.robot = robot;
        this.robotMat = new RobotMat();
        this.isAI = false;
        this.upgrades = new ArrayList<>();
        resetPlayer();
    }

    public String getName(){
        return name;
    }

    public Robot getRobot() {
        return robot;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getEnergyCube() {
        return energyCubes;
    }

    public int getCheckpoints() {
        return checkpoints;
    }

    public RobotMat getRobotMat() {
       return robotMat;
    }

    public PersonalProgrammingDeck getProgrammingDeck() {
        return programmingDeck;
    }

    public Deck getDiscardDeck() {
        return discardDeck;
    }

    public ArrayList<Card> getHand() {
        return hand;
    }

    public boolean hasChosen() {
        return hasChosen;
    }

    public boolean isRegisterFilled() {
        return registerFilled;
    }

    public String getLastPlayedCard () {
        return lastPlayedCard;
    }

    /**
     * Returns the player's readiness status.
     *
     * @return {@code true} if the player is ready; {@code false} otherwise.
     */
    public boolean isPlayerReady() {return this.playerStatus = playerStatus;}

    /**
     * Sets the player's readiness status.
     *
     * @param isReady {@code true} if ready; {@code false} otherwise.
     */
    public void setPlayerStatus(boolean isReady){
        this.playerStatus=isReady;
    }

    public void setLastPlayedCard (String cardName){
        lastPlayedCard = cardName;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public void setHasChosen(boolean hasChosen) {
        this.hasChosen = hasChosen;
    }

    public void setRegisterFilled(boolean registerFilled) {
        this.registerFilled = registerFilled;
    }

    /**
     * Sets the number of checkpoints reached by the player.
     *
     * @param checkpoints the checkpoint count.
     */
    public void setCheckpoints(int checkpoints) {
        this.checkpoints = checkpoints;
    }

    /**
     * Adds energy cubes to the player's current total.
     *
     * @param amount the number of cubes to add.
     * @return the new energy cube count.
     */
    public int addEnergyCubes(int amount) {
        return this.energyCubes = this.energyCubes + amount;
    }

    public void setEngeryCubes(int cubes) {
        this.energyCubes = cubes;
    }

    /**
     * Resets the robot's state by marking it as not rebooting.
     */
    public void resetRobot(){
        robot.setIsRebooting(false);
    }

    /**
     * Places a card from the players hand into the specified register slot.
     *
     * @param cardName the name of the card to be placed in the register, or {@code "null"} to leave the slot empty
     * @param registerPosition the position in the register where the card should be placed
     * @return {@code true} if the card was successfully placed in the register, {@code false} otherwise
     */
    public boolean selectCardForRegister(String cardName, int registerPosition) {
        if (registerFilled) {
         return false;
        }

        if (cardName.equals("null")) {
            hand.add(robotMat.getRegisters(registerPosition));
            updateRegister(null, registerPosition);
            Game.getInstance().notifyCardSelection(playerId, registerPosition, false);
            return true;
        }

        if (cardName.equals("again") && registerPosition == 0) {
            return false;
        }

        for (Card card: hand) {
            if (card.type().getName().equals(cardName)) {
                updateRegister(card, registerPosition);
                hand.remove(card);
                Game.getInstance().notifyCardSelection(playerId, registerPosition,true);
                if(robotMat.isRegisterFull()) {
                    registerFilled  = true;
                }
                return true;
            }
        }
        return false;
    }

    public void buyUpgrade(String upgradeCardName){
        //TODO
    }

    public ArrayList<UpgradeCardType> getUpgradeCards(){
        return upgrades;
    }

    /**
     * Adds an upgrade card to the player's collection.
     *
     * @param UpCard the upgrade card to add.
     */
    public void addUpgradeCard(UpgradeCardType UpCard){
        upgrades.add(UpCard);
    }

    /**
     * Updates the player's register with a card at the specified position.
     *
     * @param cardName the card to place (or {@code null}).
     * @param position the register position.
     */
    public void updateRegister(Card cardName, int position) {
        robotMat.setRegisters(cardName, position);
   }

    /**
     * Moves all cards from the player's hand to the discard deck and clears the hand.
     */
    public void discardHand() {
        discardDeck.addCards(hand);
        hand.clear();
    }

    //Memory swap
    /**
     * Draws three cards from the programming deck and adds them to the player's hand.
     *
     * @return the updated hand.
     */
    public ArrayList<Card> addThreeCardsToHand() {
        hand.add(programmingDeck.drawCard());
        hand.add(programmingDeck.drawCard());
        hand.add(programmingDeck.drawCard());
        return hand;
    }

    /**
     * Handles discarding specific cards from the player's hand.
     *
     * @param discardCards the list of cards to discard.
     */
    public void handleDiscardSome(ArrayList<Card> discardCards) {
        discardDeck.addCards(discardCards);
        hand.removeAll(discardCards);
    }

    /**
     * Resets the player's state for a new game.
     */
    public void resetPlayer(){
        energyCubes = 5;
        checkpoints = 0;
        hasChosen = false;
        registerFilled = false;
        programmingDeck = new PersonalProgrammingDeck();
        discardDeck = new PersonalDiscardDeck();
        hand = new ArrayList<>();
        lastPlayedCard = null;
        this.playerStatus = Boolean.parseBoolean(null);
        upgrades = new ArrayList<>();
    }

    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean isAI) {
        this.isAI = isAI;
    }

    public void setAdminPrivilegeUsed(int adminPrivilegeUsed) {
        this.adminPrivilegeUsed = adminPrivilegeUsed;
    }

    public int getAdminPrivilegeUsed() {
        return adminPrivilegeUsed;
    }
}
