package me.gypopo.autosellchests.commands.subcommands;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.commands.SubCommad;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
            List<Chest> chests = AutoSellChests.getInstance().getManager().getChestsByPlayer(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
            if (!chests.isEmpty()) {
                Logger.sendMessage(logger, ChatColor.translateAlternateColorCodes('&',"&aFound &7%qty% &aAutoSellChests for " + args[1] + "(" + Bukkit.getOfflinePlayer(args[1]).getUniqueId() + ")").replace("%qty%", String.valueOf(chests.size())));
                for (Chest chest : chests) {
                    Logger.sendMessage(logger, ChatColor.translateAlternateColorCodes('&',
                            "&eID: &c" + chest.getId() +
                                    " &7| &eLocation: &cWorld '" + chest.getLocation().getLeftLocation().getWorld().getName() +
                                        "', x" + chest.getLocation().getLeftLocation().getBlockX() +
                                        ", y" + chest.getLocation().getLeftLocation().getBlockY() +
                                        ", z" + chest.getLocation().getLeftLocation().getBlockZ() +
                                    " &7| &eTotalProfit: &c" + chest.getIncome("§c") +
                                    " &7| &eTotalItemsSold: &c" + chest.getItemsSold() +
                                    " &7| &eDoubleChest: &c" + chest.getLocation().isDoubleChest() +
                                    (UpgradeManager.intervalUpgrades ? " &7| &eInterval: &c" + UpgradeManager.getIntervalUpgrade(chest.getIntervalUpgrade()).getName() + "&f(" + TimeUtils.getReadableTime(chest.getInterval()) + ")" : "") +
                                    (UpgradeManager.multiplierUpgrades ? " &7| &eMultiplier: &c" + 105 + "%" : "")));
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
        switch (args.length) {
            case 2:
                return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
            default:
                return null;
        }
    }
}
