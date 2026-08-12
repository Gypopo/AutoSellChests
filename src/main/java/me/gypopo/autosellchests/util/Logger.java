package me.gypopo.autosellchests.util;

import com.gpplugins.gplib.util.Log;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class Logger {

    public Logger(AutoSellChests plugin) {
        Log.init("§8[§6Auto§4Sell§6Chests§8]§r", Config.get().getBoolean("debug"));
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
        Log.info(s);
    }

    public static void warn(String s) {
        Log.warn(s);
    }

    public static void debug(String s) {
        Log.debug(s);
    }
}
