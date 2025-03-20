package model.game.cards;

/**
 * Represents a card used in the game. Each card is associated with a {@link CardType}.
 */
public record Card(CardType type) {
    /**
     * Returns the type of this card.
     *
     * @return the {@link CardType} of the card.
     */
    public CardType type(){
        return type;
    }
    @Override
    public String toString() {
        return "game.cards.Card{" +
                "type=" + type +
                '}';
    }
}