package me.gypopo.autosellchests.objects;

import org.bukkit.Bukkit;

import java.util.Objects;

public class Location {

    public final String world; // Potentially store the world object instead of name for faster access to prevent Bukkit.getWorld() calls?
    public final int x;
    public final int y;
    public final int z;

    public Location(org.bukkit.Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
    }

    public Location(String location) {
        this.world = location.split(":")[0];
        this.x = Integer.parseInt(location.split(":")[1]);
        this.y = Integer.parseInt(location.split(":")[2]);
        this.z = Integer.parseInt(location.split(":")[3]);
    }

    public org.bukkit.Location toLoc() {
        return new org.bukkit.Location(Bukkit.getWorld(this.world), this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return this.world + ":" + this.x + ":" + this.y + ":" + this.z;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof Location loc)
            return this.world.equals(loc.world) && this.x == loc.x && this.y == loc.y && this.z == loc.z;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.world, this.x, this.y, this.z);
    }
}
