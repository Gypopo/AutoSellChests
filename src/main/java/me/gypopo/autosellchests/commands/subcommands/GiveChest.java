package me.gypopo.autosellchests.commands.subcommands;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.commands.SubCommad;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        return "/asc give [player] [quantity] [SellMultiplier]";
    }

    @Override
    public void perform(Object logger, String[] args) {
        //asc give [player] [quantity] [SellMultiplier]

        if (args.length < 1) {
            Logger.sendMessage(logger, this.getSyntax());
            return;
        }

        if (args.length == 1) {
            // asc give
            if (logger instanceof Player) {
                Player player = (Player) logger;
                this.giveQuantity(logger, player, 1);
            } else {
                Logger.sendMessage(logger, "You need to specify a player");
            }
        } else {
            // asc give ...
            int qty;
            if (args.length == 2 && logger instanceof Player) {
                // asc give [qty]
                Player player = (Player) logger;
                try {
                    qty = Integer.parseInt(args[1]);
                    this.giveQuantity(logger, player, qty);
                    return;
                } catch (NumberFormatException ignored) {
                    this.giveQuantity(logger, player, 1);
                }
            }
            Player p = Bukkit.getPlayer(args[1]);
            if (p != null) {
                if (args.length == 3) {
                    // asc give [player] [qty]
                    try {
                        qty = Integer.parseInt(args[2]);
                        this.giveQuantity(logger, p, qty);
                    } catch (NumberFormatException ex) {
                        Logger.sendMessage(logger, "Not a valid amount");
                    }
                } else if (args.length == 4) {
                    // asc give [player] [qty] [multiplier]
                    double multiplier;
                    try {
                        qty = Integer.parseInt(args[2]);
                        try {
                            multiplier = Double.parseDouble(args[3]);
                            this.giveQuantity(logger, p, qty, multiplier);
                        } catch (NumberFormatException ex) {
                            Logger.sendMessage(logger, "Not a valid sell multiplier");
                        }
                    } catch (NumberFormatException ex) {
                        Logger.sendMessage(logger, "Not a valid amount");
                    }
                }
            } else {
                Logger.sendMessage(logger, "Could not find a online player with name " + args[1]);
            }
        }
    }

    private void giveQuantity(Object logger, Player player, int qty) {
        if (player.getInventory().firstEmpty() == -1) {
            Logger.sendMessage(logger, Lang.NOT_ENOUGH_INVENTORY_SPACE.get());
            return;
        }

        player.getInventory().addItem(AutoSellChests.getInstance().getManager().getChest(qty));
        Logger.sendPlayerMessage(player, Lang.PLAYER_SELL_CHEST_GIVEN.get().replace("%amount%", String.valueOf(qty)));
        Logger.info(Lang.SELL_CHEST_GIVEN_LOG.get().replace("%player_name%", player.getName()).replace("%amount%", String.valueOf(qty)));
    }

    private void giveQuantity(Object logger, Player player, int qty, double multiplier) {
        if (player.getInventory().firstEmpty() == -1) {
            Logger.sendMessage(logger, Lang.NOT_ENOUGH_INVENTORY_SPACE.get());
            return;
        }

        player.getInventory().addItem(AutoSellChests.getInstance().getManager().getChest(qty, multiplier));
        Logger.sendPlayerMessage(player, Lang.PLAYER_SELL_CHEST_GIVEN.get().replace("%amount%", String.valueOf(qty)));
        Logger.info(Lang.SELL_CHEST_MULTIPLIER_GIVEN_LOG.get().replace("%player_name%", player.getName()).replace("%amount%", String.valueOf(qty)).replace("%multiplier%", String.valueOf(multiplier)));
    }

    @Override
    public List<String> getTabCompletion(String[] args) {
        switch (args.length) {
            case 2:
                return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
            case 3:
                return Arrays.asList("1", "2", "3", "5", "10");
            default:
                return null;
        }
    }
}
