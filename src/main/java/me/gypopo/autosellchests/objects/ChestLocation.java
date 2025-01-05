package me.gypopo.autosellchests.objects;

public class ChestLocation {

    private boolean doubleChest;
    private Location location1;
    private Location location2;

    public ChestLocation(String loc) {
        this.doubleChest = loc.contains("|");
        this.location1 = new Location(loc.split("\\|")[0]);
        if (this.doubleChest)
            this.location2 = new Location(loc.split("\\|")[1]);
    }

    public ChestLocation(org.bukkit.Location location) {
        this.doubleChest = false;
        this.location1 = new Location(location);
    }

    public ChestLocation(org.bukkit.Location location1, org.bukkit.Location location2) {
        this.doubleChest = true;
        this.location1 = new Location(location1);
        this.location2 = new Location(location2);
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
            this.location1 = this.location2;
        } else this.location2 = null;
    }

    public boolean isOneOf(Location location) {
        return this.location1.equals(location) || (this.doubleChest && this.location2.equals(location));
    }

    @Override
    public String toString() {
        return this.location1.toString() + (this.doubleChest ? "|" + this.location2.toString() : "");
    }

    public void addLocation(org.bukkit.Location loc) {
        this.location2 = new Location(loc);
        this.doubleChest = true;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof ChestLocation loc)
            return this.isOneOf(loc.getLeftLocation()) || (loc.isDoubleChest() && this.isOneOf(loc.getRightLocation()) || (this.doubleChest && loc.isOneOf(this.location2)));

        return false;
    }

    @Override
    public int hashCode() {
        // We always want it to use the 'equals(Object)' method to compare locations
        return 0;
    }
}