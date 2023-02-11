package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.util.EcoType;
import org.bukkit.scheduler.BukkitTask;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestLogger {

    private final BukkitTask task;
    private final AtomicInteger count = new AtomicInteger(0);
    private final Map<ShopItem, Integer> items = new HashMap<>();
    private final Map<EcoType, Double> prices = new HashMap<>();

    public ChestLogger(AutoSellChests plugin) {
        long delay;
        try {
            String time = Config.get().getString("console-sold-items-logging.interval");

            delay = plugin.getTimeUtils().getTime(time);
        } catch (NullPointerException | ParseException | NumberFormatException e) {
            Logger.warn("Failed to load sell logging interval from config.yml, got '" + Config.get().getString("console-sold-items-logging.interval") + "', using default of 10 minutes");
            delay = 600000;
        }

        this.task = this.getTask(plugin, delay);
    }

    private BukkitTask getTask(AutoSellChests plugin, long delay) {
        return plugin.runTaskAsyncTimer(new Runnable() {
            @Override
            public void run() {
                Logger.info(plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_INTERVAL.get()
                        .replace("%count%", String.valueOf(count.get()))
                        .replace("%amount%", String.valueOf(items.values().stream().mapToInt(Integer::intValue).sum()))
                        .replace("%interval%", plugin.getTimeUtils().getReadableTime(delay))));
                if (!items.isEmpty())
                    items.clear();
                if (!prices.isEmpty())
                    prices.clear();
            }
        }, delay / 1000L * 20L, delay / 1000L * 20L);
    }

    public void addContents(Map<ShopItem, Integer> items, Map<EcoType, Double> prices) {
        this.count.incrementAndGet();

        items.forEach((key, value) -> {
            this.items.put(key, this.items.getOrDefault(key, 0) + value);
        });
        prices.forEach((key, value) -> {
            this.prices.put(key, this.prices.getOrDefault(key, 0d) + value);
        });
    }

    public void stop() {
        this.task.cancel();
    }
}
