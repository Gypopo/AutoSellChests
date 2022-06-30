package me.gypopo.autosellchestsaddon.util;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.files.Config;
import me.gypopo.autosellchestsaddon.files.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class Logger {

    private static AutosellChests plugin;
    private static boolean debug;

    public Logger(AutosellChests plugin) {
        Logger.plugin = plugin;
        Logger.debug = Config.get().getBoolean("debug");
    }

    public static void sendPlayerMessage(Player p, String s) {
        p.sendMessage(Lang.PLUGIN_PREFIX.get() + ChatColor.RESET + " " + s);
    }

    public static void sendMessage(Object logger, String s) {
        if (logger instanceof Player) {
            sendPlayerMessage((Player) logger, s);
        } else if (logger instanceof ConsoleCommandSender) {
            ((ConsoleCommandSender) logger).sendMessage(s);
        }
    }

    public static void info(String s) {
        plugin.getLogger().info(s);
    }

    public static void warn(String s) {
        plugin.getLogger().warning(s);
    }

    public static void debug(String s) {
        if (debug) plugin.getLogger().info(s);
    }
}
