# Metalion's Experience Tweaks

A NeoForge mod for Minecraft Java 26.1.2 with a suite of experience-related tweaks designed for servers where XP is plentiful and progression should feel meaningful at any level.

- **Keep XP on death** — players respawn with their full experience level, progress bar and total XP intact. Opt-out per player via blacklist.
- **Direct mob XP** — experience from mobs goes straight to the killer instead of spawning orbs on the ground, keeping gameplay clean and fast.
- **Item-based enchanting** — replaces the XP cost at the enchantment table with a configurable item (e.g. diamonds). XP is never consumed when enchanting.
- **Per-player adaptive cooldown** — each enchantment table use raises the minimum level required for the next use, independently per button and per player. The cooldown uses a square-root curve so it stays meaningful at level 200, 500 or beyond without becoming punishing.

---

## Installation

1. Make sure you have [NeoForge](https://neoforged.net/) installed.
2. Place the mod's `.jar` file in the `mods` folder of your Minecraft installation.

---

## Configuration

The mod generates a configuration file at `config/experiencetweaks-common.toml` after the first run.

| Key | Default | Description |
|-----|---------|-------------|
| `blacklistedPlayers` | `[]` | Player names who do NOT want these tweaks applied. |
| `directExperience` | `true` | If `true`, mob XP is given directly to the player instead of dropping as orbs. |
| `enchantmentCostItem` | `""` | Item consumed when enchanting (registry name, e.g. `minecraft:diamond`). Leave empty to use lapis lazuli. |
| `enchantmentCostMultiplier` | `0.1` | Multiplier for item cost based on enchantment level. Example: level 30 × 0.1 = 3 items. |
| `enchantmentBaseRequiredLevels` | `[10, 15, 20]` | Minimum player level required for each enchantment button (1, 2, 3) on first use. |
| `enchantmentRequiredLevelBias` | `0.5` | Cooldown difficulty weight. Range: `0.0` (minimum) to `1.0` (maximum). See details below. |

---

<details>
<summary><strong>Enchantment Cooldown System — How It Works</strong></summary>

<br>

Each enchantment table button has an **independent per-player cooldown** stored server-side and persisted across sessions.

### First Use

When a player opens the enchantment table for the first time (no history yet), the required level for each button is:

```
requiredLevel[button] = max(configMin[button], ceil(currentPlayerLevel × buttonIndex / 3))
```

Where `buttonIndex` is 1, 2, or 3. This means:

- If the player is **level 9** and the config says `[10, 15, 20]`, the minimums apply as-is: **10 / 15 / 20**.
- If the player is **level 50**, the first use requires: **17 / 34 / 50** (buttons 1, 2, 3).
- If the player is **level 100**, the first use requires: **34 / 67 / 100**.

The config minimums act as a floor — they only kick in for low-level players.

A minimum gap of 1 level is always enforced between consecutive buttons.

### After Each Use

After a successful enchantment on **any** button, all three buttons are recalculated independently using the square-root cooldown formula. A button's required level is **only ever raised**, never lowered.

**Formula (applied independently per button):**

```
increment    = max(1, ceil(buttonIndex × bias × 50 / √currentPlayerLevel))
nextRequired = currentPlayerLevel + increment
```

The constant `50` is an internal scaling factor. Using `√currentLevel` as the divisor (instead of `currentLevel`) ensures the cooldown stays meaningful at high levels (200, 500, 1000+), decaying gradually rather than collapsing to +1 early.

After computing all three increments independently, a minimum gap of 1 is enforced upward:
```
next[1] = max(next[1], next[0] + 1)
next[2] = max(next[2], next[1] + 1)
```

### Bias Parameter

`enchantmentRequiredLevelBias` is a `0.0–1.0` weight. The table below shows the increment added to the player's current level for **button 3** (hardest). Buttons 1 and 2 receive proportionally smaller increments.

| Level | bias 0.1 | bias 0.5 (default) | bias 1.0 |
|-------|---------|-------------------|---------|
| 10    | +5      | +24               | +48     |
| 50    | +2      | +11               | +21     |
| 100   | +2 (+1 gap) | +8           | +15     |
| 200   | +1 (gap) | +6              | +11     |
| 500   | +1 (gap) | +4              | +7      |
| 1000  | +1 (gap) | +3              | +5      |

> `(gap)` means the natural increment was 1 and the gap rule between buttons applies.
> With `bias=0.1` the system is essentially always at minimum spacing — use `0.3`–`0.7` for a noticeable but fair cooldown.

</details>

---

<details>
<summary><strong>Worked Example</strong></summary>

<br>

Config: `enchantmentBaseRequiredLevels = [10, 15, 20]`, `enchantmentRequiredLevelBias = 0.5`

**Player Leonardo_L opens the table for the first time at level 50:**

| Button | Calculation | Required Level |
|--------|------------|----------------|
| 1 | max(10, ceil(50 × 1/3)) = max(10, 17) | **17** |
| 2 | max(15, ceil(50 × 2/3)) = max(15, 34) | **34** |
| 3 | max(20, ceil(50 × 3/3)) = max(20, 50) | **50** |

**Leonardo_L uses button 3 (currently level 50):**

Formula: `ceil(buttonIndex × 0.5 × 50 / √50)` where `√50 ≈ 7.071`

```
Button 1: ceil(1 × 0.5 × 50 / 7.071) = ceil(3.54)  = 4   →  next = 54
Button 2: ceil(2 × 0.5 × 50 / 7.071) = ceil(7.07)  = 8   →  next = 58
Button 3: ceil(3 × 0.5 × 50 / 7.071) = ceil(10.61) = 11  →  next = 61
```

Gap check: 58 ≥ 54+1 ✓, 61 ≥ 58+1 ✓ — no correction needed.

**Now at level 200, uses button 1:**

Formula: `ceil(buttonIndex × 0.5 × 50 / √200)` where `√200 ≈ 14.14`

```
Button 1: ceil(1 × 0.5 × 50 / 14.14) = ceil(1.77) = 2  →  next = 202
Button 2: ceil(2 × 0.5 × 50 / 14.14) = ceil(3.54) = 4  →  next = 204
Button 3: ceil(3 × 0.5 × 50 / 14.14) = ceil(5.30) = 6  →  next = 206
```

At level 200, cooldowns are shorter in absolute levels — but each level at this height represents significantly more raw XP than at level 50, keeping the cost meaningful.

</details>

---

<details>
<summary><strong>Technical Details</strong></summary>

<br>

### Persistence

Per-player cooldown data is stored using Minecraft's `SavedData` system, serialised via Mojang's `Codec` API, and saved in the world's `data/` folder as `experiencetweaks_enchant_data.dat`. Data persists across server restarts and world reloads.

### Client Sync

The server sends the player's current required levels to their client via a custom `SyncEnchantLevelsPacket` whenever:

- The player opens the enchantment table menu.
- A successful enchantment is performed.

The client caches these values in `ClientEnchantLevelCache` and uses them to render the button labels and tooltip correctly.

### Mixins

| Mixin | Purpose |
|-------|---------|
| `EnchantmentMenuMixin` | Intercepts button clicks to validate per-player level requirements and item costs; updates cooldown data after a successful enchant. |
| `EnchantmentScreenMixin` | Overrides button rendering to display the per-player required level (instead of the vanilla level) and adjusts affordability highlighting. |
| `EnchantmentMenuCurrencySlotMixin` | Allows the configured item (not just lapis) to be placed in the enchantment table's currency slot. |

### Blacklist

Players listed in `blacklistedPlayers` are fully exempt from all tweaks: they keep vanilla enchantment behaviour, vanilla XP on death, and vanilla orb drops.

</details>

---

## Credits

- **Author:** Leonardo K
- **Version:** 1.0.0
- **Loader:** NeoForge 26.1.2

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
