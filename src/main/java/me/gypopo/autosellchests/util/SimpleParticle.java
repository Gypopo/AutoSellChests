package me.gypopo.autosellchests.util;

import org.bukkit.Particle;

public enum SimpleParticle {

    WITCH("SPELL_WITCH"),
    REDSTONE(),
    CLOUD(),
    ;

    private final String[] legacy;

    SimpleParticle(String... legacy) {
        this.legacy = legacy;
    }

    public Particle get() {
        return Particle.valueOf(this.legacy.length == 0 ? this.name() : this.legacy[0]);
    }
}
