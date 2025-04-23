package me.gypopo.autosellchests;

import me.gypopo.autosellchests.commands.SellChestCommand;
import me.gypopo.autosellchests.database.SQLite;
import me.gypopo.autosellchests.events.PlayerListener;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.AFKDetection.AFKDetectionCMI;
import me.gypopo.autosellchests.managers.AFKDetection.AFKDetectionEssentials;
import me.gypopo.autosellchests.managers.AFKManager;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.InventoryManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.metrics.Metrics;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.*;
import me.gypopo.autosellchests.util.scheduler.ServerScheduler;
import me.gypopo.autosellchests.util.scheduler.Task;
import me.gypopo.autosellchests.util.scheduler.schedulers.BukkitScheduler;
import me.gypopo.autosellchests.util.scheduler.schedulers.FoliaScheduler;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.events.ShopItemsLoadEvent;
import me.gypopo.economyshopgui.util.EcoType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AutoSellChests extends JavaPlugin implements Listener {

    /**
     * Available versions: 18, 19, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119
     */
    public final int version = this.getVersion();

    private static AutoSellChests instance;
    public static AutoSellChests getInstance() {return instance; }

    private final SQLite database = new SQLite(this);
    private final TimeUtils timeUtils = new TimeUtils();
    private final NamespacedKey key = new NamespacedKey(this, "autosell");
    private final ServerScheduler scheduler = this.getScheduler();
    private InventoryManager inventoryManager;
    private UpgradeManager upgradeManager;
    private ChestManager manager;
    private AFKManager afkManager;
    private boolean ready = false;
    public boolean newPriceFormat = true; // New format to store prices in databases, as of ASC v2.7.0
    public boolean supportsNewAPI; // Whether we can use EconomyShopGUI API v1.7.0+
    public boolean debug;

    @Override
    public void onEnable() {
        instance = this;
        boolean premium; // Whether we use the premium version of the plugin

        Version version;
        if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI") != null) {
            this.getLogger().info("Found EconomyShopGUI, enabling...");
            if (!this.validateAPI()) return;

            premium = false;
            version = new Version(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI").getDescription().getVersion());
            supportsNewAPI = version.isGreater(new Version("6.2.5"));
            if (version.isSmaller(new Version("5.5.1"))) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI, please download version v5.5.1 or later, found v" + version.getVer() + ", disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else if (Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium") != null) {
            this.getLogger().info("Found EconomyShopGUI Premium, enabling...");
            if (!this.validateAPI()) return;

            premium = true;
            version = new Version(Bukkit.getServer().getPluginManager().getPlugin("EconomyShopGUI-Premium").getDescription().getVersion());
            supportsNewAPI = version.isGreater(new Version("5.5.3"));
            if (version.isSmaller(new Version("4.10.2"))) {
                this.getLogger().warning("This plugin requires a newer version of EconomyShopGUI Premium, please download version v4.10.2 or later, found v" + version.getVer() + ", disabling...");
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            this.getLogger().warning("Could not find EconomyShopGUI(Premium), disabling...");
            return;
        }

        if (!this.isSpigotServer()) {
            this.getLogger().warning("It seems like you are using a server type which does not support spigot, please install a supported server type like Paper which can be downloaded from here: https://papermc.io/");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Metrics metrics = new Metrics(this, 15605);
        metrics.addCustomChart(new Metrics.SimplePie("esgui_ver", () -> premium ? "EconomyShopGUI-Premium" : "EconomyShopGUI"));

        if (supportsNewAPI) {
            this.getLogger().info("Using new API methods for improved performance...");
        }

        Config.setup();
        Lang.reload();
        new Logger(this);
        ConfigUtil.updateConfig(this);

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
                if (Config.get().getBoolean("afk-prevention", false))
                    this.afkManager = this.getAfkManager();
                this.upgradeManager = new UpgradeManager(this);
                this.manager = new ChestManager(this);
                this.inventoryManager = new InventoryManager();
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

    public AFKManager getAFKManager() {
        return this.afkManager;
    }

    public InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    public void reloadManager() {
        this.manager.disable();
        if (Config.get().getBoolean("afk-prevention", false))
            this.afkManager = this.getAfkManager();
        this.upgradeManager.reload();
        this.inventoryManager.reload();
        this.manager = new ChestManager(this);
    }

    public Task runTaskTimer(Runnable runnable, long delay, long period) {
        return this.scheduler.runTaskTimer(this, runnable, delay, period);
    }

    public void runTaskLater(Runnable runnable, long delay) {
        this.scheduler.runTaskLater(this, runnable, delay);
    }

    public void runTaskLater(Runnable runnable, Location loc, long delay) {
        this.scheduler.runTaskLater(this, loc, runnable, delay);
    }

    public void runTask(Runnable runnable) {
        this.scheduler.runTask(this, runnable);
    }

    public void runTask(Chest chest, Runnable runnable) {
        this.scheduler.runTask(this, chest, runnable);
    }

    public void runTaskAsync(Runnable runnable) {
        this.scheduler.runTaskAsync(this, runnable);
    }

    public Task runTaskAsyncTimer(Runnable runnable, long delay, long period) {
        return this.scheduler.runTaskAsyncTimer(this, runnable, delay, period);
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

    private boolean isPluginEnabled(boolean premium) {
        return this.getServer().getPluginManager().getPlugin("EconomyShopGUI" + (premium ? "-Premium" : "")).isEnabled();
    }

    @EventHandler
    public void onShopItemsLoadEvent(final ShopItemsLoadEvent e) {
        this.ready = true;
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
        String color = message != null && message.contains("%profit%") ? ChatColor.getLastColors(message.split("%profit%")[0]) : null;
        return this.formatPrice(type, price) + "§r" + (color == null ? "" : color);
    }

    // Replacing a placeholder with specific colors already in it, will also update the message its color codes, so reset it back
    public String replaceColoredPlaceholder(String message, String placeholder, String replacement) {
        String c = ChatColor.getLastColors(message.split(placeholder)[0]);
        return message.replace(placeholder, replacement + "§r" + c);
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
        return builder.toString();
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

    private AFKManager getAfkManager() {
        PluginManager pm = this.getServer().getPluginManager();

        if (pm.getPlugin("Essentials") != null) {
            return new AFKDetectionEssentials(this);
        } else if (pm.getPlugin("CMI") != null) {
            return new AFKDetectionCMI(this);
        }

        return null;
    }

    private ServerScheduler getScheduler() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return new FoliaScheduler(this);
        } catch (ClassNotFoundException e) {
            return new BukkitScheduler(this);
        }
    }
}
