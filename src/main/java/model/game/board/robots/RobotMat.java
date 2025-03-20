package model.game.board.robots;

import model.game.cards.Card;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class represents the control mat for a robot.
 * <p>
 *     The RobotMat holds the programmed register cards for a robot as well as lists for permanent
 *     and temporary upgrades.
 * </p>
 */
public class RobotMat {
    private final ArrayList<Card> registers;
    private final ArrayList<Card> permanentUpgrades;
    private final ArrayList<Card> temporaryUpgrades;


    /**
     * Constructs a new RobotMat.
     */
    public RobotMat() {
        this.registers = new ArrayList<>(Collections.nCopies(5, null));
        this.permanentUpgrades = new ArrayList<>(3);
        this.temporaryUpgrades = new ArrayList<>(3);
    }

    // Setters and getters

    public void setRegisters(Card cardName, int position) {
        registers.set(position, cardName);
    }
    public Card getRegisters(int pos) {
        return registers.get(pos);
    }
    public boolean isRegisterFull() {
        return !registers.contains(null);
    }

    public ArrayList<Card> getRegisters() {
        return registers;
    }

    public Card getRegisterCard(int position) {
       if (position >= 0 && position < registers.size()) {
           return registers.get(position);
        }
        return null;
    }
}