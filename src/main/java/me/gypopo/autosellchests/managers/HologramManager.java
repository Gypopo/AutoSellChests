package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.managers.holograms.CMIHologramHook;
import me.gypopo.autosellchests.managers.holograms.DecentHologramHook;
import me.gypopo.autosellchests.managers.holograms.FakeHologramHook;
import me.gypopo.autosellchests.managers.holograms.FancyHologramHook;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.plugin.PluginManager;

import java.text.ParseException;

public class HologramManager {

    private final AutoSellChests plugin;
    private HologramProvider provider;

    public HologramManager(AutoSellChests plugin) {
        this.plugin = plugin;

        if (Config.get().getBoolean("chest-holograms.enabled")) {
            PluginManager pm = plugin.getServer().getPluginManager();
            if (pm.getPlugin("DecentHolograms") != null) {
                this.provider = new DecentHologramHook();
            } else if (pm.getPlugin("FancyHolograms") != null) {
                this.provider = new FancyHologramHook();
            } else if (pm.getPlugin("CMI") != null) {
                this.provider = new CMIHologramHook(plugin);
            }
        }

        if (this.provider == null) {
            Logger.info("Failed to find a supported hologram provider, disabling holograms...");
            this.provider = new FakeHologramHook();
        } else {
            Logger.info("Enabled holograms hook: " + (this.provider instanceof DecentHologramHook ? "DecentHolograms" : this.provider instanceof FancyHologramHook ? "FancyHolograms" : "CMI"));
        }

        if (!(this.provider instanceof FakeHologramHook))
            this.tickHolograms();
    }

    public boolean isEnabled() {
        return !(this.provider instanceof FakeHologramHook);
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
                if (!c.isLoaded() || !c.isHologram())
                    continue;

                this.provider.tickHologram(c, this.getNextInterval(c));
            }
        }, 0L, 20L);
    }

    private String getNextInterval(Chest chest) {
        return TimeUtils.getReadableTime(chest.getNextInterval() - (System.currentTimeMillis() - 1000L));
    }
}