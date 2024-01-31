package me.gypopo.autosellchests.objects;

import eu.decentsoftware.holograms.api.utils.scheduler.S;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InformationScreen implements InventoryHolder {

    private final Inventory inv;
    private final Chest chest;
    private final Location selectedChest;
    private BukkitTask dynamicLore;

    public InformationScreen(Chest chest, Location location) {
        this.inv = Bukkit.createInventory(this, 45, Lang.INFO_SCREEN_TITLE.get());
        this.selectedChest = location;
        this.chest = chest;
        this.init();
    }

    private void init() {
        // Shows how many items this chest has sold
        ItemStack soldItems = new ItemStack(Material.HOPPER);
        ItemMeta siM = soldItems.getItemMeta();
        siM.setDisplayName(Lang.SOLD_ITEMS_INFO.get().replace("%amount%", String.valueOf(this.chest.getItemsSold())));
        soldItems.setItemMeta(siM);

        // Shows when the next sell interval will occur
        ItemStack nextSell = new ItemStack(Material.ARROW);
        ItemMeta nsM = nextSell.getItemMeta();
        nsM.setDisplayName(Lang.SELL_CHEST_BLOCK_INFO.get());
        List<String> info = new ArrayList<>();
        info.add(Lang.SELL_CHEST_OWNER.get().replace("%player_name%", Bukkit.getOfflinePlayer(this.chest.getOwner()).getName()));
        info.addAll(AutoSellChests.splitLongString(Lang.SELL_CHEST_LOCATION.get().replace("%loc%", this.getLocation(this.selectedChest))));
        info.add(Lang.SELL_CHEST_ID.get().replace("%id%", String.valueOf(this.chest.getId())));
        info.add(Lang.SELL_CHEST_NEXT_SELL.get().replace("%time%", this.getNextInterval()));
        nsM.setLore(info);
        nextSell.setItemMeta(nsM);

        // Shows the total amount of money the player has made so far with this sellchest
        ItemStack totalIncome = new ItemStack(Material.GOLD_INGOT);
        List<String> l = AutoSellChests.splitLongString(this.chest.getIncome(Lang.INCOME_INFO.get()));
        ItemMeta tiM = totalIncome.getItemMeta();
        tiM.setDisplayName(l.remove(0));
        if (l.size() >= 1)
            tiM.setLore(l);
        totalIncome.setItemMeta(tiM);

        if (!this.chest.getClaimAble().isEmpty()) {
            ItemStack item = new ItemStack(Material.DIAMOND);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(Lang.CLAIM_ABLE_INFO.get());
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);

            this.inv.setItem(22, item);
        }

        // Chest settings
        ItemStack settings = new ItemStack(Material.REDSTONE);
        ItemMeta sM = settings.getItemMeta();
        sM.setDisplayName(Lang.SELL_CHEST_SETTINGS.get());
        settings.setItemMeta(sM);

        // Breaks the chest
        ItemStack destroy = new ItemStack(Material.BARRIER);
        ItemMeta dM = destroy.getItemMeta();
        dM.setDisplayName(Lang.DESTROY_CHEST.get());
        destroy.setItemMeta(dM);

        this.inv.setItem(10, soldItems);
        this.inv.setItem(13, nextSell);
        this.inv.setItem(16, totalIncome);
        this.inv.setItem(30, settings);
        this.inv.setItem(32, destroy);

        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null) {
                this.inv.setItem(i, ChestManager.getFillItem());
            }
        }

        this.dynamicLore = this.updateTime(nextSell);
    }

    private String getLocation(Location loc) {
        return Lang.LOCATION_FORMAT.get()
                .replace("%world%", loc.getWorld().getName())
                .replace("%pos_x%", String.valueOf(loc.getBlockX()))
                .replace("%pos_y%", String.valueOf(loc.getBlockY()))
                .replace("%pos_z%", String.valueOf(loc.getBlockZ()));
    }

    private String getNextInterval() {
        if (this.chest.getNextInterval() < System.currentTimeMillis()) {
            return AutoSellChests.getInstance().getTimeUtils().getReadableTime(AutoSellChests.getInstance().getManager().getNextInterval());
        } else {
            return AutoSellChests.getInstance().getTimeUtils().getReadableTime(this.chest.getNextInterval() - System.currentTimeMillis());
        }
    }

    // Every second use player#getItemOnCursor() so the lore is only updated if the player hovers this item
    private BukkitTask updateTime(ItemStack item) {
        return AutoSellChests.getInstance().runTaskTimer(() -> {
            if (this.inv.getViewers().isEmpty())
                this.stopTask();

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            lore.set(lore.size()-1, Lang.SELL_CHEST_NEXT_SELL.get().replace("%time%", this.getNextInterval()));
            meta.setLore(lore);
            item.setItemMeta(meta);

            this.inv.setItem(13, item);
        }, 20, 20);
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