package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.util.SimpleInventoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class UpgradeScreen implements InventoryHolder {

    private Inventory inv;
    private final Chest chest;

    public UpgradeScreen(Chest chest) {
        this.chest = chest;
        this.init();
    }

    private void init() {
        SimpleInventoryBuilder builder = AutoSellChests.getInstance().getInventoryManager().getUpgradeInv();
        builder.init(this);

        // Current upgrade
        if (UpgradeManager.intervalUpgrades) {
            ItemStack upgradeItem = UpgradeManager.getIntervalUpgrade(this.chest.getIntervalUpgrade()).getUpgradeItem(this.chest.isDoubleChest());
            builder.insertItem("interval-upgrade-item", upgradeItem);
        }

        if (UpgradeManager.multiplierUpgrades) {
            ItemStack upgradeItem = UpgradeManager.getMultiplierUpgrade(this.chest.getMultiplierUpgrade()).getUpgradeItem(this.chest.isDoubleChest());
            builder.insertItem("multiplier-upgrade-item", upgradeItem);
        }

        this.inv = builder.build();
    }

    public Chest getChest() {
        return this.chest;
    }

    public void open(Player p) {
        p.openInventory(this.inv);
    }

    public void updateInventory(Player p) {
        this.inv.clear();
        this.init();
        this.open(p);
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }
}
