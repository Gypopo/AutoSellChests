package me.gypopo.autosellchests.protection.hooks;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.Location;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.SimpleParticle;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldEditHook {

    private final AutoSellChests plugin;
    private final ChestManager manager;

    public WorldEditHook(AutoSellChests plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) return;
        if (this.manager.getLoadedChests().isEmpty()) return;

        World world = event.getWorld();
        if (!(world instanceof BukkitWorld)) return;

        Map<BlockVector3, Chest> cache = this.getChests(world.getName());
        if (cache.isEmpty()) return;

        Set<Chest> queued = ConcurrentHashMap.newKeySet();
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
                Chest chest = cache.get(position);
                if (chest != null && queued.add(chest))
                    update(chest);
                return super.setBlock(position, block);
            }
        });
    }

    private Map<BlockVector3, Chest> getChests(String world) {
        Map<BlockVector3, Chest> index = new HashMap<>();
        for (Chest chest : this.manager.getLoadedChests().values()) {
            me.gypopo.autosellchests.objects.Location left = chest.getLocation().getLeftLocation();
            if (left != null && left.world.equals(world))
                index.put(BlockVector3.at(left.x, left.y, left.z), chest);

            if (chest.getLocation().isDoubleChest()) {
                me.gypopo.autosellchests.objects.Location right = chest.getLocation().getRightLocation();
                if (right != null && right.world.equals(world))
                    index.put(BlockVector3.at(right.x, right.y, right.z), chest);
            }
        }
        return index;
    }

    private void update(Chest chest) {
        org.bukkit.Location loc = chest.getLocation().getLeftLocation().toLoc();
        this.plugin.getScheduler().runTaskForRegionLater(loc, () -> {
            if (!this.manager.getLoadedChests().containsValue(chest))
                return; // No longer cached (already handled elsewhere)

            this.plugin.getManager().removeChest(new ChestLocation(loc));
            this.plugin.getHologramManager().removeHologram(chest);
            Logger.warn("Chest at " + chest.getLocation().toString() + " got destroyed by a block/world edit, removing from memory...");
        }, 1L);
    }
}