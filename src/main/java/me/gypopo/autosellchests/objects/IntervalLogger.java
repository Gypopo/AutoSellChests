package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.util.EcoType;
import org.bukkit.scheduler.BukkitTask;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class IntervalLogger {

    private final BukkitTask task;
    private final AtomicInteger count = new AtomicInteger(0);
    private final Set<Integer> chests = new HashSet<>();
    private int items;

    public IntervalLogger(AutoSellChests plugin) {
        long delay;
        try {
            String time = Config.get().getString("interval-logs.interval");

            delay = plugin.getTimeUtils().getTime(time);
        } catch (NullPointerException | ParseException | NumberFormatException e) {
            Logger.warn("Failed to load sell logging interval from config.yml, got '" + Config.get().getString("interval-logs.interval") + "', using default of 10 minutes");
            delay = 600000;
        }

        this.task = this.getTask(plugin, delay);
    }

    private BukkitTask getTask(AutoSellChests plugin, long delay) {
        return plugin.runTaskAsyncTimer(new Runnable() {
            @Override
            public void run() {
                if (items > 0) {
                    Logger.info(Lang.ITEMS_SOLD_CONSOLE_INTERVAL.get()
                            .replace("%count%", String.valueOf(count.get()))
                            .replace("%items%", String.valueOf(items))
                            .replace("%chests%", String.valueOf(chests.size()))
                            .replace("%interval%", plugin.getTimeUtils().getReadableTime(delay)));

                    chests.clear();
                    count.set(0);
                    items = 0;
                }
            }
        }, delay / 1000L * 20L, delay / 1000L * 20L);
    }

    public void addContents(int items, Set<Integer> chests) {
        this.count.incrementAndGet();

        this.items += items;
        this.chests.addAll(chests);
    }

    public void stop() {
        this.task.cancel();
    }
}
