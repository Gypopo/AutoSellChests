package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.methodes.SendMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class InformationScreen implements InventoryHolder {

    private final Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

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
        nsM.setLore(Arrays.asList(
                Lang.SELL_CHEST_OWNER.get().replace("%player_name%", Bukkit.getOfflinePlayer(this.chest.getOwner()).getName()),
                Lang.SELL_CHEST_LOCATION.get().replace("%loc%", "World '" + this.selectedChest.getWorld().getName() + "', x" + this.selectedChest.getBlockX() + ", y" + this.selectedChest.getBlockY() + ", z" + this.selectedChest.getBlockZ()),
                Lang.SELL_CHEST_ID.get().replace("%id%", String.valueOf(this.chest.getId())),
                Lang.SELL_CHEST_NEXT_SELL.get().replace("%time%", this.getNextInterval())));
        nextSell.setItemMeta(nsM);

        // Shows the total amount of money the player has made so far with this sellchest
        ItemStack totalIncome = new ItemStack(Material.GOLD_INGOT);
        ItemMeta tiM = totalIncome.getItemMeta();
        tiM.setDisplayName(Lang.INCOME_INFO.get().replace("%profit%", AutoSellChests.getInstance().formatPrice(this.chest.getIncome())));
        totalIncome.setItemMeta(tiM);

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
                this.inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    private String getNextInterval() {
        if (this.chest.getNextInterval() < System.currentTimeMillis()) {
            return AutoSellChests.getInstance().getTimeUtils().getReadableTime(AutoSellChests.getInstance().getManager().getNextInterval());
        } else {
            return AutoSellChests.getInstance().getTimeUtils().getReadableTime(this.chest.getNextInterval() - System.currentTimeMillis());
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
