package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class Logger {

    private static AutoSellChests plugin;
    private static boolean debug;

    public Logger(AutoSellChests plugin) {
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
