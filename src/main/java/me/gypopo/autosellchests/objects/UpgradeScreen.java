package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class UpgradeScreen implements InventoryHolder {

    private final Inventory inv;
    private final Chest chest;

    private final int intervalSlot = UpgradeManager.multiplierUpgrades ? 11 : 13;
    private final int multiplierSlot = UpgradeManager.intervalUpgrades ? 15 : 13;

    public UpgradeScreen(Chest chest) {
        this.inv = Bukkit.createInventory(this, 27, Lang.CHEST_UPGRADE_TITLE.get());
        this.chest = chest;
        this.init();
    }

    public int getIntervalSlot() {
        return this.intervalSlot;
    }

    public int getMultiplierSlot() {
        return this.multiplierSlot;
    }

    private void init() {
        // Current upgrade
        if (UpgradeManager.intervalUpgrades) {
            ItemStack upgradeItem = UpgradeManager.getIntervalUpgrade(this.chest.getIntervalUpgrade()).getUpgradeItem(this.chest.isDoubleChest());
            this.inv.setItem(this.intervalSlot, upgradeItem);
        }

        if (UpgradeManager.multiplierUpgrades) {
            ItemStack upgradeItem = UpgradeManager.getMultiplierUpgrade(this.chest.getMultiplierUpgrade()).getUpgradeItem(this.chest.isDoubleChest());
            this.inv.setItem(this.multiplierSlot, upgradeItem);
        }

        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null) {
                this.inv.setItem(i, ChestManager.getFillItem());
            }
        }
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
