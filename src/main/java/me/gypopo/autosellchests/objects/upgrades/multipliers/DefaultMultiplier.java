package me.gypopo.autosellchests.objects.upgrades.multipliers;

import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DefaultMultiplier implements PriceMultiplier, ChestUpgrade {

    private final double multiplier;

    public DefaultMultiplier() {
        this.multiplier = 1d;
    }

    @Override
    public double getMultiplier() {
        return this.multiplier;
    }

    @Override
    public String getName() {
        return "No multiplier";
    }

    @Override
    public ItemStack getUpgradeItem(boolean doubleChest) {
        return null;
    }

    @Override
    public boolean buy(Player p, boolean doubleChest) {
        return false;
    }

    @Override
    public String getPrice(boolean doubleChest) {
        return "";
    }
}