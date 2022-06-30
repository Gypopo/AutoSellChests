package me.gypopo.autosellchestsaddon.files;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.util.ConfigUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class Config {

    private static File file;
    private static FileConfiguration config;
    private static final String fileName = "config.yml";

    public static boolean setup() {
        file = new File(AutosellChests.getInstance().getDataFolder(), fileName);

        reload();

        return config != null;
    }

    public static FileConfiguration get(){
        return config;
    }

    public static void save() {
        ConfigUtil.save(config, file);
    }

    public static void reload(){
        if (file.exists()) {

            // The edited config inside the data folder
            final FileConfiguration c = AutosellChests.getInstance().loadConfiguration(file, fileName);
            if (c == null) return;

            // Copy the default file inside the jar with all comments
            AutosellChests.getInstance().saveResource(fileName, true);

            // New config in the datafolder
            FileConfiguration conf = AutosellChests.getInstance().loadConfiguration(file, fileName);

            // Set the settings back
            for (String str : c.getKeys(false)) {
                conf.set(str, c.get(str));
            }

            // Save the old settings to the file
            ConfigUtil.save(conf, file);

            // Reload the config
            config = conf;

        } else {
            AutosellChests.getInstance().saveResource(fileName, false);
            // Set the config
            config = AutosellChests.getInstance().loadConfiguration(file, fileName);
        }
    }
}