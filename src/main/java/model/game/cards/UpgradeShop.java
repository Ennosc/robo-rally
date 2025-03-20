package model.game.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


/**
 * Represents the upgrade shop from which players can buy upgrade cards.
 */
public class UpgradeShop {

    private int mayUpgrades;

    //the three cards that can be chosen
    private List<UpgradeCardType> availableUpgrades;

    private UpgradeCardsDeck upgradeDeck;
    private int maxUpgrades ;
    private Logger logger;

    /**
     * Constructs an UpgradeShop with the specified upgrade deck.
     *
     * @param upgradeDeck the UpgradeCardsDeck to use.
     */
    public UpgradeShop(UpgradeCardsDeck upgradeDeck) {
        this.upgradeDeck = upgradeDeck;
        this.availableUpgrades = new ArrayList<>();
        this.logger = Logger.getLogger(this.getClass().getName());

    }

    //if less then 3 cards
    //gets called after every bought upgrade
    /**
     * Refills the shop with upgrade cards until it reaches the number of players.
     * <p>
     * Cards are drawn from the upgrade deck and added to the shop until the available upgrades
     * count matches the number of players or the deck is empty.
     * </p>
     *
     * @param playerNumber the number of players.
     * @return an unmodifiable list of the available upgrade cards.
     */
    public List<UpgradeCardType> refillShop(int playerNumber) {
        while (availableUpgrades.size() < playerNumber && !upgradeDeck.isEmpty()) {
            UpgradeCardType newUpgrade = upgradeDeck.drawUpgradeCard();
            availableUpgrades.add(newUpgrade);

        }
        logger.info("Upgrade-Shop wurde erneuert.");
        return availableUpgrades;
    }

    //if no upgrade was bought
    //gets checked after every player has chosen in upgrade phase
    /**
     * Clears the current shop and refills it with upgrade cards.
     *
     * @param playerNumber the number of players.
     * @return the new list of available upgrade cards.
     */
    public List<UpgradeCardType> exchangeShop(int playerNumber){
        availableUpgrades.clear();
        refillShop(playerNumber);
        return availableUpgrades;
    }

    //falls kein upgrade gekauft wurde ist der shop noch max groß
    /**
     * Checks whether the shop should be reset.
     *
     * @param numberOfPlayers the number of players.
     * @return {@code true} if the shop size equals the number of players; {@code false} otherwise.
     */
    public boolean checkForShopReset(int numberOfPlayers) {
        return availableUpgrades.size() == numberOfPlayers;
    }

    /**
     * Retrieves an upgrade card by its name.
     *
     * @param upgradeName the name of the upgrade card.
     * @return the matching UpgradeCardType, or {@code null} if not found.
     */
    public UpgradeCardType getUpgradeByName(String upgradeName) {
        return availableUpgrades.stream()
                .filter(upgrade -> upgrade.getName().equalsIgnoreCase(upgradeName))
                .findFirst()
                .orElse(null);
    }


    /**
     * Removes the specified upgrade card from the shop.
     *
     * @param upgradeCard the upgrade card to remove.
     */
    public void removeUpgrade(UpgradeCardType upgradeCard) {
        if (availableUpgrades.remove(upgradeCard)) {

            logger.info("Upgrade " + upgradeCard.getName() + " wurde aus dem Shop entfernt.");
        } else {
            logger.warning("Upgrade " + upgradeCard.getName() + " war nicht im Shop.");
        }
    }


    public List<UpgradeCardType> getAvailableUpgrades() {
        return Collections.unmodifiableList(availableUpgrades);
    }
}