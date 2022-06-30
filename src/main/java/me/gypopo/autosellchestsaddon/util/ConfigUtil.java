package me.gypopo.autosellchestsaddon.util;

import me.gypopo.autosellchestsaddon.AutosellChests;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUtil {

    public static boolean loadConfig() {
        if (new File(AutosellChests.getInstance().getDataFolder(), "config.yml").exists()) {
            try {

                // The edited config inside the data folder
                final FileConfiguration config = AutosellChests.getInstance().getConfig();

                // Load all default data
                BufferedReader input = new BufferedReader(new InputStreamReader(AutosellChests.getInstance().getResource("config.yml"), StandardCharsets.UTF_8));
                List<String> defaults = input.lines().collect(Collectors.toList());
                input.close();

                // Make string from default data
                StringBuilder builder = new StringBuilder();
                for (String s : defaults) {
                    builder.append(s).append("\n");
                }

                // Save default data
                Files.write(Paths.get(AutosellChests.getInstance().getDataFolder() + "/config.yml"), builder.toString().getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                // Config as file
                final File file = new File(AutosellChests.getInstance().getDataFolder(), "config.yml");

                // Load the new config
                AutosellChests.getInstance().reloadConfig();
                // New config in the datafolder
                FileConfiguration conf = AutosellChests.getInstance().getConfig();

                // Set the settings back
                for (String str : config.getKeys(false)) {
                    conf.set(str, config.get(str));
                }

                save(conf, file);

                // Load the new config
                AutosellChests.getInstance().reloadConfig();
            } catch (IOException e) {
                // Cannot load config
                AutosellChests.getInstance().getLogger().warning("Cannot read config.yml config because it is mis-configured, use a online Yaml parser with the error underneath here to find out the cause of the problem and to solve it.");
                e.printStackTrace();
                return false;
            }
        } else {
            AutosellChests.getInstance().saveDefaultConfig();
        }
        return true;
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
            AutosellChests.getInstance().getLogger().warning("Error while saving " + file.getName());
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
