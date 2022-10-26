package me.gypopo.autosellchests.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

public class ChestLocation {

    private boolean doubleChest;
    private Location location1;
    private Location location2;

    public ChestLocation(String loc) {
        this.doubleChest = loc.contains("|");
        this.location1 = new Location(Bukkit.getWorld(loc.split("\\|")[0].split(":")[0]), Double.parseDouble(loc.split("\\|")[0].split(":")[1]), Double.parseDouble(loc.split("\\|")[0].split(":")[2]), Double.parseDouble(loc.split("\\|")[0].split(":")[3]));
        if (this.doubleChest)
            this.location2 = new Location(Bukkit.getWorld(loc.split("\\|")[1].split(":")[0]), Double.parseDouble(loc.split("\\|")[1].split(":")[1]), Double.parseDouble(loc.split("\\|")[1].split(":")[2]), Double.parseDouble(loc.split("\\|")[1].split(":")[3]));
    }

    public ChestLocation(Location location) {
        this.doubleChest = false;
        this.location1 = location;
    }

    public ChestLocation(Location location1, Location location2) {
        this.doubleChest = true;
        this.location1 = location1;
        this.location2 = location2;
    }

    public boolean isDoubleChest() {
        return this.doubleChest;
    }

    public Location getLeftLocation() {
        return this.location1;
    }

    public Location getRightLocation() {
        return this.location2;
    }

    public void removeLocation(Location location) {
        this.doubleChest = false;
        if (this.location1.equals(location)) {
            this.location1 = location2;
        } else this.location2 = null;
    }

    public boolean isOneOf(Location location) {
        return this.location1.equals(location) || (this.doubleChest && this.location2.equals(location));
    }

    @Override
    public String toString() {
        return this.location1.getWorld().getName() + ":" + this.location1.getBlockX() + ":" + this.location1.getBlockY() + ":" + this.location1.getBlockZ() +
                (this.doubleChest ? "|" + this.location2.getWorld().getName() + ":" + this.location2.getBlockX() + ":" + this.location2.getBlockY() + ":" + this.location2.getBlockZ() : "");
    }

    public void addLocation(Location loc) {
        this.location2 = loc;
        this.doubleChest = true;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof ChestLocation) {
            ChestLocation loc = (ChestLocation) object;
            return this.isOneOf(loc.getLeftLocation()) || (loc.isDoubleChest() && this.isOneOf(loc.getRightLocation()) || (this.doubleChest && loc.isOneOf(this.location2)));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }
}