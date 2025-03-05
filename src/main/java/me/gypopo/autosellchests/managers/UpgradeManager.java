package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;
import me.gypopo.autosellchests.objects.upgrades.intervals.DefaultInterval;
import me.gypopo.autosellchests.objects.upgrades.intervals.IntervalUpgrade;
import me.gypopo.autosellchests.objects.upgrades.multipliers.DefaultMultiplier;
import me.gypopo.autosellchests.objects.upgrades.multipliers.MultiplierUpgrade;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.exceptions.UpgradeLoadException;

import java.util.*;
import java.util.stream.Collectors;

public class UpgradeManager {

    private static final List<ChestInterval> INTERVAL_UPGRADES = new ArrayList<>();
    private static final List<PriceMultiplier> MULTIPLIER_UPGRADES = new ArrayList<>();
    public static boolean intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);
    public static boolean multiplierUpgrades = Config.get().getBoolean("enable-multiplier-upgrades", true);

    public UpgradeManager(AutoSellChests plugin) {
        intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);
        multiplierUpgrades = Config.get().getBoolean("enable-multiplier-upgrades", true);

        if (intervalUpgrades) {
            this.loadIntervalUpgrades();
        } else INTERVAL_UPGRADES.add(new DefaultInterval());
        if (multiplierUpgrades) {
            this.loadMultiplierUpgrades();
        } else MULTIPLIER_UPGRADES.add(new DefaultMultiplier());
    }

    public void reload() {
        INTERVAL_UPGRADES.clear();
        MULTIPLIER_UPGRADES.clear();

        intervalUpgrades = Config.get().getBoolean("enable-interval-upgrades", true);
        multiplierUpgrades = Config.get().getBoolean("enable-multiplier-upgrades", true);

        if (intervalUpgrades) {
            this.loadIntervalUpgrades();
        } else INTERVAL_UPGRADES.add(new DefaultInterval());
        if (multiplierUpgrades) {
            this.loadMultiplierUpgrades();
        } else MULTIPLIER_UPGRADES.add(new DefaultMultiplier());
    }

    private void loadIntervalUpgrades() {
        final Map<Integer, String> levels = this.getLevels("interval-upgrades");

        final List<Integer> order = new ArrayList<>(levels.keySet());
        Collections.sort(order);

        int lvl = 0;
        for (int i : order) {
            try {
                INTERVAL_UPGRADES.add(new IntervalUpgrade(Config.get().getConfigurationSection("interval-upgrades." + levels.get(i)), lvl++));
            } catch (UpgradeLoadException e) {
                Logger.warn("Failed to enable interval upgrade '" + levels.get(i) + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!INTERVAL_UPGRADES.isEmpty())
            Logger.info("Completed loading " + INTERVAL_UPGRADES.size() + " interval upgrade(s) with weight: " + order.stream().map(String::valueOf).collect(Collectors.joining(" -> ")));
    }

    private void loadMultiplierUpgrades() {
        final Map<Integer, String> levels = this.getLevels("multiplier-upgrades");

        final List<Integer> order = new ArrayList<>(levels.keySet());
        Collections.sort(order);

        int lvl = 0;
        for (int i : order) {
            try {
                MULTIPLIER_UPGRADES.add(new MultiplierUpgrade(Config.get().getConfigurationSection("multiplier-upgrades." + levels.get(i)), lvl++));
            } catch (UpgradeLoadException e) {
                Logger.warn("Failed to enable multiplier upgrade '" + levels.get(i) + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!MULTIPLIER_UPGRADES.isEmpty())
            Logger.info("Completed loading " + MULTIPLIER_UPGRADES.size() + " multiplier upgrade(s) with weight: " + order.stream().map(String::valueOf).collect(Collectors.joining(" -> ")));
    }

    private Map<Integer, String> getLevels(String path) {
        final Map<Integer, String> levels = new HashMap<>();
        for (String upgrade : Config.get().getConfigurationSection(path).getKeys(false)) {
            int weight = Config.get().getInt(path + "." + upgrade + ".weight");
            if (levels.containsKey(weight)) {
                Logger.warn("Failed to load upgrade '" + path + "." + upgrade + "': There cannot be two upgrades with weight " + weight);
                continue;
            }

            levels.put(weight, upgrade);
        }

        return levels;
    }

    public static ChestUpgrade getIntervalUpgrade(int upgrade) {
        try {
            return (ChestUpgrade) INTERVAL_UPGRADES.get(upgrade);
        } catch (IndexOutOfBoundsException e) {}

        return null;
    }

    public static ChestUpgrade getMultiplierUpgrade(int upgrade) {
        try {
            return (ChestUpgrade) MULTIPLIER_UPGRADES.get(upgrade);
        } catch (IndexOutOfBoundsException e) {}

        return null;
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

    public static Double[] getMultipliers() {
        return MULTIPLIER_UPGRADES.stream().map(PriceMultiplier::getMultiplier).toArray(Double[]::new);
    }
}