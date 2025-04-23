package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.exceptions.InventoryLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple class that loads a static inventory based of the provided ConfigurationSection
 *
 * Made by Gypopo
 */
public class SimpleInventoryBuilder {

    private final int size;
    private final String title;
    private ItemStack fillItem;
    private final Map<String, SimplePair<Integer, ItemStack>> items = new HashMap<>();

    // The builder which is recreated each time a new inventory is opened
    private Inventory inv;

    public SimpleInventoryBuilder(ConfigurationSection config)
            throws InventoryLoadException {
        if (config == null)
            throw new NullPointerException("Cannot find inventory config in menus.yml");

        int size = config.getInt("gui-rows", -1);
        if (size > 6 || size < 1)
            throw new InventoryLoadException("Invalid inventory size at '" + config.getCurrentPath() + ".gui-rows', needs to be a number from 1-6");

        this.size = size * 9;
        this.loadItems(config);

        this.title = this.translate(config.getString("title"));

        if (config.contains("fill-item") && !config.getString("fill-item.material", "AIR").equalsIgnoreCase("AIR"))
            this.fillItem = this.loadItem(config.getConfigurationSection("fill-item"));
    }

    // Creates a new inventory object
    public void init(@Nullable InventoryHolder holder) {
        this.inv = Bukkit.createInventory(holder, this.size, this.title);

        // Add all items to the inv
        for (String i : this.items.keySet()) {
            if (i.equals("claimable-item"))
                continue; // Claimable item should only show when claimable

            SimplePair<Integer, ItemStack> item = this.items.get(i);
            if (item.value != null)
                this.inv.setItem(item.key, item.value);
        }
    }

    // Builds the final inventory
    public Inventory build() {
        // Set the fill items as last
        for (int i = 0; i < this.inv.getSize(); i++) {
            if (this.inv.getItem(i) == null)
                this.inv.setItem(i, this.fillItem);
        }

        return this.inv;
    }

    private void loadItems(ConfigurationSection config)
            throws InventoryLoadException {
        for (String i : config.getConfigurationSection("items").getKeys(false)) {
            if (i.contains("-") && i.split("-", 2)[1].equalsIgnoreCase("upgrade-item")) {
                this.items.put(i, new SimplePair<>(config.getInt("items." + i + ".slot")-1, null));
                continue;
            }

            try {
                ConfigurationSection section = config.getConfigurationSection("items." + i);
                ItemStack item = this.loadItem(section);

                if (section.contains("slot")) {
                    int slot = section.getInt("slot");

                    if (slot > this.size || slot < 1)
                        throw new InventoryLoadException("Item slots need to be a number from 1-" + this.size);

                    this.items.put(i, new SimplePair<>(slot-1, item));
                } else this.inv.addItem(item);
            } catch (Exception e) {
                throw new InventoryLoadException("Failed to load item from menus.yml at " + config.getCurrentPath() + ".items: " + e.getMessage());
            }
        }
    }

    private ItemStack loadItem(ConfigurationSection config)
            throws InventoryLoadException {
        ItemStack item;
        try {
            item = new ItemStack(Material.valueOf(config.getString("material").toUpperCase(Locale.ROOT)), config.getInt("amount", 1));
        } catch (IllegalArgumentException e) {
            throw new InventoryLoadException("Invalid item material for " + config.getString("material"));
        }

        ItemMeta meta = item.getItemMeta();
        if (config.contains("name"))
            meta.setDisplayName(this.translate(config.getString("name")));
        if (config.contains("lore"))
            meta.setLore(config.getStringList("lore").stream().map(this::translate).collect(Collectors.toList()));
        if (config.getBoolean("enchantment-glint")) {
            if (AutoSellChests.getInstance().version <= 121) {
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);

        return item;
    }

    // Used to get the slot of a item, for reference only
    public int getSlot(String item) {
        return this.items.get(item).key;
    }

    // Used to get the item object
    public ItemStack getItem(String item) {
        return this.inv.getItem(this.items.get(item).key);
    }

    // Used for items which should only be displayed if its actually used(i.e. claimAbleProfit item)
    public void enableItem(String i) {
        SimplePair<Integer, ItemStack> item = this.items.get(i);
        this.inv.setItem(item.key, item.value);
    }

    // Used to insert custom items(i.e. upgrade items(configured in config.yml))
    public void insertItem(String i, ItemStack item) {
        this.inv.setItem(this.items.get(i).key, item);
    }

    // Used to replace values in item name/lore
    public void replace(String i, Map<String, String> replacements) {
        int slot = this.items.get(i).key;
        ItemStack item = this.inv.getItem(slot).clone(); // Clone the item since we are replacing its contents

        if (!item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta.hasDisplayName()) {
            String original = meta.getDisplayName();
            boolean updated = false; // Only update if necessary, since meta#setDisplayName/#setLore is badly optimized

            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                if (original.contains(replacement.getKey())) {
                    original = original.replace(replacement.getKey(), replacement.getValue());
                    updated = true;
                }
            }

            if (updated)
                meta.setDisplayName(original);
        }

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            boolean updated = false; // Only update if necessary, since meta#setDisplayName/#setLore is badly optimized

            for (int e = 0; e < lore.size(); e++) {
                String original = lore.get(e);
                for (Map.Entry<String, String> replacement : replacements.entrySet()) {

                    original = original.replace(replacement.getKey(), replacement.getValue());
                    updated = true;
                }

                lore.set(e, original);
            }

            if (updated)
                meta.setLore(lore);
        }

        item.setItemMeta(meta);

        this.inv.setItem(slot, item);
    }

    // Translates the string from the lang.yml if used
    private String translate(String s) {
        if (s == null) return "";

        if (s.contains("%translations-")) {
            try {
                Lang placeholder = Lang.valueOf(s.split("%translations-")[1].split("%")[0].toUpperCase(Locale.ENGLISH).replace("-", "_"));
                s = s.replace("%translations-" + placeholder.getKey() + "%", placeholder.get());
            } catch (IllegalArgumentException ignored) {}
        }
        return Lang.formatColors(s, null);
    }
}
