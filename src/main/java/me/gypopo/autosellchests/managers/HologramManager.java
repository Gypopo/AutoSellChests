package me.gypopo.autosellchests.managers;

import com.gpplugins.gplib.holograms.HologramProvider;
import com.gpplugins.gplib.holograms.HologramProviders;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HologramManager {

    private static final String H_PREFIX = "ASC_";

    private final AutoSellChests plugin;
    private HologramProvider provider;
    private List<String> lines;
    private int tickLine; // The index of the hologram line which holds the %next-interval% ph
    private int displayRange;
    private double offsetY = 1.4D;

    public HologramManager(AutoSellChests plugin) {
        this.plugin = plugin;

        if (Config.get().getBoolean("chest-holograms.enabled")) {
            this.provider = HologramProviders.detect(plugin, plugin.getScheduler(), H_PREFIX);
        }

        if (this.provider == null) {
            Logger.info("Chest holograms are disabled");
            return;
        }

        List<String> lines = Config.get().getStringList("chest-holograms.lines");
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%next-interval%"))
                this.tickLine = i;
        }
        this.lines = lines.stream().map(s -> Lang.formatColors(s, null)).collect(Collectors.toList());
        this.displayRange = Config.get().getInt("chest-holograms.display-range", -1);
        this.offsetY = Config.get().getDouble("chest-holograms.offsetY", 1.4D);

        this.tickHolograms();
    }

    public boolean isEnabled() {
        return this.provider != null;
    }

    public void loadHologram(Chest chest) {
        if (this.provider != null)
            this.provider.upsert(H_PREFIX + chest.getId(), this.getLocation(chest), this.getLines(chest), this.displayRange);
    }

    public void updateHologram(Chest chest) {
        if (this.provider != null)
            this.provider.upsert(H_PREFIX + chest.getId(), this.getLocation(chest), this.getLines(chest), this.displayRange);
    }

    public void updateHologramLocation(Chest chest) {
        if (this.provider != null)
            this.provider.updateLocation(H_PREFIX + chest.getId(), this.getLocation(chest));
    }

    public void removeHologram(Chest chest) {
        if (this.provider != null)
            this.provider.delete(H_PREFIX + chest.getId());
    }

    private void tickHolograms() {
        long millis = 20L;
        try {
            millis = TimeUtils.getTime(Config.get().getString("chest-holograms.update-interval", "1s")) / 50;
        } catch (ParseException e) {
            Logger.warn("Failed to parse chest hologram update interval for " + Config.get().getString("chest-holograms.update-interval") + " with reason: " + e.getMessage());
            e.printStackTrace();
        }

        this.plugin.runTaskAsyncTimer(() -> {
            for (Chest c : this.plugin.getManager().getLoadedChests().values()) {
                if (!c.isLoaded() || !c.isHologram())
                    continue;

                this.provider.setLine(H_PREFIX + c.getId(), this.tickLine, this.lines.get(this.tickLine).replace("%next-interval%", this.getNextInterval(c)));
            }
        }, 0L, millis);
    }

    private String getNextInterval(Chest chest) {
        return TimeUtils.getReadableTime(chest.getNextInterval() - (System.currentTimeMillis() - 1000L));
    }

    private Location getLocation(Chest chest) {
        ChestLocation loc = chest.getLocation();
        double x = loc.getLeftLocation().x;
        double z = loc.getLeftLocation().z;

        if (loc.isDoubleChest()) {
            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;
        }

        return new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + this.offsetY, z + 0.5);
    }

    private ArrayList<String> getLines(Chest chest) {
        ArrayList<String> lines = new ArrayList<>(this.lines);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            lines.set(i, l.replace("%next-interval%", TimeUtils.getReadableTime(chest.getNextInterval() - (System.currentTimeMillis() - 1000L)))
                    .replace("%chest-name%", chest.getName())
                    .replace("%multiplier-name%", UpgradeManager.multiplierUpgrades ? UpgradeManager.getMultiplierUpgrade(chest.getMultiplierUpgrade()).getName() : "")
                    .replace("%multiplier-level%", UpgradeManager.multiplierUpgrades ? UpgradeManager.getMultiplierUpgrade(chest.getMultiplierUpgrade()).getLevelName() : "")
                    .replace("%interval-name%", UpgradeManager.intervalUpgrades ? UpgradeManager.getIntervalUpgrade(chest.getIntervalUpgrade()).getName() : "")
                    .replace("%interval-level%", UpgradeManager.intervalUpgrades ? UpgradeManager.getIntervalUpgrade(chest.getIntervalUpgrade()).getLevelName() : ""));
        }

        return lines;
    }
}
