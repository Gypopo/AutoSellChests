package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.ChunkLoc;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener {

    private final AutoSellChests plugin;

    public ChunkListener(AutoSellChests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent e) {
        this.plugin.getManager().loadChests(new ChunkLoc(e.getChunk()));
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent e) {
        this.plugin.getManager().unloadChests(new ChunkLoc(e.getChunk()));
    }
}