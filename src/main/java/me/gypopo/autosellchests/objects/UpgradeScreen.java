package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.util.SimpleInventoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class UpgradeScreen extends ChestInventory {

    private Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

    private boolean update;

    public UpgradeScreen(Chest chest, Location selectedChest) {
        this.selectedChest = selectedChest;
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
