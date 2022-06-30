package me.gypopo.autosellchestsaddon.commands.subcommands;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.commands.SubCommad;
import me.gypopo.autosellchestsaddon.objects.Chest;
import me.gypopo.autosellchestsaddon.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveChest implements SubCommad {

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Remove the chest you are looking at or with the chests id which you can get by using /asc view <player>";
    }

    @Override
    public String getSyntax() {
        return "/asc remove [id]";
    }

    @Override
    public void perform(Object logger, String[] args) {

        if (args.length == 1) {
            if (logger instanceof Player) {
                Player player = (Player) logger;
                Block block = player.getTargetBlockExact(10);
                if (block.getType() == Material.TRAPPED_CHEST) {
                    Chest chest = AutosellChests.getInstance().getManager().getChestByLocation(block.getLocation());
                    if (chest != null) {
                        AutosellChests.getInstance().getManager().removeChest(chest);
                        block.breakNaturally();
                        Logger.sendPlayerMessage(player, ChatColor.GREEN + "Successfully broken chest from " + Bukkit.getOfflinePlayer(chest.getOwner()).getName());
                        return;
                    }
                }
                Logger.sendPlayerMessage(player, ChatColor.RED + "No AutoSellChest found at this location");
            } else {
                Logger.sendMessage(logger, this.getSyntax());
            }
        } else {
            try {
                int id = Integer.parseInt(args[1]);
                Chest chest = AutosellChests.getInstance().getManager().getChestByID(id);
                if (chest != null) {
                    Logger.sendMessage(logger, ChatColor.GREEN + "Successfully broken chest from " + Bukkit.getOfflinePlayer(chest.getOwner()).getName());
                    AutosellChests.getInstance().getManager().removeChest(chest);
                    chest.getLocation().getBlock().breakNaturally();
                } else {
                    Logger.sendMessage(logger,  ChatColor.RED + "No sell chest found with ID " + args[1]);
                }
            } catch (NumberFormatException ex) {
                Logger.sendMessage(logger, ChatColor.RED + "Not a valid id");
            }
        }

    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        return null;
    }
}
