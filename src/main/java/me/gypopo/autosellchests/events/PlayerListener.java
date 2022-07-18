package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.InformationScreen;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;

public class PlayerListener implements Listener {

    private AutoSellChests plugin;

    public PlayerListener(AutoSellChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        Block clicked = e.getClickedBlock();

        if (clicked == null || clicked.getType() != Material.CHEST) {
            return;
        }

        Chest chest = this.plugin.getManager().getChestByLocation(clicked.getLocation());
        if (chest == null) {
            return;
        }

        new InformationScreen(chest).open(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getBlockPlaced().getType().equals(Material.CHEST)) {
            return;
        }

        if (!e.getItemInHand().hasItemMeta() || !e.getItemInHand().getItemMeta().getDisplayName().equals(ChestManager.chestName)) {
            return;
        }

        if (this.plugin.getManager().getChestsByPlayer(e.getPlayer().getUniqueId()).size() == ChestManager.maxSellChestsPlayer && !e.getPlayer().hasPermission("autosellchests.maxchests.override")) {
            Logger.sendPlayerMessage(e.getPlayer(), Lang.MAX_SELLCHESTS_REACHED.get().replace("%maxSellChests%", String.valueOf(ChestManager.maxSellChestsPlayer)));
            return;
        }

        Location loc = e.getBlockPlaced().getLocation();
        this.plugin.getManager().addChest(loc, e.getPlayer());
        loc.add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 10);
        loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, new Particle.DustOptions(Color.RED, 2F));
        loc.getWorld().playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, SoundCategory.AMBIENT, 30L, 10L);
        Logger.sendPlayerMessage(e.getPlayer(), Lang.SELLCHEST_PLACED.get());

        loc.subtract(0.5, 0.5, 0.5); // This line is needed, it caused me some headaches :/
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.CHEST) {
            return;
        }

        Chest chest = this.plugin.getManager().getChestByLocation(e.getBlock().getLocation());
        if (chest == null) {
            //Logger.debug("Chest is null for location: " + e.getBlock().getLocation());
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getHolder() instanceof InformationScreen) {
            e.setCancelled(true);
            if (e.getSlot() == 31) {
                e.getWhoClicked().closeInventory();
                this.plugin.getManager().removeChest(((InformationScreen) e.getClickedInventory().getHolder()).getChest());
                Location loc = ((InformationScreen) e.getClickedInventory().getHolder()).getChest().getLocation().add(0.5, 0.5, 0.5);
                Arrays.stream(((org.bukkit.block.Chest)loc.getBlock().getState()).getBlockInventory().getContents()).forEach(item -> {
                    if (item != null && item.getType() != Material.AIR) loc.getWorld().dropItemNaturally(loc, item);
                });
                loc.getWorld().dropItemNaturally(loc, this.plugin.getManager().getChest(1));
                loc.getBlock().setType(Material.AIR);
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 15);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 30L, 10L);
                Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.SELLCHEST_BROKEN.get());
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof InformationScreen) {
            this.plugin.runTaskLater(() -> ((Player) e.getPlayer()).updateInventory(), 1);
        }
    }
}
