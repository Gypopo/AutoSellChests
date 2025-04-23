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
    private static String prefix;
    private static ConsoleCommandSender logger;

    public Logger(AutoSellChests plugin) {
        Logger.plugin = plugin;
        Logger.debug = Config.get().getBoolean("debug");
        Logger.prefix = "§8[§6Auto§4Sell§6Chests§8]§r";
        Logger.logger = plugin.getServer().getConsoleSender();
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
        logger.sendMessage(prefix + " §8[§7INFO§8]§r: " + s);
    }

    public static void warn(String s) {
        logger.sendMessage(prefix + " §8[§cWARN§8]§r: " + s);
    }

    public static void debug(String s) {
        if (debug)
            logger.sendMessage(prefix + "§8[§6DEBUG§8]§r: " + s);
    }
}
