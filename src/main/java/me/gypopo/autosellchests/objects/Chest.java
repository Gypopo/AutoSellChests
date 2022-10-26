package me.gypopo.autosellchests.objects;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Chest {

    private final int id;
    private final ChestLocation location;
    private final UUID owner;
    private int itemsSold;
    private double income;
    private long nextInterval;

    public Chest(int id, String location, String owner, int itemsSold, double income) {
        this.id = id;
        this.location = new ChestLocation(location);
        this.owner = UUID.fromString(owner);
        this.itemsSold = itemsSold;
        this.income = income;
    }

    public Chest(int id, ChestLocation location, Player owner, int itemsSold, double income) {
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

    public ChestLocation getLocation() {
        return this.location;
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
