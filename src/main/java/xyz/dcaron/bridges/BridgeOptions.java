package xyz.dcaron.bridges;

import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

public class BridgeOptions {

    @Getter
    private final boolean moveEntitiesOnBridge;
    @Getter
    private final int ticksPerBridgeMovement;
    @Getter
    private final Set<Material> bridgeMaterials;
    @Getter
    private final int maximumMultiplePowerBoost;
    @Getter
    private final boolean allowFloatingBridges;
    @Getter
    private final Set<Material> bridgePowerBlocks;
    @Getter
    private final boolean allPowerBlocksAllowed;

    public BridgeOptions(final FileConfiguration configuration) {

        moveEntitiesOnBridge = configuration.getBoolean("moveEntitiesOnBridge");

        ticksPerBridgeMovement = configuration.getInt("ticksPerBridgeMovement");

        bridgeMaterials = configuration.getStringList("bridgeMaterials")
                .stream()
                .map(material -> Material.getMaterial(material))
                .collect(Collectors.toUnmodifiableSet());

        maximumMultiplePowerBoost = configuration.getInt("maximumMultiplePowerBoost");

        allowFloatingBridges = configuration.getBoolean("allowFloatingBridges");

        bridgePowerBlocks = configuration.getStringList("bridgePowerBlocks")
                .stream()
                .map(material -> Material.getMaterial(material))
                .collect(Collectors.toUnmodifiableSet());

        allPowerBlocksAllowed = bridgePowerBlocks.isEmpty();
    }

    public String getOptionsPrintable() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("moveEntitiesOnBridge: ").append(moveEntitiesOnBridge).append("\n");
        sb.append("ticksPerBridgeMovement: ").append(ticksPerBridgeMovement).append("\n");
        sb.append("bridgeMaterials: ").append(bridgeMaterials.toString()).append("\n");
        sb.append("maximumMultiplePowerBoost: ").append(maximumMultiplePowerBoost).append("\n");
        sb.append("allowFloatingBridges: ").append(allowFloatingBridges).append("\n");
        sb.append("bridgePowerBlocks: ").append(bridgePowerBlocks.toString()).append("\n");
        sb.append("allPowerBlocksAllowed: ").append(allPowerBlocksAllowed);
        return sb.toString();
    }

}
