package me.gypopo.autosellchests.objects.upgrades.intervals;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.ChestUpgrade;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

public class IntervalUpgrade implements ChestInterval, ChestUpgrade {

    private final Material item;
    private final String name;
    private final List<String> lore;

    private final double price;
    private final EcoType priceType;

    private final long interval;
    private final long ticks;

    public IntervalUpgrade(ConfigurationSection section)
            throws UpgradeLoadException {
        if (section == null)
            throw new UpgradeLoadException("Failed to load upgrade data", null);
        this.name = section.getString("name");
        if (this.name == null)
            throw new UpgradeLoadException("Failed to get name of upgrade", null);
        this.lore = section.getStringList("lore").stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList());

        try {
            this.item = Material.valueOf(section.getString("item"));
        } catch (IllegalArgumentException e) {
            throw new UpgradeLoadException("Failed to get item material of upgrade for '" + section.getString("item") + "'", null);
        }

        SimplePair<Double, EcoType> price = this.loadPrice(section.getString("price"));
        this.price = price.key;
        this.priceType = price.value;

        this.interval = this.getSellInterval(section.getString("interval"));
        this.ticks = this.interval / 1000L * 20L;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ItemStack getUpgradeItem() {
        ItemStack item = new ItemStack(this.item);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(this.name);
        meta.setLore(this.lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public boolean buy(Player p) {
        EconomyProvider priceProvider = EconomyShopGUIHook.getEcon(this.priceType);
        if (priceProvider.getBalance(p) < this.price) {
            p.sendMessage(Lang.INSUFFICIENT_FUNDS_UPGRADE.get().replace("%ecoType%", "money"));
            return false;
        }

        priceProvider.withdrawBalance(p, this.price);
        return true;
    }

    @Override
    public String getPrice() {
        return AutoSellChests.getInstance().formatPrice(this.priceType, this.price);
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
            if (price.contains(":")) {
                EcoType econ = EconomyType.getFromString(price.split(":")[0]);
                if (econ == null)
                    throw new UpgradeLoadException("Failed to load upgrade price, economy type such as '" + price + "' is not supported", null);

                if (EconomyShopGUIHook.getEcon(econ) == null) // EconomyType not active
                    throw new UpgradeLoadException("Failed to load upgrade price, economy type such as '" + price + "' was not enabled inside EconomyShopGUI", null);

                return new SimplePair<>(Double.parseDouble(price.split(":")[1]), econ);
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
