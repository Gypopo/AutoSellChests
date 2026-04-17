package me.gypopo.autosellchests.managers.holograms;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Display.CMIBillboard;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.HologramProvider;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CMIHologramHook implements HologramProvider {

    private final AutoSellChests plugin;
    private static final String H_PREFIX = "ASC_";
    private final List<String> lines;
    private final int display_range;
    private int tickLine; // The index of the hologram line which holds the %next-interval% ph

    public CMIHologramHook(AutoSellChests plugin) {
        this.plugin = plugin;

        List<String> lines = Config.get().getStringList("chest-holograms.lines");

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%next-interval%"))
                this.tickLine = i;
        }
        this.lines = lines.stream().map(s -> Lang.formatColors(s, null)).collect(Collectors.toList());

        this.display_range = Config.get().getInt("chest-holograms.display-range");
    }

    @Override
    public void loadHologram(Chest chest) {
        this.plugin.runTaskAsync(() -> {
            CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
            if (h == null) {
                this.createHologram(chest);
                return;
            }

            h.enable();
        });
    }

    private CMIHologram createHologram(Chest chest) {
        Location location;
        ChestLocation loc = chest.getLocation();
        if (loc.isDoubleChest()) {
            double x = loc.getLeftLocation().x;
            double z = loc.getLeftLocation().z;

            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;

            location = new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 2, z + 0.5);
        } else {
            location = new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 2, loc.getLeftLocation().z + 0.5);
        }

        CMIHologram h = new CMIHologram(H_PREFIX + chest.getId(), location);
        if (this.plugin.version > 118) {
            h.setNewDisplayMethod(true);
            h.setBillboard(CMIBillboard.CENTER);
        }

        h.setLines(this.getLines(chest));
        h.setBackgroundAlpha(50);
        h.setSeeThrough(true);
        h.setShowRange(this.display_range);

        h.enable();

        CMI.getInstance().getHologramManager().addHologram(h);

        return h;
    }

    @Override
    public void unloadHologram(Chest chest) {
        CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
        if (h != null)
            h.disable();
    }

    @Override
    public void updateHologram(Chest chest) {
        this.plugin.runTaskAsync(() -> {
            CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
            if (h != null) {
                h.setLines(this.getLines(chest));
                h.update();
            }

            Logger.debug("Updated hologram for chest " + chest.getId());
        });
    }

    @Override
    public void updateHologramLocation(Chest chest) {
        this.plugin.runTaskAsync(() -> {
            CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
            if (h == null)
                return;

            ChestLocation loc = chest.getLocation();
            if (loc.isDoubleChest()) {
                double x = loc.getLeftLocation().x;
                double z = loc.getLeftLocation().z;

                if (x != loc.getRightLocation().x)
                    x += x > loc.getRightLocation().x ? -0.5 : 0.5;
                if (z != loc.getRightLocation().z)
                    z += z > loc.getRightLocation().z ? -0.5 : 0.5;

                h.setLoc(new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 2, z + 0.5));
            } else {
                h.setLoc(new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 2, loc.getLeftLocation().z + 0.5));
            }

            h.update();
            Logger.debug("Updated hologram location for chest " + chest.getId());
        });
    }

    @Override
    public void tickHologram(Chest chest, String interval) {
        // Do not tick holograms async to prevent creating a new task each time which may be more performance heavy? (I did not test this)
        CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
        if (h != null) {
            h.setLine(this.tickLine, this.lines.get(this.tickLine).replace("%next-interval%", interval));
            h.update();
        }
    }

    @Override
    public void removeHologram(Chest chest) {
        CMIHologram h = CMI.getInstance().getHologramManager().getByName(H_PREFIX + chest.getId());
        if (h != null) {
            h.disable();
            h.remove();
        }
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