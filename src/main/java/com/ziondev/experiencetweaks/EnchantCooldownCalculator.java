package com.ziondev.experiencetweaks;

/**
 * Pure stateless calculator for enchantment table cooldown levels.
 * Contains no Minecraft dependencies so it can be unit-tested without
 * loading the game.
 */
public final class EnchantCooldownCalculator {

    public static final int BUTTON_COUNT = 3;

    /**
     * Scaling constant that defines the "base difficulty" of the curve.
     * With bias=0.5 and this constant, the increment for button 3 at level 100
     * is roughly 8, and at level 500 is roughly 3 — meaningful at all levels.
     */
    static final double SCALE_CONSTANT = 50.0;

    private EnchantCooldownCalculator() {}

    /**
     * Computes the next required levels for all three enchantment buttons after
     * a successful enchantment.
     *
     * <p><b>Step 1 — independent increment per button (square-root curve):</b>
     * <pre>  increment = max(1, ceil(buttonLevel × bias × C / √currentLevel))</pre>
     * where {@code C = 50} is an internal scaling constant, and {@code bias} is
     * a 0.0–1.0 weight configured by the server operator.
     *
     * <p>Using {@code √currentLevel} as the divisor (instead of {@code currentLevel})
     * makes the cooldown decay much more slowly at high levels, keeping the system
     * meaningful for players with hundreds of levels.
     *
     * <p><b>Step 2 — enforce minimum gap of 1 between consecutive buttons:</b>
     * <pre>  next[1] = max(next[1], next[0] + 1)
     *  next[2] = max(next[2], next[1] + 1)</pre>
     *
     * <p>This guarantees:
     * <ul>
     *   <li>Every button always requires at least currentLevel + 1.</li>
     *   <li>Button 2 always requires at least button 1 + 1.</li>
     *   <li>Button 3 always requires at least button 2 + 1.</li>
     *   <li>If the natural gap is already &gt; 1, it is preserved.</li>
     * </ul>
     *
     * @param currentLevel player's experience level at enchant time
     * @param bias         0.0–1.0 difficulty weight from config
     * @return int[3] — next required levels for buttons 0, 1, 2 (0-indexed)
     */
    public static int[] computeNextLevels(int currentLevel, double bias) {
        int[] next = new int[BUTTON_COUNT];

        for (int b = 0; b < BUTTON_COUNT; b++) {
            int buttonLevel = b + 1; // 1-based: 1, 2, 3
            int increment;
            if (currentLevel <= 0) {
                increment = 1;
            } else {
                double sqrtLevel = Math.sqrt(currentLevel);
                increment = (int) Math.ceil((buttonLevel * bias * SCALE_CONSTANT) / sqrtLevel);
            }
            increment = Math.max(1, increment);
            next[b] = currentLevel + increment;
        }
        for (int b = 1; b < BUTTON_COUNT; b++) {
            if (next[b] < next[b - 1] + 1) {
                next[b] = next[b - 1] + 1;
            }
        }

        return next;
    }

    /**
     * Computes the baseline required levels for a player's first use of the
     * enchantment table (no saved cooldown history yet).
     *
     * <p>Each button's required level is:
     * <pre>  max(configMin[b], ceil(currentLevel × (b+1) / 3))</pre>
     *
     * <p>After computing each independently, the minimum-gap rule is also applied
     * so that button ordering is always consistent.
     *
     * @param currentLevel   player's current experience level
     * @param configMinLevels int[3] — configured base minimums for each button
     * @return int[3] — first-use required levels for buttons 0, 1, 2
     */
    public static int[] computeFirstUseLevels(int currentLevel, int[] configMinLevels) {
        int[] levels = new int[BUTTON_COUNT];
        for (int b = 0; b < BUTTON_COUNT; b++) {
            int configMin = configMinLevels[b];
            int playerBased = (int) Math.ceil((double) currentLevel * (b + 1) / BUTTON_COUNT);
            levels[b] = Math.max(configMin, playerBased);
        }
        for (int b = 1; b < BUTTON_COUNT; b++) {
            if (levels[b] < levels[b - 1] + 1) {
                levels[b] = levels[b - 1] + 1;
            }
        }
        return levels;
    }
}
