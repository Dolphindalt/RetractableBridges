# Retractable Bridges

Build redstone powered retractable bridges, without using commands!

## Features

- It does not use commands. Just build a bridge and it works.
- Stateless, i.e. does not have to save anything on the server.
- It's redstone powered, giving you flexibility in controlling it.
- It is realistic. The bridge does not disappear, but it slides in and out just like a real one would.
- Holds back water or lava.
- Configurable speed and applying more or less redstone power.

## Usage

- Build the bridge itself, out of slab blocks or bridge blocks set in the configuration.
- Make sure there is at least one block adjacent to the bridge on three sides so that it can't move in that direction.
- For the direction in which you want it to move, make sure there is a block to stop at the distance you want it to move to.
- Power one of the blocks underneath the bridge with redstone. Please note: you are powering a block underneath the bridge, not the bridge itself. For the purposes of this plugin, just running a redstone wire undernearth the block will power it.
- If you power more than one block, the bridge will move faster.
- Make sure that when a bridge is fully extended, it is still in contact with the redstone power underneath it.
- When the block recieves redstone power, the bridge will move to the south or east. When the block loses redstone power, it will move to the north or west.
- Here is an example. ![Bridge Example GIF](bridge_example.gif)

## Configuration

See the `config.yml` folder for a description of the configurable options.

## Feedback

This plugin is a fork of Captain Chao's Rectractable Bridges updated to work with modern Minecraft. Please leave an issue on Github if there are any bugs with this new adaption.