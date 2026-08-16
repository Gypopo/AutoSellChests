package me.gypopo.autosellchests.api;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.Chest;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutoSellChestsAPI {

    private AutoSellChestsAPI() {}

    private static ChestManager getManger() {
        return AutoSellChests.getInstance().getManager();
    }

    /**
     * @return whether the given block is a sell chest
     */
    public static boolean isSellChest(@Nullable Block block) {
        return block != null && isSellChest(block.getLocation());
    }

    /**
     * @return whether a registered sell chest exists at the given location
     */
    public static boolean isSellChest(@Nullable Location location) {
        return location != null && getManger().getChestByLocation(location) != null;
    }

    /**
     * @return the sell chest at the given block, or {@code null} if none exists there
     */
    @Nullable
    public static Chest getSellChest(@Nullable Block block) {
        return block == null ? null : getSellChest(block.getLocation());
    }

    /**
     * @return the sell chest at the given location, or {@code null} if none exists there
     */
    @Nullable
    public static Chest getSellChest(@Nullable Location location) {
        return location == null ? null : getManger().getChestByLocation(location);
    }

    /**
     * @return the sell chest with the given internal id, or {@code null} if it does not exist
     */
    @Nullable
    public static Chest getSellChest(int id) {
        return getManger().getChestByID(id);
    }

    /**
     * @return the current sell price multiplier upgrade level of the chest
     */
    public static int getMultiplierLvl(@NotNull Chest chest) {
        Validate.notNull(chest, "chest cannot be null");

        return chest.getMultiplierUpgrade();
    }

    /**
     * @return the current sell price multiplier value of the chest (e.g. {@code 1.5} for +50%)
     */
    public static double getMultiplier(@NotNull Chest chest) {
        Validate.notNull(chest, "chest cannot be null");

        return chest.getMultiplier();
    }

    /**
     * @return the current interval upgrade level of the chest
     */
    public static int getIntervalLvl(@NotNull Chest chest) {
        Validate.notNull(chest, "chest cannot be null");

        return chest.getIntervalUpgrade();
    }

    /**
     * @return the current selling interval of the chest in milliseconds
     */
    public static long getInterval(@NotNull Chest chest) {
        Validate.notNull(chest, "chest cannot be null");

        return chest.getInterval();
    }

    /**
     * @return the highest available multiplier upgrade level (the amount of configured levels minus one)
     */
    public static int getMaxMultiplierLvl() {
        return UpgradeManager.getMultipliers().length - 1;
    }

    /**
     * @return the highest available interval upgrade level (the amount of configured levels minus one)
     */
    public static int getMaxIntervalLvl() {
        return UpgradeManager.getDifferentIntervals() - 1;
    }
}
