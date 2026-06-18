package com.ziondev.experiencetweaks.network;

import java.util.List;

/**
 * Client-side cache of the per-player required levels received from the server.
 * Updated whenever a {@link SyncEnchantLevelsPacket} arrives.
 */
public final class ClientEnchantLevelCache {

    private static int[] cachedLevels = new int[]{10, 15, 20};

    private ClientEnchantLevelCache() {}

    public static void update(List<Integer> levels) {
        int[] updated = new int[levels.size()];
        for (int i = 0; i < levels.size(); i++) {
            updated[i] = levels.get(i);
        }
        cachedLevels = updated;
    }

    /**
     * Returns the required level for the given 0-based button index.
     * Falls back to a reasonable default if the cache has not been populated yet.
     */
    public static int getRequiredLevel(int buttonId) {
        if (buttonId >= 0 && buttonId < cachedLevels.length) {
            return cachedLevels[buttonId];
        }
        return (buttonId + 1) * 10;
    }
}
