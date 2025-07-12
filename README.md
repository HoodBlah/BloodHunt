# Bloodhunt

A Minecraft Forge mod for version 1.20.1 that creates visible particle paths to selected nearby entities.

## Features

- **Entity Selection GUI**: Press 'B' (configurable) to open a menu showing all nearby entities within range
- **Advanced Pathfinding**: Uses A* pathfinding to calculate the best path to the target
  - Supports paths through air blocks
  - Can path through waterlogged blocks
  - Falls back to direct path if no valid path is found
- **Blood Trail Effect**: Creates an animated particle trail that leads to the selected entity
  - Particles gradually spawn from player to target
  - Trail fades over time for a dynamic effect

## Installation

1. Install Minecraft Forge for version 1.20.1
2. Download the latest release of Bloodhunt
3. Place the .jar file in your Minecraft mods folder
4. Launch Minecraft with the Forge profile

## Usage

1. Press 'B' in-game to open the entity selector
2. Click on an entity from the list to select it
3. A blood trail will appear, leading you to the selected entity
4. The trail will persist until it reaches the target

## Configuration

- Key bindings can be configured in Minecraft's Controls menu under the "Bloodhunt" category
- The maximum tracking range is set to 50 blocks by default

## Development

### Building from Source

1. Clone the repository
2. Run `./gradlew build`
3. Find the built jar in `build/libs/`

### Dependencies

- Minecraft 1.20.1
- Forge 47.1.0+

## License

This project is licensed under the MIT License - see the LICENSE file for details. 