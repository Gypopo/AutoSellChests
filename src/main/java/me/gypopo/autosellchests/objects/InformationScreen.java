package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import org.bukkit.Bukkit;
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

    public InformationScreen(Chest chest) {
        this.inv = Bukkit.createInventory(this, 45, Lang.INFO_SCREEN_TITLE.get());
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
                Lang.SELL_CHEST_OWNER.get().replace("%player_name%", Bukkit.getPlayer(this.chest.getOwner()).getName()),
                Lang.SELL_CHEST_LOCATION.get().replace("%loc%", "World '" + this.chest.getLocation().getWorld().getName() + "', x" + this.chest.getLocation().getBlockX() + ", y" + this.chest.getLocation().getBlockY() + ", z" + this.chest.getLocation().getBlockZ()),
                Lang.SELL_CHEST_ID.get().replace("%id%", String.valueOf(this.chest.getId())),
                Lang.SELL_CHEST_NEXT_SELL.get().replace("%time%", AutoSellChests.getInstance().getTimeUtils().getReadableTime(this.chest.getNextInterval()))));
        nextSell.setItemMeta(nsM);

        // Shows the total amount of money the player has made so far with this sellchest
        ItemStack totalIncome = new ItemStack(Material.GOLD_INGOT);
        ItemMeta tiM = totalIncome.getItemMeta();
        tiM.setDisplayName(Lang.INCOME_INFO.get().replace("%profit%", String.valueOf(this.chest.getIncome())));
        totalIncome.setItemMeta(tiM);

        // Breaks the chest
        ItemStack destroy = new ItemStack(Material.BARRIER);
        ItemMeta dM = destroy.getItemMeta();
        dM.setDisplayName(Lang.DESTROY_CHEST.get());
        destroy.setItemMeta(dM);

        this.inv.setItem(10, soldItems);
        this.inv.setItem(13, nextSell);
        this.inv.setItem(16, totalIncome);
        this.inv.setItem(31, destroy);

        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null) {
                this.inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    public Chest getChest() {
        return this.chest;
    }

    public void open(Player p) {
        p.openInventory(this.inv);
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }
}
