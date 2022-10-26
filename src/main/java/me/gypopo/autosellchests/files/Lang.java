package me.gypopo.autosellchests.files;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.util.ConfigUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Lang {

    PLUGIN_PREFIX("#CDCDCD&lAuto#FF7070&lSell#CDCDCD&lChests &8>>"),

    INFO_SCREEN_TITLE("&8&lChest information"),
    SOLD_ITEMS_INFO("&c&lThis chest has sold &a%amount% &c&litem(s) in total."),
    INCOME_INFO("&c&lThis chest has sold items for &a$%profit%"),
    SELLCHEST_PLACED("&aYou have successfully placed a sell chest, left click it for more info."),
    SELLCHEST_BROKEN("&cYou have picked-up an #CDCDCDAuto#FF7070Sell#CDCDCDChest"),
    MAX_SELLCHESTS_REACHED("&cYou already have &a%maxSellChests% &cplaced and cannot place anymore"),
    ITEMS_SOLD_PLAYER_LOG("&a&lYour #CDCDCD&lAuto#FF7070&lSell#CDCDCD&lChest &a&lhas sold &6%amount% &a&litem(s) for &6%profit%"),
    ITEMS_SOLD_CONSOLE_LOG("AutoSellChest from player %player% on location %location% has sold %amount% item(s) for %profit%"),
    NO_PERMISSIONS("&cYou do not have permissions for that"),
    SELL_CHEST_GIVEN_LOG("%player_name% was given %amount% AutoSellChests"),
    PLAYER_SELL_CHEST_GIVEN("&cYou were given &7%amount% #CDCDCD&lAuto#FF7070Sell#CDCDCDChests"),
    NOT_ENOUGH_INVENTORY_SPACE("&cYou do not have enough space in your inventory"),
    PLACED_SELL_CHESTS_ACTION_BAR("&6&lPlaced sell chests: &c%amount%&c&4/&c%limit%"),
    PLACED_SELL_CHESTS_ACTION_BAR_MAX("∞"),
    PLACED_SELL_CHESTS_BOSS_BAR("&a&lPlaced sell chests:"),
    CANNOT_FORM_DOUBLE_CHEST("&cThe sell chest cannot be a placed against a non sell chest"),
    CANNOT_REMOVE_SELL_CHEST("&cYou do not have the required permissions to remove this sell chest"),

    // Chest information screen
    DESTROY_CHEST("&c&lPick up this #CDCDCD&lAuto#FF7070&lSell#CDCDCD&lChest"),

    // Sell chest block information
    SELL_CHEST_BLOCK_INFO("#FF7070&lChest info:"),
    SELL_CHEST_OWNER("&eOwner: #FF7070%player_name%"),
    SELL_CHEST_LOCATION("&eLocation: #FF7070%loc%"),
    SELL_CHEST_ID("&eChest ID: #FF7070%id%"),
    SELL_CHEST_NEXT_SELL("&eNext sell interval: #FF7070%time%"),
    ;

    private String def;

    private static File configFile;
    private static FileConfiguration config;
    private static AutoSellChests plugin;

    private static Map<String, String> messages = new HashMap<>();

    private static final Pattern rgbPattern = Pattern.compile("#[a-fA-F0-9]{6}");

    private Lang(String def) {
        this.def = def;
    }

    public static void reload() {
        if (!messages.isEmpty()) messages.clear();
        plugin = AutoSellChests.getInstance();
        configFile = new File(plugin.getDataFolder(), "lang.yml");
        loadLanguageFile();
    }

    public String getKey() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-").replace("0",",");
    }

    public void clearMessages() {
        messages.clear();
    }

    public String get() {
        String key = getKey();
        if (messages.containsKey(key)) {
            return messages.get(key);
        } else {
            String value;
            // If config doesn't contain the key, return the default one
            try {
                value = config.getString(key, def);
            } catch (NullPointerException npe) {
                value = def;
            }
            value = formatColors(value);
            messages.put(key, value);
            return value;
        }
    }

    public static String formatColors(String value) {
        if (plugin.version >= 116) {
            Matcher matcher = rgbPattern.matcher(value);
            while (matcher.find()) {
                String hex = value.substring(matcher.start(), matcher.end());
                value = value.replace(hex, net.md_5.bungee.api.ChatColor.of(hex) + "");
                matcher = rgbPattern.matcher(value);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', value).replace("\\n", "\n");
    }

    private static void loadLanguageFile() {

        File lang = new File(plugin.getDataFolder(), "lang.yml");
        if (lang.exists()) {
            // Make sure it is up to date
            ConfigUtil.save(plugin.loadConfiguration(configFile, "lang.yml"), new File(plugin.getDataFolder(), "lang.yml"));

            // Load updated config
            config = plugin.loadConfiguration(new File(plugin.getDataFolder(), "lang.yml"), "lang.yml");
        } else AutoSellChests.getInstance().saveResource("lang.yml", false);

    }
}