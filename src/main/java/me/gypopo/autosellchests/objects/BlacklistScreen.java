package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.SimpleInventoryBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;

public class BlacklistScreen extends ChestInventory {

    private Inventory inv;
    private final Chest chest;
    private final Location selectedChest;

    private int page;
    private boolean update;

    public BlacklistScreen(Chest chest, Location selectedChest) {
        this.chest = chest;
        this.selectedChest = selectedChest;
        this.init();
    }

    private void init() {
        SimpleInventoryBuilder builder = AutoSellChests.getInstance().getInventoryManager().getBlacklistInv();
        builder.init(this);

        int rows = builder.getSize() / 9;
        int perPage = (rows - 2) * 7; // The inner content area, leaving a border around it

        ArrayList<Material> blacklisted = new ArrayList<>(this.chest.getBlacklist());

        // Clamp the page in case items were removed since it was last opened
        int maxPage = blacklisted.isEmpty() ? 0 : (blacklisted.size() - 1) / perPage;
        if (this.page > maxPage)
            this.page = maxPage;

        // Leave the border of the menu and add the items only in the middle
        int i = this.page * perPage;
        for (int r = 1; r < rows - 1; r++) {
            for (int s = 1; s < 8; s++) {
                if (i >= blacklisted.size())
                    break;

                ItemStack item = new ItemStack(blacklisted.get(i++));
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setLore(Collections.singletonList(Lang.BLACKLIST_REMOVE_ITEM_HINT.get()));
                    item.setItemMeta(meta);
                }
                builder.setItem((r * 9) + s, item);
            }
        }

        // Only display the pagination buttons when there is a page to navigate to
        if (this.page > 0)
            builder.enableItem("previous-page-item");
        if ((this.page + 1) * perPage < blacklisted.size())
            builder.enableItem("next-page-item");

        this.inv = builder.build();
    }

    public void nextPage(Player p) {
        this.page++;
        this.updateInventory(p);
    }

    public void previousPage(Player p) {
        if (this.page > 0)
            this.page--;
        this.updateInventory(p);
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
