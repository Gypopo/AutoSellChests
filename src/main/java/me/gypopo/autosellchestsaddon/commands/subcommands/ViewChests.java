package me.gypopo.autosellchestsaddon.commands.subcommands;

import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.commands.SubCommad;
import me.gypopo.autosellchestsaddon.objects.Chest;
import me.gypopo.autosellchestsaddon.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ViewChests implements SubCommad {

    @Override
    public String getName() {
        return "view";
    }

    @Override
    public String getDescription() {
        return "Get information about the chest(s) a player has";
    }

    @Override
    public String getSyntax() {
        return "/asc view <player>";
    }

    @Override
    public void perform(Object logger, String[] args) {
        //asc view <player>
        if (args.length > 1) {
            List<Chest> chests = AutosellChests.getInstance().getManager().getChestsByPlayer(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
            if (!chests.isEmpty()) {
                Logger.sendMessage(logger, ChatColor.translateAlternateColorCodes('&',"&aFound %7%qty% &aAutoSellChests for " + args[0] + "(" + Bukkit.getOfflinePlayer(args[0]).getUniqueId() + ")".replace("%qty%", String.valueOf(chests.size()))));
                for (Chest chest : chests) {
                    Logger.sendMessage(logger, ChatColor.translateAlternateColorCodes('&', "&eID: &c" + chest.getId() + " &7| &eLocation: &cWorld '" + chest.getLocation().getWorld().getName() + "', x" + chest.getLocation().getBlockX() + ", y" + chest.getLocation().getBlockY() + ", z" + chest.getLocation().getBlockZ() + " &7| &eTotalProfit: &c$" + chest.getIncome() + " &7| &eTotalItemsSold: &c" + chest.getItemsSold()));
                }
            } else {
                Logger.sendMessage(logger, ChatColor.RED + "This player has no placed AutoSellChests");
            }
        } else {
            Logger.sendMessage(logger, this.getSyntax());
        }
    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        return null;
    }
}
