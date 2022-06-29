package xyz.dcaron.bridges;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class BridgeBlockListener implements Listener {

    private final BridgeOptions options;

    private final BlockFace[] searchDirections = {
            BlockFace.UP,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH
    };

    private final Set<BridgeMover> bridgeMovers = new HashSet<BridgeMover>();

    public BridgeBlockListener(final BridgeOptions options) {
        this.options = options;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRedstoneBlockChange(final BlockRedstoneEvent event) {
        final Block block = event.getBlock();
        final boolean isPowerOn = event.getOldCurrent() == 0;

        if (!this.isPowerOnOrOffEvent(event)) {
            return;
        }

        for (final BlockFace direction : this.searchDirections) {
            this.findBridge(block, direction).ifPresent((bridge) -> {
                BridgesPlugin.log("Bridge found " + bridge.toString(), Level.FINE);

                Set<BlockFace> blockedDirections = bridge.getBlockedDirections();

                if (isPowerOn) {
                    if (blockedDirections.size() == 3) {
                        if (!blockedDirections.contains(BlockFace.SOUTH)) {
                            move(bridge, BlockFace.SOUTH);
                        } else if (!blockedDirections.contains(BlockFace.EAST)) {
                            move(bridge, BlockFace.EAST);
                        }
                    } else {
                        if (blockedDirections.contains(BlockFace.NORTH) && 
                            blockedDirections.contains(BlockFace.SOUTH)) {
                                move(bridge, BlockFace.EAST);
                            } else {
                                move(bridge, BlockFace.SOUTH);
                            }
                    }
                } else {
                    if (blockedDirections.size() == 3) {
                        if (!blockedDirections.contains(BlockFace.NORTH)) {
                            move(bridge, BlockFace.NORTH);
                        } else if (!blockedDirections.contains(BlockFace.WEST)) {
                            move(bridge, BlockFace.WEST);
                        }
                    } else {
                        if (blockedDirections.contains(BlockFace.NORTH) && 
                            blockedDirections.contains(BlockFace.SOUTH)) {
                                move(bridge, BlockFace.WEST);
                            } else {
                                move(bridge, BlockFace.NORTH);
                            }
                    }
                }
                
            });
        }

    }

    private boolean materialWithSlabToBlockEquals(final Block blockToCompare, Material material, Optional<Slab.Type> slabType) {
        return blockToCompare.getType().equals(material) && Bridge.getSlabType(blockToCompare.getBlockData()).equals(slabType);
    }

    /**
     * Find a bridge above the adjacent block. A bridge is a rectangular area of 
     * slabs or double slabs, parallel to the ground. It can have no holes or bits 
     * sticking out, and it must be contrained on three sides, or on two opposite 
     * sides.
     * 
     * @param block Origin block
     * @param direction Direction of adjacent block
     * @return Some Bridge or None
     */
    private Optional<Bridge> findBridge(final Block block, final BlockFace direction) {
        final Block targetBlock = block.getRelative(direction);

        if (!isBlockBridgePowerBlock(targetBlock.getType())) {
            BridgesPlugin.log("Block is not bridge power block", Level.FINE);
            return Optional.empty();
        }

        final Block aboveBlock = targetBlock.getRelative(BlockFace.UP);
        if (isPotentialBridgeBlock(aboveBlock.getType())) {

            int x = aboveBlock.getX();
            int y = aboveBlock.getY();
            int z = aboveBlock.getZ();
            int width = 1, height = 1;

            final Material material = aboveBlock.getType();

            // Record the slab orientation if the block is a slab.
            final Optional<Slab.Type> slabType = Bridge.getSlabType(aboveBlock.getBlockData());

            Block adjacentBlock = aboveBlock.getRelative(BlockFace.WEST);
            while (materialWithSlabToBlockEquals(adjacentBlock, material, slabType)) {
                x--;
                width++;
                adjacentBlock = adjacentBlock.getRelative(BlockFace.WEST);
            }

            adjacentBlock = aboveBlock.getRelative(BlockFace.NORTH);
            while (materialWithSlabToBlockEquals(adjacentBlock, material, slabType)) {
                z--;
                height++;
                adjacentBlock = adjacentBlock.getRelative(BlockFace.NORTH);
            }

            adjacentBlock = aboveBlock.getRelative(BlockFace.EAST);
            while (materialWithSlabToBlockEquals(adjacentBlock, material, slabType)) {
                width++;
                adjacentBlock = adjacentBlock.getRelative(BlockFace.EAST);
            }

            adjacentBlock = aboveBlock.getRelative(BlockFace.SOUTH);
            while (materialWithSlabToBlockEquals(adjacentBlock, material, slabType)) {
                height++;
                adjacentBlock = adjacentBlock.getRelative(BlockFace.SOUTH);
            }

            if (width >= 2 && height >= 2) {
                // Bridges must be completely filled with no holes and 
                // have no material obtruding it's square form.
                final World world = targetBlock.getWorld();
                Set<BlockFace> blockedDirections = EnumSet.noneOf(BlockFace.class);

                for (int dz = 0; dz < height; dz++) {
                    Block edgeBlock = world.getBlockAt(x - 1, y, z + dz);
                    if (materialWithSlabToBlockEquals(edgeBlock, material, slabType)) {
                        // A block is sticking out.
                        return Optional.empty();
                    } else if (!edgeBlock.getType().isAir()) {
                        blockedDirections.add(BlockFace.WEST);
                    }

                    edgeBlock = world.getBlockAt(x + width, y, z + dz);
                    if (materialWithSlabToBlockEquals(edgeBlock, material, slabType)) {
                        // A block is sticking out.
                        return Optional.empty();
                    } else if (!edgeBlock.getType().isAir()) {
                        blockedDirections.add(BlockFace.EAST);
                    }
                }

                for (int dx = 0; dx < width; dx++) {
                    Block edgeBlock = world.getBlockAt(x + dx, y, z - 1);
                    if (materialWithSlabToBlockEquals(edgeBlock, material, slabType)) {
                        // A block is sticking out.
                        return Optional.empty();
                    } else if (!edgeBlock.getType().isAir()) {
                        blockedDirections.add(BlockFace.NORTH);
                    }

                    for (int dz = 0; dz < height; dz++) {
                        final Block bridgeBlock = world.getBlockAt(x + dx, y, z + dz);
                        if (!materialWithSlabToBlockEquals(bridgeBlock, material, slabType)) {
                            // There is a hole in the bridge.
                            return Optional.empty();
                        }
                    }

                    edgeBlock = world.getBlockAt(x + dx, y, z + height);
                    if (materialWithSlabToBlockEquals(edgeBlock, material, slabType)) {
                        // A block is sticking out.
                        return Optional.empty();
                    } else if (!edgeBlock.getType().isAir()) {
                        blockedDirections.add(BlockFace.SOUTH);
                    }
                }

                // The bridge is a proper rectangle.
                if (this.blockedInThreeDirections(blockedDirections) || 
                    this.opposingDirectionsAreBlocked(blockedDirections)) {
                        return Optional.of(new Bridge(world.getName(), x, z, y, width, height, material, blockedDirections, slabType));
                }
            }

        }

        return Optional.empty();
    }

    private void move(final Bridge bridge, final BlockFace direction) {
        this.bridgeMovers.stream()
            .filter(potentialMover -> potentialMover.getBridge().equals(bridge))
            .findFirst()
            .ifPresentOrElse(
                mover -> {
                    // The bridge material may have changed since it was last moved.
                    mover.setBridge(bridge);
                    mover.move(direction);
                }, 
                () -> {
                    BridgeMover mover = new BridgeMover(bridge, this.options);
                    this.bridgeMovers.add(mover);
                    mover.move(direction);
                }
            );
    }

    private boolean blockedInThreeDirections(final Set<BlockFace> blockedDirections) {
        return blockedDirections.size() == 3;
    }

    private boolean opposingDirectionsAreBlocked(final Set<BlockFace> blockedDirections) {
        return blockedDirections.size() == 2 &&
            ((blockedDirections.contains(BlockFace.NORTH) && blockedDirections.contains(BlockFace.SOUTH)) ||
                (blockedDirections.contains(BlockFace.EAST) && blockedDirections.contains(BlockFace.WEST)));
    }

    private boolean isPotentialBridgeBlock(final Material material) {
        return this.options.getBridgeMaterials().contains(material);
    }

    private boolean isBlockBridgePowerBlock(final Material material) {
        return this.options.isAllPowerBlocksAllowed() || this.options.getBridgePowerBlocks().contains(material);
    }

    private boolean isPowerOnOrOffEvent(final BlockRedstoneEvent event) {
        return event.getOldCurrent() == 0 || !(event.getNewCurrent() == 0);
    }

}
