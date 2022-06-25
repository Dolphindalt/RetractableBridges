package xyz.dcaron.bridges;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
public class Bridge {

    @Getter
    private final String worldName;
    @Getter
    @Setter
    private int x, z;
    @Getter
    private final int y, width, height;
    @Getter
    private final Material type;
    @Getter
    @Setter
    private Set<BlockFace> blockedDirections;

    public Bridge(final String worldName, final int x, final int z, final int y, final int width, final int height,
            final Material type, final Set<BlockFace> blockedDirections) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.blockedDirections = blockedDirections;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("x: ").append(x);
        sb.append(" z: ").append(z);
        sb.append(" y: ").append(y);
        sb.append(" width: ").append(width);
        sb.append(" height: ").append(height);
        sb.append(" material: ").append(type);
        sb.append(" directions blocked: ").append(blockedDirections.toString());
        return sb.toString();
    }

}
