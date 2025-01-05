package me.gypopo.autosellchests.util.scheduler.schedulers;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.scheduler.ServerScheduler;
import me.gypopo.autosellchests.util.scheduler.Task;
import me.gypopo.autosellchests.util.scheduler.tasks.FoliaTask;

import java.util.concurrent.TimeUnit;

public class FoliaScheduler implements ServerScheduler {

    private final GlobalRegionScheduler globalRegionScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaScheduler(AutoSellChests plugin) {
        this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
        this.regionScheduler = plugin.getServer().getRegionScheduler();
        this.asyncScheduler = plugin.getServer().getAsyncScheduler();
    }

    @Override
    public void runTask(AutoSellChests plugin, Runnable run) {
        this.globalRegionScheduler.run(plugin, t -> run.run());
    }

    @Override
    public void runTask(AutoSellChests plugin, Chest chest, Runnable run) {
        this.regionScheduler.run(plugin, chest.getLocation().getLeftLocation().toLoc(), t -> run.run());
    }

    @Override
    public void runTaskLater(AutoSellChests plugin, Runnable run, final long delay) {
        if (delay <= 0) this.runTask(plugin, run);
        this.globalRegionScheduler.runDelayed(plugin, t -> run.run(), delay);
    }

    @Override
    public Task runTaskTimer(AutoSellChests plugin, Runnable run, long delay, long period) {
        if (delay <= 0) delay = 1L;
        return new FoliaTask(this.globalRegionScheduler.runAtFixedRate(plugin, t -> run.run(), delay, period));
    }

    @Override
    public void runTaskAsync(AutoSellChests plugin, Runnable run) {
        this.asyncScheduler.runNow(plugin, t -> run.run());
    }

    @Override
    public void runTaskLaterAsync(AutoSellChests plugin, Runnable run, long delay) {
        if (delay <= 0) this.runTaskAsync(plugin, run);
        this.asyncScheduler.runDelayed(plugin, t -> run.run(), delay * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public Task runTaskAsyncTimer(AutoSellChests plugin, Runnable run, long delay, long period) {
        if (delay <= 0) delay = 1L;
        return new FoliaTask(this.asyncScheduler.runAtFixedRate(plugin, t -> run.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS));
    }
}