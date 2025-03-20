package viewmodel;

import java.util.HashMap;
import java.util.Map;

public class UpgradeCardTypeMapper {
    private static final Map<String, String> mapping = new HashMap<>();

    static {
        mapping.put("RearLaser", "permanent");
        mapping.put("AdminPrivilege", "permanent");
        mapping.put("SpamBlocker", "temporary");
        mapping.put("MemorySwap", "temporary");
    }

    /**
     * Returns the type of the given upgrade card.
     * @param upgradeCardName the name of the upgrade card
     * @return "permanent" or "temporary", or null if not found.
     */
    public static String getType(String upgradeCardName) {
        return mapping.get(upgradeCardName);
    }

}
