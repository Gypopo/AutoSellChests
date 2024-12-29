package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.intervals.DefaultInterval;
import me.gypopo.autosellchests.objects.upgrades.intervals.IntervalUpgrade;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.exceptions.UpgradeLoadException;

import java.util.*;
import java.util.stream.Collectors;

public class UpgradeManager {

    private static final List<ChestInterval> INTERVAL_UPGRADES = new ArrayList<>();
    private static final List<ChestUpgrade> MULTIPLIER_UPGRADES = new ArrayList<>();
    public static boolean intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);
    public static boolean multiplierUpgrades = Config.get().getBoolean("enable-multiplier-upgrades", true);

    public UpgradeManager(AutoSellChests plugin) {
        intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);

        if (intervalUpgrades) {
            this.loadIntervalUpgrades();
        } else INTERVAL_UPGRADES.add(new DefaultInterval());
    }

    public void reload() {
        INTERVAL_UPGRADES.clear();
        MULTIPLIER_UPGRADES.clear();

        intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);

        if (intervalUpgrades) {
            this.loadIntervalUpgrades();
        } else INTERVAL_UPGRADES.add(new DefaultInterval());
    }

    private void loadIntervalUpgrades() {
        final Map<Integer, IntervalUpgrade> levels = new HashMap<>();
        for (String upgrade : Config.get().getConfigurationSection("interval-upgrades").getKeys(false)) {
            int weight = Config.get().getInt("interval-upgrades." + upgrade + ".weight");
            if (levels.containsKey(weight)) {
                Logger.warn("Failed to enable interval upgrade '" + upgrade + "': There cannot be two upgrades with weight " + weight);
                continue;
            }

            try {
                levels.put(weight, new IntervalUpgrade(Config.get().getConfigurationSection("interval-upgrades." + upgrade)));
            } catch (UpgradeLoadException e) {
                Logger.warn("Failed to enable interval upgrade '" + upgrade + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        final List<Integer> order = new ArrayList<>(levels.keySet());
        Collections.sort(order);
        for (int i : order) {
            INTERVAL_UPGRADES.add(levels.get(i));
        }

        if (!INTERVAL_UPGRADES.isEmpty())
            Logger.info("Completed loading " + INTERVAL_UPGRADES.size() + " interval upgrade(s) with weight: " + order.stream().map(String::valueOf).collect(Collectors.joining(" -> ")));
    }

    public static ChestUpgrade getIntervalUpgrade(int upgrade) {
        return (ChestUpgrade) INTERVAL_UPGRADES.get(upgrade);
    }

    public static Long[] getIntervalsInTicks() {
        return INTERVAL_UPGRADES.stream().map(ChestInterval::getTicks).toArray(Long[]::new);
    }

    public static Long[] getIntervals() {
        return INTERVAL_UPGRADES.stream().map(ChestInterval::getInterval).toArray(Long[]::new);
    }

    public static int getDifferentIntervals() {
        return INTERVAL_UPGRADES.size();
    }
}