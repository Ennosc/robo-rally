package model.game.cards;

import model.game.Player;
import model.game.board.robots.Robot;

/**
 * Interface for they type of a card and defines the effect that the card has when played.
 */
public interface CardType {
    String getName();

    void applyEffect(Robot robot);
}
