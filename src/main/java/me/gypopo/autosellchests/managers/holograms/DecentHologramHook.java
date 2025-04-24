package me.gypopo.autosellchests.managers.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.HologramProvider;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class DecentHologramHook implements HologramProvider {

    private static final String H_PREFIX = "ASC_";
    private final List<String> lines;
    private int tickLine; // The index of the hologram line which holds the %next-interval% ph

    public DecentHologramHook() {
        List<String> lines = Config.get().getStringList("chest-holograms.lines");

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("%next-interval%"))
                this.tickLine = i;
        }
        this.lines = lines.stream().map(s -> Lang.formatColors(s, null)).collect(Collectors.toList());
    }

    @Override
    public void loadHologram(Chest chest) {
        Hologram h = DHAPI.getHologram(H_PREFIX + chest.getId());
        if (h == null) {
            this.createHologram(chest);
            return;
        }

        h.enable();
    }

    private Hologram createHologram(Chest chest) {
        Hologram h;
        ChestLocation loc = chest.getLocation();
        if (loc.isDoubleChest()) {
            double x = loc.getLeftLocation().x;
            double z = loc.getLeftLocation().z;

            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;

            h = DHAPI.createHologram(H_PREFIX + chest.getId(),  new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 2, z + 0.5), false, this.getLines(chest));
        } else {
            h = DHAPI.createHologram(H_PREFIX + chest.getId(), new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 2, loc.getLeftLocation().z + 0.5), false, this.getLines(chest));
        }

        Logger.debug("Created hologram for chest " + chest.getId());
        h.enable();

        return h;
    }

    @Override
    public void unloadHologram(Chest chest) {
        Hologram h = DHAPI.getHologram(H_PREFIX + chest.getId());
        if (h != null)
            h.disable();
    }

    @Override
    public void updateHologram(Chest chest) {
        Hologram h = DHAPI.getHologram(H_PREFIX + chest.getId());

        int i = 0;
        for (String s : this.getLines(chest)) {
            h.getPage(0).setLine(i++, s);
        }

        Logger.debug("Updated hologram for chest " + chest.getId());
    }

    @Override
    public void updateHologramLocation(Chest chest) {
        ChestLocation loc = chest.getLocation();
        if (loc.isDoubleChest()) {
            double x = loc.getLeftLocation().x;
            double z = loc.getLeftLocation().z;

            if (x != loc.getRightLocation().x)
                x += x > loc.getRightLocation().x ? -0.5 : 0.5;
            if (z != loc.getRightLocation().z)
                z += z > loc.getRightLocation().z ? -0.5 : 0.5;

            DHAPI.moveHologram(H_PREFIX + chest.getId(), new Location(Bukkit.getWorld(loc.getLeftLocation().world), x + 0.5, loc.getLeftLocation().y + 2, z + 0.5));
        } else {
            DHAPI.moveHologram(H_PREFIX + chest.getId(), new Location(Bukkit.getWorld(loc.getLeftLocation().world), loc.getLeftLocation().x + 0.5, loc.getLeftLocation().y + 2, loc.getLeftLocation().z + 0.5));
        }

        Logger.debug("Updated hologram location for chest " + chest.getId());
    }

    @Override
    public void tickHologram(Chest chest, String interval) {
        Hologram h = DHAPI.getHologram(H_PREFIX + chest.getId());
        if (h == null)
            h = this.createHologram(chest); // Create if not created yet

        if (h.isDisabled())
            return; // If the hologram is unloaded

        DHAPI.setHologramLine(h, this.tickLine, this.lines.get(this.tickLine).replace("%next-interval%", interval));
    }

    @Override
    public void removeHologram(Chest chest) {
        DHAPI.removeHologram(H_PREFIX + chest.getId());
    }

    private ArrayList<String> getLines(Chest chest) {
        ArrayList<String> lines = new ArrayList<>(this.lines);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.contains("%next-interval%")) {
                lines.set(i, l.replace("%next-interval%", TimeUtils.getReadableTime(chest.getNextInterval() - System.currentTimeMillis())));
            } else if (l.contains("%chest-name%")) {
                lines.set(i, l.replace("%chest-name%", chest.getName()));
            }
        }

        return lines;
    }
}