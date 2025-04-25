package me.gypopo.autosellchests.objects.upgrades.intervals;

import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.ParseException;

public class DefaultInterval implements ChestInterval, ChestUpgrade {

    private final long interval;
    private final long ticks;

    public DefaultInterval() {
        this.interval = this.getSellInterval();
        this.ticks = this.interval / 1000L * 20L;
    }

    @Override
    public long getInterval() {
        return this.interval;
    }

    @Override
    public long getTicks() {
        return this.ticks;
    }

    private long getSellInterval() {
        try {
            return TimeUtils.getTime(Config.get().getString("autosell-interval"));
        } catch (ParseException | NullPointerException e) {
            Logger.warn("Could not read the 'autosell-interval' from config, using 10 minutes as default.");
            e.printStackTrace();
            return 600000; // Default to ten minutes
        }
    }

    @Override
    public String getName() {
        return "Default interval";
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