package me.gypopo.autosellchestsaddon.commands;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.commands.subcommands.GiveChest;
import me.gypopo.autosellchestsaddon.commands.subcommands.Reload;
import me.gypopo.autosellchestsaddon.commands.subcommands.RemoveChest;
import me.gypopo.autosellchestsaddon.commands.subcommands.ViewChests;
import me.gypopo.autosellchestsaddon.files.Lang;
import me.gypopo.autosellchestsaddon.util.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellChestCommand implements TabExecutor {

    private final ArrayList<SubCommad> subCommads = new ArrayList<>();

    private final AutosellChests plugin;

    public SellChestCommand(AutosellChests plugin) {
        subCommads.add(new GiveChest());
        subCommads.add(new ViewChests());
        subCommads.add(new RemoveChest());
        subCommads.add(new Reload());

        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                for (SubCommad subCommad : this.subCommads) {
                    if (args[0].equalsIgnoreCase(subCommad.getName())) {
                        if (player.hasPermission("autosellchests." + subCommad.getName())) {
                            subCommad.perform(sender, args);
                        } else {
                            Logger.sendMessage(sender, Lang.NO_PERMISSIONS.get());
                        }
                        return true;
                    }
                }
            }

            // If player has any permissions to use the command without subcommand
            if (hasPermission(player)) {
                sendAllSyntaxes(sender);
            } else {
                player.sendMessage(Lang.NO_PERMISSIONS.get());
            }
        } else if (sender instanceof ConsoleCommandSender) {
            if (args.length > 0) {
                for (SubCommad subCommad : this.subCommads) {
                    if (args[0].toLowerCase().equalsIgnoreCase(subCommad.getName())) {
                        subCommad.perform(sender, args);
                        return true;
                    }
                }
            }

            // If player has any permissions to use the command without subcommand
            sendAllSyntaxes(sender);
        }

        return true;
    }

    private boolean hasPermission(Player p) {
        return p.hasPermission("autosellchests.give") ||
                p.hasPermission("autosellchests.view");
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (args.length == 1) {

            //get all subcommands
            for (SubCommad subCommad : this.subCommads) {
                tabCompletions.add(subCommad.getName());
            }

            //Check for closest match
            if (!args[0].isEmpty()) {
                final List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], tabCompletions, completions);
                Collections.sort(completions);
                return completions;
            } else {
                return tabCompletions;
            }
        } else if (args.length > 1) {
            //get all subcommands and execute the tab completion method's of that command
            for (SubCommad subCommad : this.subCommads) {
                if (args[0].equalsIgnoreCase(subCommad.getName())) {
                    return subCommad.getTabCompletion(args);
                }
            }
        }

        return null;
    }

    private void sendAllSyntaxes(Object logger) {
        Logger.sendMessage(logger, ChatColor.DARK_GREEN + "----------------------------------------");
        for (SubCommad subCommad : this.subCommads) {
            Logger.sendMessage(logger, " ");
            Logger.sendMessage(logger, ChatColor.DARK_GREEN + "- " + ChatColor.GREEN + subCommad.getSyntax() + " - " + subCommad.getDescription());
            Logger.sendMessage(logger, " ");
        }
        Logger.sendMessage(logger, ChatColor.DARK_GREEN + "----------------------------------------");
    }
}