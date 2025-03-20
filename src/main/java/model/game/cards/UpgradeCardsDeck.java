package model.game.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a deck of upgrade cards.
 */
public class UpgradeCardsDeck extends Deck {

    private static final int COUNT = 10;

    /**
     * Constructs an UpgradeCardsDeck and initializes it.
     */
    public UpgradeCardsDeck() {
        super();
        initializeDeck();
    }

    /**
     * Initializes the upgrade deck with a set number of each upgrade card.
     * The deck is then shuffled.
     */
    @Override
    public void initializeDeck() {
        for (int i = 0; i < COUNT; i++) {
            deck.add(new Card(UpgradeCardType.ADMIN_PRIVILEGE));
            deck.add(new Card(UpgradeCardType.REAR_LASER));
            deck.add(new Card(UpgradeCardType.MEMORY_SWAP));
            deck.add(new Card(UpgradeCardType.SPAM_BLOCKER));
        }
        shuffle();
    }

    /**
     * Draws an upgrade card from the deck.
     *
     * @return the drawn {@link UpgradeCardType} if available; {@code null} otherwise.
     */
    public UpgradeCardType drawUpgradeCard() {
        Card drawnCard = super.drawCard();
        if (drawnCard != null && drawnCard.type() instanceof UpgradeCardType upgradeType) {
            return upgradeType;
        }
        return null;
    }



}