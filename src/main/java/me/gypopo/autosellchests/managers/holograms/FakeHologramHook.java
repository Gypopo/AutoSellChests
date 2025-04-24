package me.gypopo.autosellchests.managers.holograms;

import me.gypopo.autosellchests.managers.HologramProvider;
import me.gypopo.autosellchests.objects.Chest;


/**
 * A hacky solution to not having to check for hologramProvider == null
 * Instead, use a dummy class which doesn't actually call any methods
 *
 * Plugin performance > Everything
 */
public class FakeHologramHook implements HologramProvider {

    @Override
    public void loadHologram(Chest chest) {

    }

    @Override
    public void unloadHologram(Chest chest) {

    }

    @Override
    public void updateHologram(Chest chest) {

    }

    @Override
    public void updateHologramLocation(Chest chest) {

    }

    @Override
    public void tickHologram(Chest chest, String interval) {

    }

    @Override
    public void removeHologram(Chest chest) {

    }
}