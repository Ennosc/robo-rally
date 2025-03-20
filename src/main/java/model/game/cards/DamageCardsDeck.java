package model.game.cards;

/**
 * A deck of damage cards for a specific damage type.
 */
public class DamageCardsDeck extends Deck {
    private final DamageCardType cardType;


    /**
     * Constructs a DamageCardsDeck for the specified damage card type.
     *
     * @param cardType the {@link DamageCardType} of the deck.
     */
    public DamageCardsDeck(DamageCardType cardType) {
        super();
        this.cardType = cardType;
    }

    /**
     * Initializes the deck by adding a fixed number of cards based on the damage card type.
     */
    @Override
    public void initializeDeck() {
        int count = switch (cardType) {
            case SPAM -> 38;
            case TROJAN -> 12;
            case VIRUS -> 18;
            case WORM -> 6;
        };

        for (int i = 0; i < count; i++) {
            deck.add(new Card(cardType));
        }
    }
}