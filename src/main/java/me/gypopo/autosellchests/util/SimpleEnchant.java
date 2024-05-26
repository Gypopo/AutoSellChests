package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

public enum SimpleEnchant {

    UNBREAKING("DURABILITY"),
    ;

    private static boolean components; // API 1.20.5+

    static {
        try {
            Version ver = new Version(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].replace("_", ".").replace("v", "").replace("R", ""));
            components = ver.isGreater(new Version("1.20.3")); // Checks whether package name is greater as v1_20_R3(ie. 1.20.5+)
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            Version ver = new Version(Bukkit.getBukkitVersion().split("-")[0]);
            components = ver.isGreater(new Version("1.20.4")); // Checks whether the re-allocated package name is greater as v1_20_R3(ie. 1.20.5+)
        }
    }

    private final String[] legacy;

    SimpleEnchant(String... legacy) {
        this.legacy = legacy;
    }

    public String getVanillaName() {
        return components || this.legacy.length == 0 ? this.name() : this.legacy[0];
    }

    public Enchantment get() {
        return Enchantment.getByKey(NamespacedKey.minecraft(this.getVanillaName().toLowerCase(Locale.ENGLISH)));
    }
}
