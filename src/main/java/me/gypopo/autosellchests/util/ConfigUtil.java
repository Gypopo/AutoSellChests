package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUtil {

    public static void updateConfig(AutoSellChests plugin) {
        int configVer = Integer.parseInt(Config.get().getString("config-version", "1.0.0").replace(".", ""));
        if (configVer < Integer.parseInt(YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml"))).getString("config-version").replace(".", ""))) {
            Logger.info("Updating configs to newer version");

            if (configVer == 100) {
                int max = Config.get().getInt("player-max-sellchests");
                if (max != 0)
                    Config.get().set("max-sellchests.default", max);

                configVer = 101;
            }

            if (configVer == 101) {
                Config.get().set("compatibility-mode", true);

                configVer = 102;
            }

            if (configVer == 102) {
                plugin.newPriceFormat = false;

                configVer = 110;
            }

            if (configVer == 110) {
                Config.get().set("chest-holograms.display-range", 10);

                // Add the new item to the default settings menu
                File file = new File(plugin.getDataFolder(), "menus.yml");
                YamlConfiguration menuConfig = plugin.loadConfiguration(file, "menus.yml");

                // Only update if config is has default meny
                if (menuConfig.getInt("settings-menu.items.logging-item.slot") == 3 &&
                        menuConfig.getInt("settings-menu.items.rename-item.slot") == 7) {
                    // Add the toggle hologram item to the settings menu
                    ConfigurationSection item = menuConfig.createSection("settings-menu.items.hologram-item");
                    item.set("material", "ARMOR_STAND");
                    item.set("name", "%translations-toggle-chest-hologram%");
                    item.set("lore", Collections.singletonList("%translations-current-value%"));
                    item.set("slot", 5);
                    menuConfig.set("settings-menu.items.hologram-item", item);

                    menuConfig.set("settings-menu.items.logging-item.slot", 2);
                    menuConfig.set("settings-menu.items.rename-item.slot", 8);

                    ConfigUtil.save(menuConfig, file);
                }

                String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

                // Add the upgrade lvl name settings
                List<String> intervalUpgrades = new ArrayList<>(Config.get().getConfigurationSection("interval-upgrades").getKeys(false));
                for (int i = 0; i < intervalUpgrades.size(); i++) {
                    if (Config.get().getString("interval-upgrades." + intervalUpgrades.get(i) + ".lvl-name", null) == null)
                        Config.get().set("interval-upgrades." + intervalUpgrades.get(i) + ".lvl-name", romanNumerals[i]);
                }
                List<String> multiplierUpgrades = new ArrayList<>(Config.get().getConfigurationSection("multiplier-upgrades").getKeys(false));
                for (int i = 0; i < multiplierUpgrades.size(); i++) {
                    if (Config.get().getString("multiplier-upgrades." + multiplierUpgrades.get(i) + ".lvl-name", null) == null)
                        Config.get().set("multiplier-upgrades." + multiplierUpgrades.get(i) + ".lvl-name", romanNumerals[i]);
                }

                configVer = 120;
            }

            Config.get().set("config-version", getConfigVersion(configVer));
            Config.save();
            Config.reload();
        }
    }

    private static String getConfigVersion(int version) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < String.valueOf(version).toCharArray().length; i++) {
            if (String.valueOf(version).toCharArray().length != (i+1)) {
                builder.append(String.valueOf(version).toCharArray()[i]).append(".");
            } else {
                builder.append(String.valueOf(version).toCharArray()[i]);
            }
        }
        return builder.toString();
    }

    public static void save(FileConfiguration fileConfiguration, File file) {

        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

            // Load all comments
            Map<String, String> comments = getComments(input.lines().collect(Collectors.toList()));
            input.close();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

            for (String key : fileConfiguration.getKeys(true)) {
                String[] keys = key.split("\\.");
                String actualKey = keys[keys.length - 1];
                String comment = comments.remove(key);

                StringBuilder prefixBuilder = new StringBuilder();
                int indents = keys.length - 1;
                appendPrefixSpaces(prefixBuilder, indents);
                String prefixSpaces = prefixBuilder.toString();

                if (comment != null) {
                    writer.write(comment);//No \n character necessary, new line is automatically at end of comment
                }

                Object obj = fileConfiguration.get(key);


                if (obj instanceof ConfigurationSerializable) {
                    writer.write(prefixSpaces + actualKey + ": " + new Yaml().dump(((ConfigurationSerializable) obj).serialize()));
                } else if (obj instanceof String || obj instanceof Character) {
                    if (obj instanceof String) {
                        String s = (String) obj;
                        obj = s.replace("\n", "\\n");
                    }

                    writer.write(prefixSpaces + actualKey + ": " + new Yaml().dump(obj).replace("\n ", ""));
                } else if (obj instanceof List) {
                    writer.write(getListAsString((List) obj, actualKey, prefixSpaces));
                } else if (obj instanceof MemorySection) {
                    writer.write(prefixSpaces + actualKey + ":\n");
                } else {
                    writer.write(prefixSpaces + actualKey + ": " + new Yaml().dump(obj));
                }
            }

            String danglingComments = comments.get(null);

            if (danglingComments != null) {
                writer.write(danglingComments);
            }

            // Save it
            writer.close();

        } catch (IOException e) {
            AutoSellChests.getInstance().getLogger().warning("Error while saving " + file.getName());
            e.printStackTrace();
        }
    }

    //Key is the config key, value = comment
    //Parses comments, blank lines, and ignored sections
    private static Map<String, String> getComments(List<String> lines) {
        Map<String, String> comments = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        StringBuilder keyBuilder = new StringBuilder();
        int lastLineIndentCount = 0;

        for (String line : lines) {
            if (line != null && line.trim().startsWith("-"))
                continue;

            if (line == null || line.trim().equals("") || line.trim().startsWith("#")) {
                builder.append(line).append("\n");
            } else {
                lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount);

                if (keyBuilder.length() > 0) {
                    comments.put(keyBuilder.toString(), builder.toString());
                    builder.setLength(0);
                }
            }
        }

        if (builder.length() > 0) {
            comments.put(null, builder.toString());
        }

        return comments;
    }

    //Updates the keyBuilder and returns configLines number of indents
    private static int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
        int currentIndents = countIndents(configLine);
        String key = configLine.trim().split(":")[0];

        if (keyBuilder.length() == 0) {
            keyBuilder.append(key);
        } else if (currentIndents == lastLineIndentCount) {
            //Replace the last part of the key with current key
            removeLastKey(keyBuilder);

            if (keyBuilder.length() > 0) {
                keyBuilder.append(".");
            }

            keyBuilder.append(key);
        } else if (currentIndents > lastLineIndentCount) {
            //Append current key to the keyBuilder
            keyBuilder.append(".").append(key);
        } else {
            int difference = lastLineIndentCount - currentIndents;

            for (int i = 0; i < difference + 1; i++) {
                removeLastKey(keyBuilder);
            }

            if (keyBuilder.length() > 0) {
                keyBuilder.append(".");
            }

            keyBuilder.append(key);
        }

        return currentIndents;
    }

    private static String getListAsString(List list, String actualKey, String prefixSpaces) {
        StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

        if (list.isEmpty()) {
            builder.append(" []\n");
            return builder.toString();
        }

        builder.append("\n");

        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);

            if (o instanceof String || o instanceof Character) {
                String s = o.toString();
                if (s.contains("\"") || s.contains("'")) {
                    s = s.replace("'", "''");
                }
                builder.append(prefixSpaces).append("- '").append(s).append("'");
            } else if (o instanceof List) {
                builder.append(prefixSpaces).append("- ").append(new Yaml().dump(o));
            } else {
                builder.append(prefixSpaces).append("- ").append(o);
            }

            if (i != list.size()) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    //Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
    private static int countIndents(String s) {
        int spaces = 0;

        for (char c : s.toCharArray()) {
            if (c == ' ') {
                spaces += 1;
            } else {
                break;
            }
        }

        return spaces / 2;
    }

    //Ex. keyBuilder = key1.key2.key3 --> key1.key2
    private static void removeLastKey(StringBuilder keyBuilder) {
        String temp = keyBuilder.toString();
        String[] keys = temp.split("\\.");

        if (keys.length == 1) {
            keyBuilder.setLength(0);
            return;
        }

        temp = temp.substring(0, temp.length() - keys[keys.length - 1].length() - 1);
        keyBuilder.setLength(temp.length());
    }

    private static String getPrefixSpaces(int indents) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < indents; i++) {
            builder.append("  ");
        }

        return builder.toString();
    }

    private static void appendPrefixSpaces(StringBuilder builder, int indents) {
        builder.append(getPrefixSpaces(indents));
    }
}
