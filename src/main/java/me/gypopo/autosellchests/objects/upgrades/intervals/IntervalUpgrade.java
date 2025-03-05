package me.gypopo.autosellchests.objects.upgrades.intervals;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.SimplePair;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.autosellchests.util.exceptions.UpgradeLoadException;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.providers.EconomyProvider;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IntervalUpgrade implements ChestInterval, ChestUpgrade {

    private final int level;

    private final Material item;
    private final String name;
    private final List<String> lore;
    private final boolean enchanted;

    private final double price;
    private final EcoType priceType;
    private final String permission;

    private final long interval;
    private final long ticks;

    public IntervalUpgrade(ConfigurationSection section, int level)
            throws UpgradeLoadException {
        if (section == null)
            throw new UpgradeLoadException("Failed to load upgrade data", null);
        this.level = level;

        SimplePair<Double, EcoType> price = this.loadPrice(section.getString("price"));
        this.price = price.key;
        this.priceType = price.value;
        this.permission = section.getString("permission");

        this.interval = this.getSellInterval(section.getString("interval"));
        this.ticks = this.interval / 1000L * 20L;

        this.name = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
        if (this.name == null)
            throw new UpgradeLoadException("Failed to get name of upgrade", null);
        this.lore = section.getStringList("lore").stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList());
        this.enchanted = section.getBoolean("enchanted");

        try {
            this.item = Material.valueOf(section.getString("item"));
        } catch (IllegalArgumentException e) {
            throw new UpgradeLoadException("Failed to get item material of upgrade for '" + section.getString("item") + "'", null);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ItemStack getUpgradeItem(boolean doubleChest) {
        ItemStack item = new ItemStack(this.item);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(this.name);
        meta.setLore(this.getLore(doubleChest));
        if (this.enchanted) {
            if (AutoSellChests.getInstance().version >= 121) {
                meta.setEnchantmentGlintOverride(true);
            } else {
                meta.addItemFlags(ItemFlag.values());
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            }
        }
        item.setItemMeta(meta);

        return item;
    }

    private List<String> getLore(boolean doubleChest) {
        ChestUpgrade nextUpgrade = UpgradeManager.getIntervalUpgrade(this.level+1);
        if (nextUpgrade != null) {
            List<String> lore = new ArrayList<>(this.lore);
            lore.replaceAll(s -> s.replace("%next-upgrade-cost%", nextUpgrade.getPrice(doubleChest)));
            return lore;
        } else return this.lore;
    }

    @Override
    public boolean buy(Player p, boolean doubleChest) {
        EconomyProvider priceProvider = EconomyShopGUIHook.getEcon(this.priceType);
        if (priceProvider.getBalance(p) < this.price) {
            Logger.sendPlayerMessage(p, Lang.INSUFFICIENT_FUNDS_UPGRADE.get().replace("%ecoType%", priceProvider.getFriendly()));
            return false;
        }

        if (this.permission != null && !this.permission.isEmpty()) {
            if (!p.hasPermission(this.permission)) {
                Logger.sendPlayerMessage(p, Lang.NO_UPGRADE_PERMISSIONS.get());
                return false;
            }
        }

        priceProvider.withdrawBalance(p, this.price);
        return true;
    }

    @Override
    public String getPrice(boolean doubleChest) {
        return AutoSellChests.getInstance().formatPrice(this.priceType, doubleChest ? this.price * 2 : this.price);
    }

    @Override
    public long getInterval() {
        return this.interval;
    }

    @Override
    public long getTicks() {
        return this.ticks;
    }

    private SimplePair<Double, EcoType> loadPrice(String price) throws UpgradeLoadException {
        try {
            if (price.contains("::")) {
                EcoType econ = EconomyType.getFromString(price.split("::")[0]);
                if (econ == null)
                    throw new UpgradeLoadException("Failed to load upgrade price, economy type such as '" + price + "' is not supported", null);

                if (EconomyShopGUIHook.getEcon(econ) == null) // EconomyType not active
                    throw new UpgradeLoadException("Failed to load upgrade price, economy type such as '" + price + "' was not enabled inside EconomyShopGUI", null);

                return new SimplePair<>(Double.parseDouble(price.split("::")[1]), econ);
            } else
                return new SimplePair<>(Double.parseDouble(price), null);
        } catch (NumberFormatException | NullPointerException e) {
            throw new UpgradeLoadException("Failed to load upgrade price, amount for '" + price + "' is invalid", null);
        }
    }

    private long getSellInterval(String time) throws UpgradeLoadException {
        try {
            return TimeUtils.getTime(time);
        } catch (ParseException | NullPointerException e) {
            throw new UpgradeLoadException("Invalid sell interval for " + time, e);
        }
    }
}
