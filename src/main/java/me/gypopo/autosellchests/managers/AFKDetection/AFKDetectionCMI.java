package me.gypopo.autosellchests.managers.AFKDetection;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Afk.AfkManager;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.managers.AFKManager;
import me.gypopo.autosellchests.util.Logger;

import java.util.UUID;

public class AFKDetectionCMI implements AFKManager {

    private final AfkManager manager;

    public AFKDetectionCMI(AutoSellChests plugin) {
        this.manager = CMI.getInstance().getAfkManager();
        Logger.debug("Enabled AFK detection provider 'CMI'");
    }

    @Override
    public boolean isAFK(UUID uuid) {
        return this.manager.isAfk(uuid);
    }
}
