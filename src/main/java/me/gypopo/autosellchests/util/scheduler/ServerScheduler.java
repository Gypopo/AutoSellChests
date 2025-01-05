package me.gypopo.autosellchests.util.scheduler;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import org.bukkit.Location;

public interface ServerScheduler {

    void runTask(AutoSellChests plugin, Runnable run);

    void runTask(AutoSellChests plugin, Chest chest, Runnable run);

    void runTaskLater(AutoSellChests plugin, Runnable run, final long delay);

    void runTaskLater(AutoSellChests plugin, Location loc, Runnable run, final long delay);

    Task runTaskTimer(AutoSellChests plugin, Runnable run, final long delay, long period);

    void runTaskAsync(AutoSellChests plugin, Runnable run);

    void runTaskLaterAsync(AutoSellChests plugin, Runnable run, long delay);

    Task runTaskAsyncTimer(AutoSellChests plugin, Runnable run, long delay, long period);
}