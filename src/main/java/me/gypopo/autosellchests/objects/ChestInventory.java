package me.gypopo.autosellchests.objects;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class ChestInventory implements InventoryHolder {

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public abstract Chest getChest();

    public abstract Location getSelectedChest();

    public abstract boolean isUpdatingInventory();
}