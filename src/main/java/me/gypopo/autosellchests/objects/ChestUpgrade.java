package me.gypopo.autosellchests.objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ChestUpgrade {

    String getName();

    String getLevelName();

    ItemStack getUpgradeItem(boolean doubleChest);

    boolean buy(Player p, boolean doubleChest);

    String getPrice(boolean doubleChest);
}