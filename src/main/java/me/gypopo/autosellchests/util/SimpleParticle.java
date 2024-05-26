package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import org.bukkit.Bukkit;
import org.bukkit.Particle;

public enum SimpleParticle {

    WITCH("SPELL_WITCH"),
    DUST("REDSTONE"),
    CLOUD(),
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

    SimpleParticle(String... legacy) {
        this.legacy = legacy;
    }

    public Particle get() {
        return Particle.valueOf(components || this.legacy.length == 0 ? this.name() : this.legacy[0]);
    }
}
