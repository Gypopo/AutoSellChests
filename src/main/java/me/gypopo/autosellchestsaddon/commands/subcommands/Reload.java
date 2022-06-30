package me.gypopo.autosellchestsaddon.commands.subcommands;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.commands.SubCommad;
import me.gypopo.autosellchestsaddon.files.Config;
import me.gypopo.autosellchestsaddon.files.Lang;
import me.gypopo.autosellchestsaddon.util.Logger;

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
            AutosellChests.getInstance().reloadManager();
            Logger.sendMessage(logger, "Reloaded successful, took " + (System.currentTimeMillis()-start) + "ms to complete");
        }

    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        return null;
    }
}
