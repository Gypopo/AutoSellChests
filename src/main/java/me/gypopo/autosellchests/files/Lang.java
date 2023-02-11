package me.gypopo.autosellchests.files;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.util.ConfigUtil;
import me.gypopo.autosellchests.util.Gradient;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Lang {

    PLUGIN_PREFIX("#FF7070&lSell#CDCDCD&lChests &8>>"),

    SELLCHEST_PLACED("&aYou have successfully placed a %chest-name%, left click it for more info."),
    SELLCHEST_BROKEN("&cYou have picked-up an %chest-name%"),
    MAX_SELLCHESTS_REACHED("&cYou already have &a%maxSellChests% &cplaced and cannot place anymore"),
    ITEMS_SOLD_PLAYER_LOG("&a&lYour %chest-name% &a&lhas sold &6%amount% &a&litem(s) for &6%profit%"),
    ITEMS_SOLD_CONSOLE_INTERVAL("Completed %count% sell interval(s) for %chests% loaded sell chest(s) and sold %items% item(s) in the last %interval%."),
    ITEMS_SOLD_CONSOLE_LOG("Sell chest with id '%id%' from player %player% on location %location% has sold %amount% item(s) for %profit%"),
    NO_PERMISSIONS("&cYou do not have permissions for that"),
    SELL_CHEST_GIVEN_LOG("%player_name% was given %amount% SellChests"),
    SELL_CHEST_MULTIPLIER_GIVEN_LOG("%player_name% was given %amount% SellChests with a %multiplier%% sell multiplier"),
    PLAYER_SELL_CHEST_GIVEN("&cYou were given &7%amount% #FF7070Sell#CDCDCDChests"),
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

    // Info menu
    INFO_SCREEN_TITLE("&8&lChest information"),
    SOLD_ITEMS_INFO("&c&lTotal items sold: &a%amount%"),
    INCOME_INFO("&c&lTotal profit made: &a%profit%"),
    CLAIM_ABLE_INFO( "&a&lClick to view unclaimed profits."),
    SELL_CHEST_SETTINGS("&c&lChest settings"),

    // Profits claim menu
    AVAILABLE_PROFIT_MENU_TITLE("&8&lAvailable profits"),
    AVAILABLE_PROFIT("&6&lClick to claim &a%profit%&6&l."),
    CANNOT_CLAIM_PROFIT("&cUnable to claim this right now."),

    // Settings menu
    CHEST_SETTINGS_TITLE("&8&lChest settings"),
    TOGGLE_SOLD_ITEMS_LOGGING("&c&lToggle sold item logging"),
    CURRENT_VALUE("&eCurrent value: &a%value%"),
    CHANGE_CHEST_NAME("&c&lClick to update the name"),
    CURRENT_DISPLAYNAME("&eCurrent: &a%chest-name%"),

    // Enter name menu
    ENTER_NAME_MENU_TITLE("&8&lEnter new name"),
    ;

    private String def;

    private static File configFile;
    private static FileConfiguration config;
    private static AutoSellChests plugin;

    private static Map<String, String> messages = new HashMap<>();

    private static final Pattern rgbPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Pattern startGradientPattern = Pattern.compile("<gradient:#[a-fA-F0-9]{6}>");
    private static final Pattern endGradientPattern = Pattern.compile("</gradient:#[a-fA-F0-9]{6}>");

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
            value = formatColors(value, this.findGradient(value));

            messages.put(key, value);
            return value;
        }
    }

    public String raw() {
        // If config doesn't contain the key, return the default one
        try {
            return config.getString(this.getKey(), def);
        } catch (NullPointerException npe) {
            return def;
        }
    }

    private Gradient findGradient(String value) {
        Matcher matcher = startGradientPattern.matcher(value);

        if (matcher.find()) {
            String g1 = value.substring(matcher.start(), matcher.end());
            matcher = endGradientPattern.matcher(value);
            if (matcher.find()) {
                String g2 = value.substring(matcher.start(), matcher.end());
                return new Gradient(g1, g2, value);
            } else return null; // No gradient defined
        } else return null; // No gradient defined
    }

    public static String formatColors(String value, Gradient gradient) {
        if (plugin.version >= 116) {
            if (gradient != null)
                value = gradient.get();
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
        } else AutoSellChests.getInstance().saveResource("lang.yml", false);

        config = plugin.loadConfiguration(new File(plugin.getDataFolder(), "lang.yml"), "lang.yml");
    }
}