package me.gypopo.autosellchests;

import me.gypopo.autosellchests.commands.SellChestCommand;
import me.gypopo.autosellchests.database.SQLite;
import me.gypopo.autosellchests.events.PlayerListener;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.metrics.Metrics;
import me.gypopo.autosellchests.util.ConfigUtil;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.providers.EconomyProvider;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AutoSellChests extends JavaPlugin implements Listener {

    /**
     * Available versions: 18, 19, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119
     */
    public final int version = this.getVersion();

    private static AutoSellChests instance;
    public static AutoSellChests getInstance() {return instance; }

    private SQLite database;
    private final TimeUtils timeUtils = new TimeUtils();
    private final NamespacedKey key = new NamespacedKey(this, "autosell");
    private ChestManager manager;
    public boolean debug;

    @Override
    public void onEnable() {
        boolean premium;

        if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI") != null) {
            this.getLogger().info("Found EconomyShopGUI, enabling...");
            if (!this.validateAPI()) return;

            premium = false;
            int version = Integer.parseInt(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI").getDescription().getVersion().split("-")[0].replace(".", ""));
            if (version < 544) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI, please download version v5.4.4 or later, disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium") != null) {
            this.getLogger().info("Found EconomyShopGUI Premium, enabling...");
            if (!this.validateAPI()) return;

            premium = true;
            int version = Integer.parseInt(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium").getDescription().getVersion().split("-")[0].replace(".", ""));
            if (version < 490) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI Premium, please download version v4.9.0 or later, disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            this.getLogger().warning("Could not find EconomyShopGUI(Premium), disabling...");
            return;
        }
        instance = this;

        if (!this.isSpigotServer()) {
            this.getLogger().warning("It seems like you are using a server type which does not support spigot, please install a supported server type like Paper which can be downloaded from here: https://papermc.io/");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (this.checkForGson()) {
            Metrics metrics = new Metrics(this, 15605);
            metrics.addCustomChart(new Metrics.SimplePie("esgui_ver", () -> premium ? "EconomyShopGUI-Premium" : "EconomyShopGUI"));
        }

        Config.setup();
        Lang.reload();
        new Logger(this);
        ConfigUtil.updateConfig(this);

        this.database = new SQLite();
        if (!this.database.connect()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerCommands();
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(this, this);

        // Alternative to the 'softdepend' tag inside the plugin.yml which doesn't always work as intended
        this.runTaskLater(() -> {
            if (this.isPluginEnabled(premium)) {
                this.manager = new ChestManager(this);
            } else {
                this.getLogger().warning("Found EconomyShopGUI in a disabled state, please make sure it is enabled and up to date, disabling the plugin...");
                this.getServer().getPluginManager().disablePlugin(this);
            }
        }, 1);

        this.debug = Config.get().getBoolean("debug");
    }

    @Override
    public void onDisable() {
        if (this.manager != null) this.manager.disable();
        if (this.database != null) this.database.closeConnection();
    }

    private void registerCommands() {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            commandMap.register("autosellchests", new SellChestCommand(this));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.warn("Exception occurred registering commands.");
            e.printStackTrace();
        }
    }

    public TimeUtils getTimeUtils() {
        return this.timeUtils;
    }

    public SQLite getDatabase() {
        return this.database;
    }

    public NamespacedKey getKey() {
        return this.key;
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

    public BukkitTask runTaskAsync(Runnable runnable) {
        return this.getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public BukkitTask runTaskAsyncTimer(Runnable runnable, long delay, long period) {
        return this.getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, delay, period);
    }

    public void callEvent(Event event) {
        this.getServer().getPluginManager().callEvent(event);
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

    private boolean isPluginEnabled(boolean premium) {
        return this.getServer().getPluginManager().getPlugin("EconomyShopGUI" + (premium ? "-Premium" : "")).isEnabled();
    }

    private boolean validateAPI() {
        try {
            Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook");
            return true;
        } catch (ClassNotFoundException e) {
            this.getLogger().warning("Failed to integrate with EconomyShopGUI, disabling...");
            this.getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private boolean isSpigotServer() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public String formatPrice(EcoType econ, double price) {
        if (econ != null && (econ.getType().name().equalsIgnoreCase("ITEM") || econ.getType().name().equalsIgnoreCase("LEVELS") || econ.getType().name().equalsIgnoreCase("PLAYER_POINTS")))
            price = (double)Math.round(price);

        if (price == Math.floor(price)) {
            return (price == 1 ? EconomyShopGUIHook.getEcon(econ).getSingular() : EconomyShopGUIHook.getEcon(econ).getPlural())
                    .replace("%price%", String.format("%,.0f", price).replace("\u202F", ".").replace("\u00A0", "."));
        } else {
            return (price == 1 ? EconomyShopGUIHook.getEcon(econ).getSingular() : EconomyShopGUIHook.getEcon(econ).getPlural())
                    .replace("%price%", String.format("%,.2f", price).replace("\u202F", ".").replace("\u00A0", "."));
        }
    }

    public String formatPrices(EcoType type, Double price, String message) {
        return message != null ? message.replace("%profit%", this.formatPrice(type, price) + "§r") : this.formatPrice(type, price) + "§r";
    }

    public String formatPrices(Map<EcoType, Double> prices, String message) {
        StringBuilder builder = new StringBuilder();
        String color = message != null && message.contains("%profit%") ? ChatColor.getLastColors(message.split("%profit%")[0]) : null;

        int i = 0;
        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            builder.append(this.formatPrice(entry.getKey(), entry.getValue()));

            if (color != null) builder.append("§r").append(color);
            if (i != prices.size()-1) builder.append(", ");
            i++;
        }
        return color != null ? message.replace("%profit%", builder.toString()) : builder.toString();
    }

    public static List<String> splitLongString(String string) {
        List<String> list = new ArrayList<>();
        String[] words = string.split(" ");

        int e = 0;
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            e = e + (ChatColor.stripColor(word).length() + 1);
            if (e >= 42) {
                list.add(line.toString());
                e = word.length();
                line = new StringBuilder();
                line.append(ChatColor.getLastColors(list.get(list.size()-1)));
                // Add the first word of the new line
                line.append(word);
                continue;
            }
            if (line.length() != 0) line.append(" ");
            line.append(word);
        }
        list.addAll(Arrays.asList(line.toString().split("\n")));

        return list;
    }
}
