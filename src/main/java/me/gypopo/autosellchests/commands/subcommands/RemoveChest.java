package me.gypopo.autosellchests.commands.subcommands;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.commands.SubCommad;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Arrays;
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
                if (block.getType() == Material.CHEST) {
                    Chest chest = AutoSellChests.getInstance().getManager().getChestByLocation(block.getLocation());
                    if (chest != null) {
                        AutoSellChests.getInstance().getManager().removeChest(chest);
                        this.breakNaturally(chest, chest.getLocation().getLeftLocation().toLoc());
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
                Chest chest = AutoSellChests.getInstance().getManager().getChestByID(id);
                if (chest != null) {
                    Logger.sendMessage(logger, ChatColor.GREEN + "Successfully broken chest from " + Bukkit.getOfflinePlayer(chest.getOwner()).getName());
                    AutoSellChests.getInstance().getManager().removeChest(chest);
                    this.breakNaturally(chest, chest.getLocation().getLeftLocation().toLoc());
                } else {
                    Logger.sendMessage(logger,  ChatColor.RED + "No sell chest found with ID " + args[1]);
                }
            } catch (NumberFormatException ex) {
                Logger.sendMessage(logger, ChatColor.RED + "Not a valid id");
            }
        }

    }

    private void breakNaturally(Chest chest, Location loc) {
        loc.add(0.5, 0.5, 0.5);

        // Drop the contents of the chest
        Arrays.stream(((org.bukkit.block.Chest) chest.getLocation().getLeftLocation().toLoc().getBlock().getState()).getInventory().getContents()).forEach(item -> {
            if (item != null && item.getType() != Material.AIR) loc.getWorld().dropItemNaturally(loc, item);
        });

        // In 1.16.5, Block#setType(AIR) causes the chest to also drop its contents, so make sure its empty or the items will be duplicated
        if (AutoSellChests.getInstance().version == 116)
            ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory().clear();

        // Break the chest
        loc.getBlock().setType(Material.AIR);
        if (chest.getLocation().isDoubleChest())
            chest.getLocation().getRightLocation().toLoc().getBlock().setType(Material.AIR);

        loc.getWorld().dropItemNaturally(loc, AutoSellChests.getInstance().getManager().getChest(chest, chest.getLocation().isDoubleChest() ? 2 : 1));
    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        return null;
    }
}
