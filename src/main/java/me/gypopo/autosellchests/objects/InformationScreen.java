package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.util.*;
import me.gypopo.autosellchests.util.exceptions.InventoryLoadException;
import me.gypopo.autosellchests.util.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InformationScreen implements InventoryHolder {

    private Inventory inv;
    private final Chest chest;
    private final Location selectedChest;
    private SimplePair<Integer, String> updatedString;
    private int infoItemSlot; // The slot of the item which contains the %time% placeholder
    private Task dynamicLore;

    public InformationScreen(Chest chest, Location location) {
        this.selectedChest = location;
        this.chest = chest;

        this.init();
    }

    private void init() {
        SimpleInventoryBuilder builder = AutoSellChests.getInstance().getInventoryManager().getMainInv();
        builder.init(this);

        // Shows how many items this chest has sold
        builder.replace("sold-item", Collections.singletonMap("%amount%", String.valueOf(this.chest.getItemsSold())));

        // Shows when the next sell interval will occur
        builder.replace("info-item", Map.of(
                "%player_name%", Bukkit.getOfflinePlayer(this.chest.getOwner()).getName(),
                "%loc%", this.getLocation(this.selectedChest),
                "%id%", String.valueOf(this.chest.getId()),
                "%interval-name%", UpgradeManager.getIntervalUpgrade(this.chest.getIntervalUpgrade()).getName(),
                "%interval%", TimeUtils.getReadableTime(this.chest.getInterval()),
                "%multiplier-name%", UpgradeManager.getMultiplierUpgrade(this.chest.getMultiplierUpgrade()).getName(),
                "%multiplier%", String.valueOf(this.chest.getMultiplier())
        ));

        // Search the line of meta which contains the %time% placeholder so we can continuously update it
        this.updatedString = this.getIndex(builder.getItem("info-item"), "%time%");
        this.infoItemSlot = builder.getSlot("info-item");

        // Shows the total amount of money the player has made so far with this sellchest
        builder.replace("income-item", Collections.singletonMap("%profit%", this.chest.getIncome(Lang.INCOME_INFO.get())));

        // Only enable claimable item if there are claim ables to claim
        if (!this.chest.getClaimAble().isEmpty())
            builder.enableItem("claimable-item");

        this.inv = builder.build();

        // If %time% placeholder is present, create a new task to update it every 20 ticks
        if (this.updatedString != null)
            this.dynamicLore = this.updateTime(builder.getItem("info-item"));
    }

    private String getLocation(Location loc) {
        return Lang.LOCATION_FORMAT.get()
                .replace("%world%", loc.getWorld().getName())
                .replace("%pos_x%", String.valueOf(loc.getBlockX()))
                .replace("%pos_y%", String.valueOf(loc.getBlockY()))
                .replace("%pos_z%", String.valueOf(loc.getBlockZ()));
    }

    private String getNextInterval() {
        return TimeUtils.getReadableTime(this.chest.getNextInterval() - (System.currentTimeMillis() - 1000L));
    }

    private SimplePair<Integer, String> getIndex(ItemStack item, String toFind) {
        if (!item.hasItemMeta())
            return null;

        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName() && meta.getDisplayName().contains(toFind))
            return new SimplePair<>(-1, meta.getDisplayName());

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains(toFind))
                    return new SimplePair<>(i, lore.get(i));
            }
        }

        return null;
    }

    // Every second use player#getItemOnCursor() so the lore is only updated if the player hovers this item
    private Task updateTime(ItemStack item) {
        return AutoSellChests.getInstance().runTaskTimer(() -> {
            if (this.inv.getViewers().isEmpty())
                this.stopTask();

            ItemMeta meta = item.getItemMeta();
            if (this.updatedString.key == -1) {
                meta.setDisplayName(this.updatedString.value.replace("%time%", this.getNextInterval()));
            } else {
                List<String> lore = meta.getLore();
                lore.set(this.updatedString.key, this.updatedString.value.replace("%time%", this.getNextInterval()));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);

            this.inv.setItem(this.infoItemSlot, item);
        }, 0, 20);
    }

    private void stopTask() {
        this.dynamicLore.cancel();
    }

    public Chest getChest() {
        return this.chest;
    }

    public Location getSelectedChest() {
        return this.selectedChest;
    }

    public void open(Player p) {
        p.openInventory(this.inv);
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }
}