package model.game.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player's personal programming deck.
 */
public class PersonalProgrammingDeck extends Deck {
    /**
     * Constructs a PersonalProgrammingDeck and initializes it.
     */
    public PersonalProgrammingDeck() {
        super();
        initializeDeck();
    }

    /**
     * Initializes the programming deck with a set of predefined programming cards.
     * The deck is then shuffled.
     */
    @Override
    public void initializeDeck() {
        for (int i = 0; i < 5; i++) {
            deck.add(new Card(ProgrammingCardType.MOVE_1));
        }

        for (int i = 0; i < 3; i++) {
            deck.add(new Card(ProgrammingCardType.MOVE_2));
            deck.add(new Card(ProgrammingCardType.TURN_RIGHT));
            deck.add(new Card(ProgrammingCardType.TURN_LEFT));
        }

        for (int i = 0; i < 2; i++) {
            deck.add(new Card(ProgrammingCardType.AGAIN));
        }

        deck.add(new Card(ProgrammingCardType.MOVE_3));
        deck.add(new Card(ProgrammingCardType.BACK_UP));
        deck.add(new Card(ProgrammingCardType.U_TURN));
        deck.add(new Card(ProgrammingCardType.POWER_UP));

        shuffle();
    }
}
