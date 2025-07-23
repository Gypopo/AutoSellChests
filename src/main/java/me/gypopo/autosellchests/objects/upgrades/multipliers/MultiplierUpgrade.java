package me.gypopo.autosellchests.objects.upgrades.multipliers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.ChestUpgrade;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;
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

public class MultiplierUpgrade implements PriceMultiplier, ChestUpgrade {

    private final int level;

    private final Material item;
    private final String name;
    private final String lvlName;
    private final List<String> lore;
    private final boolean enchanted;

    private final double price;
    private final EcoType priceType;
    private final String permission;

    private final double multiplier;

    public MultiplierUpgrade(ConfigurationSection section, int level)
            throws UpgradeLoadException {
        if (section == null)
            throw new UpgradeLoadException("Failed to load upgrade data", null);
        this.level = level;

        this.name = Lang.formatColors(section.getString("name"));
        if (this.name == null)
            throw new UpgradeLoadException("Failed to get name of upgrade", null);
        this.lvlName = Lang.formatColors(section.getString("lvl-name", String.valueOf(level)));
        this.lore = section.getStringList("lore").stream().map(Lang::formatColors).collect(Collectors.toList());
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
    public String getLevelName() {
        return this.lvlName;
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
        ChestUpgrade nextUpgrade = UpgradeManager.getMultiplierUpgrade(this.level+1);
        if (nextUpgrade != null) {
            List<String> lore = new ArrayList<>(this.lore);
            lore.replaceAll(s -> s.replace("%next-upgrade-cost%", nextUpgrade.getPrice(doubleChest)));
            return lore;
        } else return this.lore;
    }

    @Override
    public boolean buy(Player p, boolean doubleChest) {
        final double finalPrice = doubleChest ? this.price * 2 : this.price;

        EconomyProvider priceProvider = EconomyShopGUIHook.getEcon(this.priceType);
        if (priceProvider.getBalance(p) < finalPrice) {
            Logger.sendPlayerMessage(p, AutoSellChests.getInstance().replaceColoredPlaceholder(Lang.INSUFFICIENT_FUNDS_UPGRADE.get(), "%ecoType%", priceProvider.getFriendly()));
            return false;
        }

        if (this.permission != null && !this.permission.isEmpty()) {
            if (!p.hasPermission(this.permission)) {
                Logger.sendPlayerMessage(p, Lang.NO_UPGRADE_PERMISSIONS.get());
                return false;
            }
        }

        priceProvider.withdrawBalance(p, finalPrice);
        return true;
    }

    @Override
    public String getPrice(boolean doubleChest) {
        return AutoSellChests.getInstance().formatPrice(this.priceType, doubleChest ? this.price * 2 : this.price);
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