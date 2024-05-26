package me.gypopo.autosellchests.managers.AFKDetection;

import com.earth2me.essentials.Essentials;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.managers.AFKManager;
import me.gypopo.autosellchests.util.Logger;

import java.util.UUID;

public class AFKDetectionEssentials implements AFKManager {

    private final Essentials ess;

    public AFKDetectionEssentials(AutoSellChests plugin) {
        this.ess = (Essentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        Logger.debug("Enabled AFK detection provider 'Essentials'");
    }

    @Override
    public boolean isAFK(UUID uuid) {
        return this.ess.getUser(uuid).isAfk();
    }
}
