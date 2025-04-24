package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.objects.Chest;

public interface HologramProvider {

    // Enable or load the hologram for the chest
    void loadHologram(Chest chest);

    // Unload hologram if chest got unloaded
    void unloadHologram(Chest chest);

    // To update the whole hologram
    void updateHologram(Chest chest);

    // To move the hologram
    void updateHologramLocation(Chest chest);

    // To tick the interval timer of the hologram
    void tickHologram(Chest chest, String interval);

    // When a chest is removed
    void removeHologram(Chest chest);
}