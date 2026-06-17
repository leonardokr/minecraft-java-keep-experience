# Metalion's Experience Tweaks

A minimalist mod for Minecraft Java 26.1.2 (NeoForge) that allows players to keep their accumulated experience upon death.

### Features
- Keeps experience level and progress bar after death.
- Removes player experience orb drops on death to prevent duplication.

### Installation
1. Make sure you have [NeoForge](https://neoforged.net/) installed.
2. Place the mod's `.jar` file in the `mods` folder of your Minecraft installation.

### Configuration
The mod generates a configuration file at `config/experiencetweaks-common.toml` after the first run.

- **blacklistedPlayers**: A list of player names who do NOT want to keep their experience.
  - Example: `blacklistedPlayers = ["Player1", "Player2"]`

### Credits
- **Author:** Leonardo K
- **Version:** 1.0.0
- **Loader:** NeoForge 26.1.2

### License
This project is licensed under the MIT License. See the `LICENSE` file for details.