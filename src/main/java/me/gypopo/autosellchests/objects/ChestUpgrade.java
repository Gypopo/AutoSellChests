package me.gypopo.autosellchests.objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ChestUpgrade {

    String getName();

    ItemStack getUpgradeItem();

    boolean buy(Player p);

    String getPrice();
}