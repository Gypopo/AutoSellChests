package me.gypopo.autosellchests.objects.upgrades.multipliers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;
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
import java.util.List;
import java.util.stream.Collectors;

public class MultiplierUpgrade implements PriceMultiplier, ChestUpgrade {

    private final Material item;
    private final String name;
    private final List<String> lore;
    private final boolean enchanted;

    private final double price;
    private final EcoType priceType;
    private final String permission;

    private final double multiplier;

    public MultiplierUpgrade(ConfigurationSection section)
            throws UpgradeLoadException {
        if (section == null)
            throw new UpgradeLoadException("Failed to load upgrade data", null);
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

        SimplePair<Double, EcoType> price = this.loadPrice(section.getString("price"));
        this.price = price.key;
        this.priceType = price.value;
        this.permission = section.getString("permission");

        this.multiplier = this.getPriceMultiplier(section.getString("multiplier"));
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
        if (this.enchanted) {
            if (AutoSellChests.getInstance().version >= 121) {
                meta.setEnchantmentGlintOverride(true);
            } else {
                meta.addItemFlags(ItemFlag.values());
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            }
        }
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

        if (this.permission != null && !this.permission.isEmpty()) {
            if (!p.hasPermission(this.permission)) {
                p.sendMessage(Lang.NO_UPGRADE_PERMISSIONS.get());
                return false;
            }
        }

        priceProvider.withdrawBalance(p, this.price);
        return true;
    }

    @Override
    public String getPrice() {
        return AutoSellChests.getInstance().formatPrice(this.priceType, this.price);
    }

    @Override
    public double getMultiplier() {
        return this.multiplier;
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

    private double getPriceMultiplier(String multiplier) throws UpgradeLoadException {
        try {
            double x = Double.parseDouble(multiplier);
            if (x <= 0)
                throw new UpgradeLoadException("Invalid price multiplier for " + multiplier + ", cannot be below 0", null);

            return x;
        } catch (NumberFormatException e) {
            throw new UpgradeLoadException("Invalid price multiplier for " + multiplier, e);
        }
    }
}