package me.gypopo.autosellchestsaddon.commands.subcommands;

import jdk.jpackage.internal.Log;
import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.commands.SubCommad;
import me.gypopo.autosellchestsaddon.files.Lang;
import me.gypopo.autosellchestsaddon.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GiveChest implements SubCommad {

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getDescription() {
        return "&7Gives a #CDCDCDAuto#FF7070Sell#CDCDCDChest &7to the player specified";
    }

    @Override
    public String getSyntax() {
        return "/asc give [player] [quantity]";
    }

    @Override
    public void perform(Object logger, String[] args) {
        //asc give [player] [quantity]

        if (args.length < 1) {
            Logger.sendMessage(logger, this.getSyntax());
            return;
        }

        if (args.length == 1) {
            if (logger instanceof Player) {
                Player player = (Player) logger;
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(AutosellChests.getInstance().getManager().getChest(1));
                    Logger.sendPlayerMessage(player, Lang.PLAYER_SELL_CHEST_GIVEN.get().replace("%amount%", "1"));
                    Logger.info(Lang.SELL_CHEST_GIVEN_LOG.get().replace("%player_name%", player.getName()).replace("%amount%", "1"));
                } else {
                    Logger.sendPlayerMessage(player, Lang.NOT_ENOUGH_INVENTORY_SPACE.get());
                }
            } else {
                Logger.sendMessage(logger, "You need to specify a player");
            }
        } else {
            int qty;
            if (args.length == 2 && logger instanceof Player) {
                // Player gives himself an amount of chests (/asc give [qty])
                Player player = (Player) logger;
                try {
                    qty = Integer.parseInt(args[1]);
                    player.getInventory().addItem(AutosellChests.getInstance().getManager().getChest(qty));
                    Logger.sendPlayerMessage(player, Lang.PLAYER_SELL_CHEST_GIVEN.get().replace("%amount%", String.valueOf(qty)));
                    Logger.info(Lang.SELL_CHEST_GIVEN_LOG.get().replace("%player_name%", player.getName()).replace("%amount%", String.valueOf(qty)));
                    return;
                } catch (NumberFormatException ignored) {}
            }
            Player p = Bukkit.getPlayer(args[1]);
            if (p != null) {
                if (args.length > 2) {
                    try {
                        qty = Integer.parseInt(args[2]);
                        p.getInventory().addItem(AutosellChests.getInstance().getManager().getChest(qty));
                        Logger.info(Lang.SELL_CHEST_GIVEN_LOG.get().replace("%player_name%", p.getName()).replace("%amount%", String.valueOf(qty)));
                    } catch (NumberFormatException ex) {
                        Logger.sendMessage(logger, "Not a valid amount");
                    }
                }
            } else {
                Logger.sendMessage(logger, "Could not find a online player with name " + args[1]);
            }
        }
    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        switch (args.length) {
            case 2:
                // No need to suggest online players because it will happen from default
                return null;
            case 3:
                return Arrays.asList("1", "2", "3", "5", "10");
            default:
                return null;
        }
    }
}
