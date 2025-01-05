package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IntervalLogger {

    private final AtomicInteger count = new AtomicInteger(0);
    private final Set<Integer> chests = new HashSet<>();
    private int items;

    public IntervalLogger(ScheduledExecutorService thread) {
        long delay;
        try {
            String time = Config.get().getString("interval-logs.interval");

            delay = TimeUtils.getTime(time);
        } catch (NullPointerException | ParseException | NumberFormatException e) {
            Logger.warn("Failed to load sell logging interval from config.yml, got '" + Config.get().getString("interval-logs.interval") + "', using default of 10 minutes");
            delay = 600000;
        }

        thread.scheduleAtFixedRate(this.getTask(delay), delay, delay, TimeUnit.MILLISECONDS);
    }

    public Runnable getTask(long delay) {
        return new Runnable() {
            @Override
            public void run() {
                if (items > 0) {
                    Logger.info(Lang.ITEMS_SOLD_CONSOLE_INTERVAL.get()
                            .replace("%count%", String.valueOf(count.get()))
                            .replace("%items%", String.valueOf(items))
                            .replace("%chests%", String.valueOf(chests.size()))
                            .replace("%interval%", TimeUtils.getReadableTime(delay)));

                    chests.clear();
                    count.set(0);
                    items = 0;
                }
            }
        };
    }

    public void addContents(int items, int chestID) {
        this.count.incrementAndGet();

        this.items += items;
        this.chests.add(chestID);
    }
}
