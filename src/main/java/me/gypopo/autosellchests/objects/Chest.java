package me.gypopo.autosellchests.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Chest {

    private final int id;
    private final Location location;
    private final UUID owner;
    private int itemsSold;
    private double income;
    private long nextInterval;

    public Chest(int id, String location, String owner, int itemsSold, double income) {
        this.id = id;
        this.location = new Location(Bukkit.getWorld(location.split(":")[0]), Double.parseDouble(location.split(":")[1]), Double.parseDouble(location.split(":")[2]), Double.parseDouble(location.split(":")[3]));
        this.owner = UUID.fromString(owner);
        this.itemsSold = itemsSold;
        this.income = income;
    }

    public Chest(int id, Location location, Player owner, int itemsSold, double income) {
        this.id = id;
        this.location = location;
        this.owner = owner.getUniqueId();
        this.itemsSold = itemsSold;
        this.income = income;
    }

    public void addItemsSold(int itemsSold) {
        this.itemsSold += itemsSold;
    }

    public void addIncome(double income) {
        this.income += income;
    }

    public void setNextInterval(long nextInterval) {
        this.nextInterval = nextInterval;
    }

    public int getId() {
        return this.id;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public double getIncome() {
        return income;
    }

    public long getNextInterval() {
        return this.nextInterval;
    }
}
