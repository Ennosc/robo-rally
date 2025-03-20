package model.game.cards;

import model.game.Game;
import model.game.board.robots.Robot;
import model.server_client.ConnectionHandler;
import network.JsonHandler;
import network.messages.phases7.programming.NotYourCardsMessage;
import network.messages.phases7.programming.YourCardsMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration of upgrade card types.
 * <p>
 * Each upgrade card type may have an associated energy cost and a flag indicating if it is permanent.
 * Some upgrade cards (e.g., MEMORY_SWAP, SPAM_BLOCKER) have effects that are applied when played.
 * </p>
 */
public enum UpgradeCardType implements CardType {

    ADMIN_PRIVILEGE("AdminPrivilege",3, true){
        @Override
        public void applyEffect(Robot robot) {
            // Effect not handled here
        }
    },
    REAR_LASER("RearLaser",2, true){
        @Override
        public void applyEffect(Robot robot) {
            // Effect not handled here
        }
    },
    MEMORY_SWAP("MemorySwap",1, false){
        @Override
        public void applyEffect(Robot robot) {
            Game.getInstance().handleMemorySwap(robot);
        }
    },
    SPAM_BLOCKER("SpamBlocker",3, false){
        @Override
        public void applyEffect(Robot robot) {
            Game.getInstance().handleSpamBlocker(robot);
        }
    },;

    private final String name;
    private final int energyCost;
    private final boolean isPermanent;

    UpgradeCardType(String name,int energyCost, boolean isPermanent) {
        this.name = name;
        this.energyCost = energyCost;
        this.isPermanent = isPermanent;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean getIsPermanent() {
        return isPermanent;
    }

    @Override
    public void applyEffect(Robot robot) {}
    public int getEnergyCost() {
        return energyCost;
    }
}