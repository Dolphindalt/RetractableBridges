package xyz.dcaron.bridges;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@AllArgsConstructor
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
    @Getter
    private final Optional<Slab.Type> slabType;

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

    public static Optional<Slab.Type> getSlabType(final BlockData data) {
        
        if (data instanceof Slab) {
            return Optional.of(((Slab) data).getType());
        }
        return Optional.empty();
    }

}
