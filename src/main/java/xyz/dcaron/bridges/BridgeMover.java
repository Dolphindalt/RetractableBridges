package xyz.dcaron.bridges;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitScheduler;

import lombok.Getter;
import lombok.Setter;

public class BridgeMover implements Runnable {

    @Getter
    @Setter
    private Bridge bridge;
    private final BridgeOptions options;

    private BlockFace direction = null;
    private int taskId = 0, boosts, movingDelay;

    public BridgeMover(final Bridge bridge, final BridgeOptions options) {
        this.bridge = bridge;
        this.options = options;
    }

    public void move(final BlockFace direction) {
        if (direction != this.direction) {
            this.direction = direction;
            this.movingDelay = this.options.getTicksPerBridgeMovement();
        } else if (this.boosts < this.options.getMaximumMultiplePowerBoost()) {
            this.movingDelay /= 2;
            if (this.movingDelay < 1) {
                this.movingDelay = 1;
            }
            this.boosts++;
        }

        BukkitScheduler scheduler = BridgesPlugin.getPlugin().getServer().getScheduler();
        if (this.taskId != 0) {
            scheduler.cancelTask(this.taskId);
        }
        this.taskId = scheduler.scheduleSyncRepeatingTask(BridgesPlugin.getPlugin(), 
            this, Math.max(this.movingDelay / 2, 1), this.movingDelay);
    }

    @Override
    public void run() {
        if (!tryMoveBridge()) {
            BridgesPlugin.getPlugin().getServer().getScheduler().cancelTask(this.taskId);
            this.taskId = 0;
            this.direction = null;
        }
    }

    private boolean tryMoveBridge() {
        final World world = BridgesPlugin.getPlugin().getServer().getWorld(bridge.getWorldName());
        if (world == null) {
            BridgesPlugin.log("World of name " + bridge.getWorldName() + " is missing!", Level.FINE);
            return false;
        }

        Set<Point> chunkCoordinates = getChunkCoords(bridge.getX(), bridge.getZ(), bridge.getWidth(), bridge.getHeight());
        if (!this.areAllChunksLoaded(world, chunkCoordinates)) {
            BridgesPlugin.log("Chunks not loaded, cancelling bridge move!", Level.FINE);
            return false;
        }

        if (!this.isBridgeWhole(world)) {
            BridgesPlugin.log("Bridge is no longer valid, cancelling bridge move!", Level.FINE);
            return false;
        }

        if (!this.tryToMove(direction, world, chunkCoordinates)) {
            return false;
        }

        return true;
    }

    private boolean bridgeFloatingChecks(final int dx, final int dz, final World world) {
        boolean floatingOnAir = true;
        final int ddx = (int) Math.signum(dx);
        final int ddz = (int) Math.signum(dz);
        for (int x = bridge.getX() + ddx; x < bridge.getX() + ddx + bridge.getWidth(); x++) {
            for (int z = bridge.getZ() + ddz; z < bridge.getZ() + ddz + bridge.getHeight(); z++) {
                final Material materialBeneathBridge = world.getBlockAt(x, bridge.getY() - 1, z).getType();

                if (!materialBeneathBridge.isAir()) {
                    floatingOnAir = false;
                }
            }
        }

        if (floatingOnAir && !this.options.isAllowFloatingBridges()) {
            BridgesPlugin.log("No solid block beneath bridge and floating not allowed", Level.FINE);
            return false;
        }

        return true;
    }

    private boolean tryToMove(final BlockFace direction, final World world, final Set<Point> chunksLoaded) {
        int length, dx = 0, dz = 0, xMult = 0, zMult = 0;
        switch (direction) {
            case WEST:
                length = bridge.getHeight();
                dx = -1;
                zMult = 1;
                break;
            case NORTH:
                length = bridge.getWidth();
                dz = -1;
                xMult = 1;
                break;
            case EAST:
                length = bridge.getHeight();
                dx = bridge.getWidth();
                zMult = 1;
                break;
            case SOUTH:
                length = bridge.getWidth();
                dz = bridge.getHeight();
                xMult = 1;
                break;
            default:
                throw new AssertionError(direction);
        }

        for (int i = 0; i < length; i++) {
            final Material blockType = world.getBlockAt(bridge.getX() + dx + i * xMult, bridge.getY(), bridge.getZ() + dz + i * zMult).getType();
            if (!blockType.isAir()) {
                BridgesPlugin.log("Not enough room besides bridge; blocked by " + blockType.toString(), Level.FINE);
                return false;
            }
        }

        if (!bridgeFloatingChecks(dx, dz, world)) {
            return false;
        }

        // Its bridge moving time.
        BridgesPlugin.log("Moving bridge " + direction + " one row", Level.FINE);
        for (int i = 0; i < length; i++) {
            world.getBlockAt(bridge.getX() + dx + i * xMult, bridge.getY(), bridge.getZ() + dz + i * zMult).setType(bridge.getType());
        }

        // Moves entities standing on the bridge.
        tryMoveEntities(world, chunksLoaded);

        if (dx == -1) {
            dx = bridge.getWidth() - 1;
        } else if (dx == bridge.getWidth()) {
            dx = 0;
        } else if (dz == -1) {
            dz = bridge.getHeight() - 1;
        } else {
            dz = 0;
        }

        for (int i = 0; i < length; i++) {
            // Set block behind to air.
            world.getBlockAt(bridge.getX() + dx + i * xMult, bridge.getY(), bridge.getZ() + dz + i * zMult).setType(Material.AIR);
        }

        bridge.setX(bridge.getX() + direction.getModX());
        bridge.setZ(bridge.getZ() + direction.getModZ());

        return true;
    }

    private void tryMoveEntities(final World world, final Set<Point> chunksLoaded) {
        if (this.options.isMoveEntitiesOnBridge()) {
            chunksLoaded.stream()
                .map(point -> world.getChunkAt(point.x, point.y))
                .map(chunk -> Arrays.asList(chunk.getEntities()))
                .flatMap(Collection::stream)
                .forEach(entity -> {
                    if (this.isOnBridge(entity.getLocation()) && isSpaceToMove(world, entity, entity.getLocation())) {
                        final Location location = entity.getLocation();
                        location.setX(entity.getLocation().getX() + direction.getModX());
                        location.setZ(entity.getLocation().getZ() + direction.getModZ());
                        entity.teleport(location);
                    }
                });
        }
    }

    private boolean isSpaceToMove(final World world, final Entity entity, final Location location) {
        final int newBlockX = location.getBlockX() + direction.getModX();
        final int newBlockY = location.getBlockY() + direction.getModY() + ((this.materialIsBottomSlab(world.getBlockAt(location))) ? 1 : 0);
        final int newBlockZ = location.getBlockZ() + direction.getModZ();

        if (!world.getBlockAt(newBlockX, newBlockY, newBlockZ).getType().isAir()) {
            return false;
        }

        if (entity instanceof LivingEntity && ((LivingEntity) entity).getEyeHeight(true) > 1.0) {
            // Check the block above for double high entities.
            if (!world.getBlockAt(newBlockX, newBlockY + 1, newBlockZ).getType().isAir()) {
                return false;
            }
        }

        return true;
    }

    private boolean materialIsBottomSlab(final Block block) {
        final BlockData data = block.getBlockData();
        return (data instanceof Slab && ((Slab) data).getType().equals(Slab.Type.BOTTOM));
    }

    private boolean isOnBridge(final Location location) {
        final float dy = (materialIsBottomSlab(location.getBlock().getRelative(BlockFace.SOUTH))) ? 0.5f : 1.0f;
        return location.getX() >= bridge.getX() && location.getX() < bridge.getX() + bridge.getWidth() &&
            location.getZ() >= bridge.getZ() && location.getZ() < bridge.getZ() + bridge.getHeight() && 
            location.getY() >= bridge.getY() + dy && location.getY() < bridge.getY() + dy + 0.25;
    }

    private boolean isBridgeWhole(final World world) {
        int bridgeX1 = bridge.getX(), bridgeX2 = bridgeX1 + bridge.getWidth();
        int bridgeZ1 = bridge.getZ(), bridgeZ2 = bridgeZ1 + bridge.getHeight();
        final Material bridgeMaterial = bridge.getType();

        for (int x = bridgeX1; x < bridgeX2; x++) {
            for (int z = bridgeZ1; z < bridgeZ2; z++) {
                final Block block = world.getBlockAt(x, bridge.getY(), z);
                if (!block.getType().equals(bridgeMaterial)) {
                    BridgesPlugin.log("Bridge is no longer valid, cancelling bridge move!", Level.FINE);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean areAllChunksLoaded(final World world, final Set<Point> chunkCoords) {
        return !chunkCoords.stream()
            .anyMatch(point -> !world.isChunkLoaded(point.x, point.y));
    }

    private Set<Point> getChunkCoords(int x, int z, int width, int height) {
        switch (direction) {
            case WEST:
                x--;
                width++;
                break;
            case NORTH:
                z--;
                height++;
                break;
            case EAST:
                width++;
                break;
            case SOUTH:
                height++;
                break;
            default:
                break;
        }

        Set<Point> chunkCoords = new HashSet<Point>();
        for (int u = x; u <= (x + width - 1); u += 16) {
            for (int v = z; v <= (z + height - 1); v += 16) {
                chunkCoords.add(new Point(u >> 4, v >> 4));
            }
            chunkCoords.add(new Point(u >> 4, (z + height - 1) >> 4));
        }
        chunkCoords.add(new Point((x + width - 1) >> 4, (z + height - 1) >> 4));

        return chunkCoords;
    }
    
}
