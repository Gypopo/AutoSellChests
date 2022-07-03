package me.gypopo.autosellchests;

import me.gypopo.autosellchests.commands.SellChestCommand;
import me.gypopo.autosellchests.database.SQLite;
import me.gypopo.autosellchests.events.PlayerListener;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.metrics.Metrics;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.economyshopgui.EconomyShopGUI;
import me.gypopo.economyshopgui.providers.EconomyProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class AutoSellChests extends JavaPlugin {

    /**
     * Available versions: 18, 19, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119
     */
    public final int version = this.getVersion();

    private static AutoSellChests instance;
    public static AutoSellChests getInstance() {return instance; }

    private SQLite database;
    private final TimeUtils timeUtils = new TimeUtils();
    private EconomyProvider economy;
    private ChestManager manager;

    @Override
    public void onEnable() {
        boolean premium;

        if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI") != null && Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI").isEnabled()) {
            this.getLogger().info("Found EconomyShopGUI, enabling...");
            premium = false;
        } else  if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium") != null && Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium").isEnabled()) {
            this.getLogger().info("Found EconomyShopGUI Premium, enabling...");
            premium = true;
        } else {
            this.getLogger().warning("Could not find EconomyShopGUI(Premium), disabling... Please make sure EconomyShopGUI is enabled and up to date!");
            return;
        }
        instance = this;

        if (this.checkForGson()) {
            Metrics metrics = new Metrics(this, 15605);
            metrics.addCustomChart(new Metrics.SimplePie("esgui_ver", () -> premium ? "EconomyShopGUI-Premium" : "EconomyShopGUI"));
        }

        Config.setup();
        Lang.reload();
        new Logger(this);
        this.economy = EconomyShopGUI.getInstance().economy;

        this.database = new SQLite();
        if (!this.database.connect()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getCommand("autosellchests").setExecutor(new SellChestCommand(this));
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        this.manager = new ChestManager(this);
    }

    @Override
    public void onDisable() {
        this.manager.disable();
        this.database.closeConnection();
    }

    public EconomyProvider getEconomy() {
        return this.economy;
    }

    public TimeUtils getTimeUtils() {
        return this.timeUtils;
    }

    public SQLite getDatabase() {
        return this.database;
    }

    public ChestManager getManager() {
        return this.manager;
    }

    public void reloadManager() {
        this.manager.disable();
        this.manager = new ChestManager(this);
    }

    public BukkitTask runTaskTimer(Runnable runnable, long delay, long period) {
        return this.getServer().getScheduler().runTaskTimer(this, runnable, delay, period);
    }

    public BukkitTask runTaskLater(Runnable runnable, long delay) {
        return this.getServer().getScheduler().runTaskLater(this, runnable, delay);
    }

    public BukkitTask runTask(Runnable runnable) {
        return this.getServer().getScheduler().runTask(this, runnable);
    }

    private Integer getVersion() {
        String version = Bukkit.getBukkitVersion().split("-")[0];

        if (StringUtils.countMatches(version, ".") == 2) {
            return Integer.valueOf(version.substring(0, version.length() - 2).replace(".", ""));
        } else {
            return Integer.valueOf(version.replace(".", ""));
        }
    }

    public YamlConfiguration loadConfiguration(File file, String fileName) {
        Validate.notNull(file, "Cannot load " + fileName + " config file.");

        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            return config;
        } catch (FileNotFoundException e) {
            // Could not find or open the config file
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            this.getLogger().warning("Cannot read " + fileName + " config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it.");
            e.printStackTrace();
        }

        return null;
    }

    private boolean checkForGson() {
        String VersionName = getServer().getClass().getPackage().getName().split("\\.")[3];
        return !VersionName.equalsIgnoreCase("v1_8_R1") && !VersionName.equalsIgnoreCase("v1_8_R2");
    }
}
