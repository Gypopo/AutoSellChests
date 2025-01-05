package me.gypopo.autosellchests.util.scheduler.schedulers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.scheduler.ServerScheduler;
import me.gypopo.autosellchests.util.scheduler.Task;
import me.gypopo.autosellchests.util.scheduler.tasks.BukkitTask;
import org.bukkit.Location;

public class BukkitScheduler implements ServerScheduler {

    private final org.bukkit.scheduler.BukkitScheduler scheduler;

    public BukkitScheduler(AutoSellChests plugin) {
        this.scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public void runTask(AutoSellChests plugin, Runnable run) {
        this.scheduler.runTask(plugin, run);
    }

    @Override
    public void runTask(AutoSellChests plugin, Chest chest, Runnable run) {
        this.runTask(plugin, run);
    }

    @Override
    public void runTaskLater(AutoSellChests plugin, Runnable run, final long delay) {
        this.scheduler.runTaskLater(plugin, run, delay);
    }

    @Override
    public void runTaskLater(AutoSellChests plugin, Location loc, Runnable run, final long delay) {
        this.runTaskLater(plugin, run, delay);
    }

    @Override
    public Task runTaskTimer(AutoSellChests plugin, Runnable run, final long delay, long period) {
        return new BukkitTask(this.scheduler.runTaskTimer(plugin, run, delay, period));
    }

    @Override
    public void runTaskAsync(AutoSellChests plugin, Runnable run) {
        this.scheduler.runTaskAsynchronously(plugin, run);
    }

    @Override
    public void runTaskLaterAsync(AutoSellChests plugin, Runnable run, long delay) {
        this.scheduler.runTaskLaterAsynchronously(plugin, run, delay);
    }

    @Override
    public Task runTaskAsyncTimer(AutoSellChests plugin, Runnable run, long delay, long period) {
        return new BukkitTask(this.scheduler.runTaskTimerAsynchronously(plugin, run, delay, period));
    }
}
