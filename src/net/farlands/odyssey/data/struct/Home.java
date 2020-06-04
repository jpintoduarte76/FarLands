package net.farlands.odyssey.data.struct;

import net.farlands.odyssey.util.FLUtils;
import net.farlands.odyssey.util.LocationWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;

public final class Home extends LocationWrapper {
    private String name;

    public Home(String name, UUID world, double x, double y, double z, float yaw, float pitch) {
        super(world, x, y, z, yaw, pitch);
        this.name = name;
    }

    public Home(String name, Location loc) {
        this(name, loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public Location getLocation() {
        return asLocation();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return ChatColor.GOLD + name + ": " + ChatColor.AQUA + FLUtils.toStringTruncated(x) + ", " +
                FLUtils.toStringTruncated(y) + ", " + FLUtils.toStringTruncated(z);
    }
}
