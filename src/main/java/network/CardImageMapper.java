package network;

import model.game.cards.Card;
import model.game.cards.ProgrammingCardType;

import java.util.HashMap;
import java.util.Map;


/**
 * Maps card names to their corresponding image file paths.
 * <p>
 * This class provides a static mapping from a card's name (as used in the game) to the relative
 * file path of its image.
 * </p>
 */
public class CardImageMapper {
        private static final Map<String, String> CardsImageMap = new HashMap<>();

        static {
            CardsImageMap.put("Again", "/images/general/cards/Again.png");
            CardsImageMap.put("MoveI", "/images/general/cards/MoveI.png");
            CardsImageMap.put("MoveII", "/images/general/cards/MoveII.png");
            CardsImageMap.put("MoveIII", "/images/general/cards/MoveIII.png");
            CardsImageMap.put("TurnRight", "/images/general/cards/TurnRight.png");
            CardsImageMap.put("TurnLeft", "/images/general/cards/TurnLeft.png");
            CardsImageMap.put("UTurn", "/images/general/cards/UTurn.png");
            CardsImageMap.put("BackUp", "/images/general/cards/BackUp.png");
            CardsImageMap.put("PowerUp", "/images/general/cards/PowerUp.png");
            CardsImageMap.put("Worm", "/images/general/cards/Worm.png");
            CardsImageMap.put("Spam", "/images/general/cards/Spam.png");
            CardsImageMap.put("Virus", "/images/general/cards/Virus.png");
            CardsImageMap.put("Trojan", "/images/general/cards/Trojan.png");
            CardsImageMap.put("RearLaser", "/images/general/cards/RearLaser.png");
            CardsImageMap.put("AdminPrivilege", "/images/general/cards/AdminPrivilege.png");
            CardsImageMap.put("SpamBlocker", "/images/general/cards/SpamBlocker.png");
            CardsImageMap.put("MemorySwap", "/images/general/cards/MemorySwap.png");
            CardsImageMap.put("CardBackside", "/images/general/cards/CardBackside.png");
            CardsImageMap.put("UpgradePermanentBS", "/images/general/cards/UpgradePermanentBS.png");
            CardsImageMap.put("UpgradeTemporaryBS", "/images/general/cards/UpgradeTemporaryBS.png");
            CardsImageMap.put("AdminPrivilegeBS", "/images/general/cards/AdminPrivilegeBS.png");

        }
    /**
     * Returns the image path corresponding to the given card name.
     *
     * @param cardName the name of the card.
     * @return the relative image file path, or {@code null} if the card name is not mapped.
     */
    public static String getImagePath(String cardName) {
        return CardsImageMap.get(cardName);
    }

}
