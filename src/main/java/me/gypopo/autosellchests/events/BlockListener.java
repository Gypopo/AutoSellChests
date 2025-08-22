package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BlockListener implements Listener {

    private final AutoSellChests plugin;

    public BlockListener(AutoSellChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled() ||
                (this.plugin.version == 121 &&
                        (event.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK || event.getExplosionResult() == ExplosionResult.KEEP)))
            return;

        this.handleBlockExploison(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled())
            return;

        this.handleBlockExploison(event.blockList());
    }

    private void handleBlockExploison(List<Block> affected) {
        // Use separate list as 1.16.5 trows ConcurrentModificationException when modifying the list in the loop
        final Collection<Block> toRemove = new ArrayList<>();

        for (Block block : affected) {
            if (block == null || block.getType() != Material.CHEST)
                continue;

            Chest chest = this.plugin.getManager().getChestByLocation(block.getLocation());
            if (chest == null)
                continue;

            toRemove.add(block);
            this.plugin.getManager().removeChest(new ChestLocation(block.getLocation()));

            block.getWorld().dropItemNaturally(block.getLocation(), this.plugin.getManager().getChest(chest, 1));
            if (this.plugin.version != 116) {
                Arrays.stream(((org.bukkit.block.Chest) block.getState()).getBlockInventory().getContents()).forEach(item -> {
                    if (item != null && item.getType() != Material.AIR)
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                });
            }

            block.setType(Material.AIR);

            Logger.debug("SellChest at location 'World '" + chest.getLocation().getLeftLocation().world + "', x" + chest.getLocation().getLeftLocation().x + ", y" + chest.getLocation().getLeftLocation().y + ", z" + chest.getLocation().getLeftLocation().z + "' exploded");
        }

        affected.removeAll(toRemove);
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
