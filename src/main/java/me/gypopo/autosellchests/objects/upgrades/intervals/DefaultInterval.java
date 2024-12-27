package me.gypopo.autosellchests.objects.upgrades.intervals;

import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;

import java.text.ParseException;

public class DefaultInterval implements ChestInterval {

    private final long interval;
    private final long ticks;

    public DefaultInterval() {
        this.interval = this.getSellInterval();
        this.ticks = this.interval / 1000L * 20L;
    }

    @Override
    public long getInterval() {
        return this.interval;
    }

    @Override
    public long getTicks() {
        return this.ticks;
    }

    private long getSellInterval() {
        try {
            return TimeUtils.getTime(Config.get().getString("autosell-interval"));
        } catch (ParseException | NullPointerException e) {
            Logger.warn("Could not read the 'autosell-interval' from config, using 10 minutes as default.");
            e.printStackTrace();
            return 600000; // Default to ten minutes
        }
    }
}