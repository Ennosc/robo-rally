package model.game.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Abstract class representing a deck of cards.
 */
public abstract class Deck {
    protected List<Card> deck;

    /**
     * Constructs an empty deck.
     */
    public Deck() {
        deck = new ArrayList<>();
    }

    /**
     * Initializes the deck with a predefined set of cards.
     */
    public abstract void initializeDeck();

    /**
     * Shuffles the deck.
     */
    public void shuffle() {
        Collections.shuffle(deck);
    }

    /**
     * Checks if the deck is empty.
     *
     * @return {@code true} if the deck has no cards; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return deck.isEmpty();
    }

    /**
     * Clears all cards from the deck.
     */
    public void clearDeck() {
        deck.clear();}

    /**
     * Draws the top card from the deck.
     *
     * @return the drawn {@link Card}, or {@code null} if the deck is empty.
     */
    public Card drawCard() {
        if (!isEmpty()) {
            return deck.remove(0);
        }
        return null;
    }

    /**
     * Adds a list of cards to the deck.
     *
     * @param cardsToAdd the list of cards to add.
     */
    public void addCards(ArrayList<Card> cardsToAdd) {
        deck.addAll(cardsToAdd);
    }

    /**
     * Adds a single card to the deck.
     *
     * @param card the card to add.
     */
    public void addCard(Card card){
        deck.add(card);
    }

    public ArrayList<Card> getCards() {
        return new ArrayList<>(deck);
    }
}
