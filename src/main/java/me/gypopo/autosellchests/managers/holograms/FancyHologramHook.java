package me.gypopo.autosellchests.managers.holograms;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
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
import org.bukkit.entity.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FancyHologramHook implements HologramProvider {

    private static final String H_PREFIX = "ASC_";
    private final HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
    private final List<String> lines;
    private final int display_range;
    private int tickLine; // The index of the hologram line which holds the %next-interval% ph

    public FancyHologramHook() {
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
        Hologram h = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (h == null) {
            this.createHologram(chest);
            return;
        }

        if (h.getData() instanceof TextHologramData textData) {
            textData.setText(this.getLines(chest));
            h.queueUpdate();
        }
    }

    private Hologram createHologram(Chest chest) {
        Location location;
        ChestLocation loc = chest.getLocation();
        if (loc.isDoubleChest()) {
            double x = loc.getLeftLocation().x;
            double z = loc.getLeftLocation().z;

            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;

            location = new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 1.35, z + 0.5);
        } else {
            location = new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 1.35, loc.getLeftLocation().z + 0.5);
        }

        TextHologramData data = new TextHologramData(H_PREFIX + chest.getId(), location);
        if (AutoSellChests.getInstance().version > 118)
            data.setBillboard(Display.Billboard.CENTER);

        data.setText(this.getLines(chest));
        //data.setBackground(Color.fromARGB(50, Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue()));
        data.setSeeThrough(true);
        data.setPersistent(false);
        data.setVisibilityDistance(this.display_range);

        Hologram hologram = this.manager.create(data);
        this.manager.addHologram(hologram);

        return hologram;
    }

    @Override
    public void unloadHologram(Chest chest) {
        Hologram hologram = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (hologram == null)
            return;

        this.manager.removeHologram(hologram);
    }

    @Override
    public void updateHologram(Chest chest) {
        Hologram hologram = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (hologram == null)
            return;

        if (hologram.getData() instanceof TextHologramData textData) {
            textData.setText(this.getLines(chest));
            hologram.queueUpdate();
        }

        Logger.debug("Updated hologram for chest " + chest.getId());
    }

    @Override
    public void updateHologramLocation(Chest chest) {
        Hologram hologram = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (hologram == null)
            return;

        ChestLocation loc = chest.getLocation();
        if (loc.isDoubleChest()) {
            double x = loc.getLeftLocation().x;
            double z = loc.getLeftLocation().z;

            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;

            hologram.getData().setLocation(new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 1.35, z + 0.5));
        } else {
            hologram.getData().setLocation(new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 1.35, loc.getLeftLocation().z + 0.5));
        }

        hologram.queueUpdate();
        Logger.debug("Updated hologram location for chest " + chest.getId());
    }

    @Override
    public void tickHologram(Chest chest, String interval) {
        Hologram hologram = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (hologram == null)
            return;

        if (hologram.getData() instanceof TextHologramData textData) {
            List<String> text = textData.getText();
            text.set(this.tickLine, this.lines.get(this.tickLine).replace("%next-interval%", interval));
            textData.setText(text);
            hologram.queueUpdate();
        }
    }

    @Override
    public void removeHologram(Chest chest) {
        Hologram hologram = this.manager.getHologram(H_PREFIX + chest.getId()).orElse(null);
        if (hologram == null)
            return;

        this.manager.removeHologram(hologram);
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