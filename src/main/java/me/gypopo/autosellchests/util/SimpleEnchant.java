package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

public enum SimpleEnchant {

    UNBREAKING("DURABILITY"),
    ;

    private final String[] legacy;

    SimpleEnchant(String... legacy) {
        this.legacy = legacy;
    }

    public String getVanillaName() {
        return this.legacy.length == 0 ? this.name() : this.legacy[0];
    }

    public Enchantment get() {
        return Enchantment.getByKey(NamespacedKey.minecraft(this.getVanillaName().toLowerCase(Locale.ENGLISH)));
    }
}
