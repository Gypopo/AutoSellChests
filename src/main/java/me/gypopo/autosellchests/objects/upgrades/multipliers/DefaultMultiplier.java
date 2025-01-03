package me.gypopo.autosellchests.objects.upgrades.multipliers;

import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;

public class DefaultMultiplier implements PriceMultiplier {

    private final double multiplier;

    public DefaultMultiplier() {
        this.multiplier = 1d;
    }

    @Override
    public double getMultiplier() {
        return this.multiplier;
    }
}