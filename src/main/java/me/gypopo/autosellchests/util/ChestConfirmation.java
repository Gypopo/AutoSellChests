package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.economyshopgui.methodes.SendMessage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public enum ChestConfirmation {

    NONE,
    ACTION_BAR,
    BOSS_BAR;

    public void playEffect(Player p) {
        if (this == BOSS_BAR) {
            BarColor color;
            try {
                color = BarColor.valueOf(Config.get().getString("chest-confirmation-boss-bar-color"));
            } catch (IllegalArgumentException e) {
                color = BarColor.GREEN;
                SendMessage.warnMessage("Failed to find a boss bar color for: '" + Config.get().getString("chest-confirmation-boss-bar-color") + "'");
            }
            BossBar bar = Bukkit.createBossBar(Lang.PLACED_SELL_CHESTS_BOSS_BAR.get(), color, BarStyle.SEGMENTED_10);
            bar.setProgress(Math.min((AutoSellChests.getInstance().getManager().getChestsByPlayer(p.getUniqueId()).size() * 100 / ChestManager.maxSellChestsPlayer) / 10 * 0.1, 1));
            bar.addPlayer(p);
            AutoSellChests.getInstance().runTaskLater(() -> bar.removePlayer(p), 20 * 5);
        } else if (this == ACTION_BAR) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Lang.PLACED_SELL_CHESTS_ACTION_BAR.get()
                    .replace("%amount%", String.valueOf(AutoSellChests.getInstance().getManager().getChestsByPlayer(p.getUniqueId()).size()))
                    .replace("%limit%", p.hasPermission("autosellchests.maxchests.override") ? Lang.PLACED_SELL_CHESTS_ACTION_BAR_MAX.get() : String.valueOf(ChestManager.maxSellChestsPlayer))));
        }
    }

}
