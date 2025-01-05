package me.gypopo.autosellchests.commands.subcommands;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.commands.SubCommad;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.util.Logger;

import java.util.List;

public class Reload implements SubCommad {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload the plugin";
    }

    @Override
    public String getSyntax() {
        return "/asc reload";
    }

    @Override
    public void perform(Object logger, String[] args) {

        if (args.length >= 1) {
            long start = System.currentTimeMillis();
            Config.reload();
            Lang.reload();
            AutoSellChests.getInstance().reloadManager();
            Logger.sendMessage(logger, "Reloaded successful, took " + (System.currentTimeMillis()-start) + "ms to complete");
        }

    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        return null;
    }
}
