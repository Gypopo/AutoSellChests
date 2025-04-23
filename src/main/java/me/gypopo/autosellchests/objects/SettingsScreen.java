package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.util.SimpleInventoryBuilder;
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

    private Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

    public SettingsScreen(Chest chest, Location selectedChest) {
        this.selectedChest = selectedChest;
        this.chest = chest;
        this.init();
    }

    private void init() {
        SimpleInventoryBuilder builder = AutoSellChests.getInstance().getInventoryManager().getSettingsInv();
        builder.init(this);

        // Toggle sold items logging
        builder.replace("logging-item", Collections.singletonMap("%value%", String.valueOf(chest.isLogging())));

        // Allow the chest to have a custom name
        builder.replace("rename-item", Collections.singletonMap("%chest-name%", String.valueOf(chest.getName())));

        this.inv = builder.build();
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
