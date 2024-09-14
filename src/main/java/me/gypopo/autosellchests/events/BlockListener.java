package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BlockListener implements Listener {

    private final AutoSellChests plugin;

    public BlockListener(AutoSellChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block == null || block.getType() != Material.CHEST)
                return;

            Chest chest = this.plugin.getManager().getChestByLocation(block.getLocation());
            if (chest == null)
                return;

            this.plugin.getManager().removeChest(new ChestLocation(block.getLocation()));
        }
    }

    // Redundant, as even vanilla chests cannot be moved by pistons
    /*
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block == null || block.getType() != Material.CHEST)
                return;

            Chest chest = this.plugin.getManager().getChestByLocation(block.getLocation());
            if (chest == null)
                return;

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block == null || block.getType() != Material.CHEST)
                return;

            Chest chest = this.plugin.getManager().getChestByLocation(block.getLocation());
            if (chest == null)
                return;

            event.setCancelled(true);
        }
    }

     */
}
