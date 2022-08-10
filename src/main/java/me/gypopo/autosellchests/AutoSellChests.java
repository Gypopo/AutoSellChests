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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class AutoSellChests extends JavaPlugin implements Listener {

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

        if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI") != null) {
            this.getLogger().info("Found EconomyShopGUI, enabling...");
            premium = false;
            int version = Integer.parseInt(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI").getDescription().getVersion().split("-")[0].replace(".", ""));
            if (version < 481) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI, please download version v4.8.1 or later, disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else  if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium") != null) {
            this.getLogger().info("Found EconomyShopGUI Premium, enabling...");
            premium = true;
            int version = Integer.parseInt(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium").getDescription().getVersion().split("-")[0].replace(".", ""));
            if (version < 311) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI, please download version v3.1.1 or later, disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            this.getLogger().warning("Could not find EconomyShopGUI(Premium), disabling...");
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

        this.database = new SQLite();
        if (!this.database.connect()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getCommand("autosellchests").setExecutor(new SellChestCommand(this));
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(this, this);

        // Alternative to the 'softdepend' tag inside the plugin.yml which doesn't always work as intended
        this.runTaskLater(() -> {
            if (this.economy != null) {
                this.manager = new ChestManager(this);
            } else {
                this.getLogger().warning("Found EconomyShopGUI in a disabled state, please make sure it is enabled and up to date, disabling the plugin...");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }, 1);
    }

    @Override
    public void onDisable() {
        if (this.manager != null) this.manager.disable();
        if (this.database != null) this.database.closeConnection();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        // Sometimes bukkit's plugin load order is weird and EconomyShopGUI loads after AutoSellChests even if its a load before
        if (e.getPlugin().getName().equals("EconomyShopGUI") || e.getPlugin().getName().equals("EconomyShopGUI-Premium")) {
            this.economy = EconomyShopGUI.getInstance().economy;
        }
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

    public String formatPrice(double price) {
        return String.valueOf(String.format("%,.2f", price));
    }
}
