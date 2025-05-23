package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.economyshopgui.util.EcoType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ClaimProfitsScreen extends ChestInventory {

    private final Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

    private boolean update;

    public ClaimProfitsScreen(Chest chest, Location selectedChest) {
        this.inv = Bukkit.createInventory(this, (int)Math.ceil(chest.getClaimAble().size()/9.0)*9, Lang.AVAILABLE_PROFIT_MENU_TITLE.get());
        this.selectedChest = selectedChest;
        this.chest = chest;
        this.init();
    }

    private void init() {
        int slot = 0;
        for (EcoType claimAble : this.chest.getClaimAble().keySet()) {
            ItemStack item;
            try {
                item = claimAble.getCurrency() != null && claimAble.getType().name().equals("ITEM") ?
                        new ItemStack(Material.valueOf(claimAble.getCurrency().toUpperCase(Locale.ENGLISH))) : new ItemStack(Material.GOLD_INGOT);
            } catch (IllegalArgumentException e) {
                item = new ItemStack(Material.GOLD_INGOT);
            }
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(Lang.AVAILABLE_PROFIT.get().replace("%profit%", AutoSellChests.getInstance().formatPrice(claimAble, this.chest.getClaimAble().get(claimAble))));
            item.setItemMeta(meta);
            this.inv.setItem(slot++, item);
        }

        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null) {
                this.inv.setItem(i, ChestManager.getFillItem());
            }
        }
    }

    @Override
    public Chest getChest() {
        return this.chest;
    }

    @Override
    public Location getSelectedChest() {
        return this.selectedChest;
    }

    @Override
    public boolean isUpdatingInventory() {
        return this.update;
    }

    public void open(Player p) {
        p.openInventory(this.inv);
    }

    public void update() {
        this.update = true;
        AutoSellChests.getInstance().runTaskLater(() -> this.update = false, 1L);
    }

    public void updateInventory(Player p) {
       this.update();

        this.inv.clear();
        this.init();
        this.open(p);
    }
}