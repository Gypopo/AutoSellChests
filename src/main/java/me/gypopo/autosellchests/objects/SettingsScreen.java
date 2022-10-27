package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.files.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class SettingsScreen implements InventoryHolder {

    private final Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

    public SettingsScreen(Chest chest, Location selectedChest) {
        this.inv = Bukkit.createInventory(this, 9, Lang.CHEST_SETTINGS_TITLE.get());
        this.selectedChest = selectedChest;
        this.chest = chest;
        this.init();
    }

    private void init() {
        // Toggle sold items logging
        ItemStack logging = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta lM = logging.getItemMeta();
        lM.setDisplayName(Lang.TOGGLE_SOLD_ITEMS_LOGGING.get());
        lM.setLore(Collections.singletonList(Lang.CURRENT_VALUE.get().replace("%value%", String.valueOf(chest.isLogging()))));
        logging.setItemMeta(lM);

        this.inv.setItem(4, logging);

        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null) {
                this.inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
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
