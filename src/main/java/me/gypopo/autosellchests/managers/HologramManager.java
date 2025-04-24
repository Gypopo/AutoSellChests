package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.managers.holograms.DecentHologramHook;
import me.gypopo.autosellchests.managers.holograms.FakeHologramHook;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.TimeUtils;

public class HologramManager {

    private final AutoSellChests plugin;
    private HologramProvider provider;

    public HologramManager(AutoSellChests plugin) {
        this.plugin = plugin;

        if (plugin.getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            this.provider = new DecentHologramHook();
        } else
            this.provider = new FakeHologramHook();

        if (Config.get().getBoolean("chest-holograms.enabled") &&
                !(this.provider instanceof FakeHologramHook))
            this.tickHolograms();
    }

    public void loadHologram(Chest chest) {
        this.provider.loadHologram(chest);
    }

    public void updateHologram(Chest chest) {
        this.provider.updateHologram(chest);
    }

    public void updateHologramLocation(Chest chest) {
        this.provider.updateHologramLocation(chest);
    }

    public void removeHologram(Chest chest) {
        this.provider.removeHologram(chest);
    }

    private void tickHolograms() {
        this.plugin.runTaskAsyncTimer(() -> {
            for (Chest c : this.plugin.getManager().getLoadedChests().values()) {
                if (!c.isLoaded())
                    continue;

                this.provider.tickHologram(c, this.getNextInterval(c));
            }
        }, 0L, 20L);
    }

    private String getNextInterval(Chest chest) {
        return TimeUtils.getReadableTime(chest.getNextInterval() - System.currentTimeMillis());
    }
}