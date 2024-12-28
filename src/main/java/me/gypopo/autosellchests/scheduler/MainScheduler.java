package me.gypopo.autosellchests.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.IntervalLogger;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.api.objects.SellPrices;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainScheduler {

    private final AutoSellChests plugin;

    private final ScheduledExecutorService SCHEDULER_THREAD = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("ASC_SCHEDULER_THREAD").build());
    private final SchedulerQueue queue;

    private final IntervalLogger logger;

    private boolean running = true;
    private long currentTick;

    private long start = System.currentTimeMillis();

    private final boolean onlineOwner;
    private final boolean intervalLogging;
    private final boolean afkDetection;

    public MainScheduler(AutoSellChests plugin) {
        this.plugin = plugin;
        this.queue = new SchedulerQueue();

        this.onlineOwner = Config.get().getBoolean("online-chest-owner", true);
        this.intervalLogging = Config.get().getBoolean("interval-logs.enable");
        this.afkDetection = plugin.getAFKManager() != null;

        this.logger = Config.get().getString("interval-logs.interval", "").isEmpty() ? null : new IntervalLogger(plugin);

        Logger.info("Starting sell interval...");
        this.SCHEDULER_THREAD.scheduleAtFixedRate(this::processChests, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    private void processChests() {
        Chest nextChest = this.queue.peek();
        if (nextChest == null)
            return;

        while (this.running && (nextChest.getNextInterval() - System.currentTimeMillis()) <= 50) {
            System.out.println("Processing chest with ID " + nextChest.getId());
            Chest chest = this.queue.getNextAndUpdate();
            this.plugin.runTask(() -> this.sellContents(chest));

            nextChest = this.queue.peek();
        }
    }

    private void sellContents(Chest chest) {
        try {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
            if (this.onlineOwner && !owner.isOnline()) {
                Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
                return;
            } else if (this.afkDetection && this.plugin.getAFKManager().isAFK(owner.getUniqueId())) {
                Logger.debug("Owner from chest " + chest.getId() + " is afk, skipping...");
                return;
            }
            if (!this.plugin.getManager().getLoadedChests().containsKey(chest.getLocation())) {
                Logger.debug("Failed to find sell chest with id " + chest.getId() + " while executing the sell interval, skipping...");
                return;
            }
            Logger.debug("Starting sell interval for chest with id " + chest.getId());

            org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getLeftLocation().getBlock().getState();
            if (block.getInventory().isEmpty()) {
                return;
            }

            ItemStack[] items = block.getInventory().getContents(); // Retrieve the items from the inventory
            SellPrices transaction = EconomyShopGUIHook.getCutSellPrices(owner, items, true); // Get the sell prices of the items, and modify the array

            if (!transaction.isEmpty()) {
                int total = transaction.getItems().values().stream().mapToInt(Integer::intValue).sum();
                Map<EcoType, Double> prices = owner.isOnline() ? this.callPreTransactionEvent(transaction, total) : transaction.getPrices();
                if (prices == null)
                    return; // Transaction cancelled by PreTransactionEvent

                block.getInventory().setContents(items); // Update the inventory with the updated array of items

                chest.addItemsSold(total);
                chest.addIncome(transaction.getPrices());
                transaction.updateLimits(); // Update DynamicPricing, limited stock and sell limits **in sync** | Should be fairly safe to call synchronous, unless MySQL is used which performs direct database calls instead of cache
                prices.forEach((type, price) -> {
                    if (!this.isClaimableCurrency(type)) {
                        EconomyShopGUIHook.getEcon(type).depositBalance(owner, price);
                    } else chest.addClaimAble(type, price);
                });
                this.handleLogs(chest, owner, prices, total);
            }
        } catch (Exception e) {
            Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
            if (e instanceof ClassCastException) {
                Logger.warn("The chest at this location does not longer exist, removing chest from database...");
                this.plugin.getManager().removeChest(new ChestLocation(chest.getLocation().getLeftLocation()));
            }
            if (this.plugin.debug) e.printStackTrace();
        }
        this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
    }

    private void handleLogs(Chest chest, OfflinePlayer owner, Map<EcoType, Double> prices, int items) {
        if (chest.isLogging() && this.plugin.getManager().soldItemsLoggingPlayer && owner.isOnline()) {
            Logger.sendPlayerMessage((Player) owner, this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_PLAYER_LOG.get()
                    .replace("%chest-name%", chest.getName()).replace("%amount%", String.valueOf(items)))
                    .replace("%id%", String.valueOf(chest.getId())));
        }
        if (this.intervalLogging) {
            if (this.logger == null) {
                // Log every interval
                Logger.info(this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%chest-name%", ChatColor.stripColor(chest.getName()).replace("%player%", owner.getName()))
                        .replace("%location%", "world '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ())
                        .replace("%amount%", String.valueOf(items)).replace("%id%", String.valueOf(chest.getId()))));
            } else this.logger.addContents(items, chest.getId());
        }
    }

    private Map<EcoType, Double> callPreTransactionEvent(SellPrices prices, int amount) {
        PreTransactionEvent preTransactionEvent = new PreTransactionEvent(prices.getItems(), prices.getPrices(), (Player) prices.getPlayer(), amount, Transaction.Type.AUTO_SELL_CHEST);
        Bukkit.getPluginManager().callEvent(preTransactionEvent);
        return preTransactionEvent.isCancelled() ? null : preTransactionEvent.getPrices();
    }

    private boolean isClaimableCurrency(EcoType ecoType) {
        return switch (ecoType.getType()) {
            case ITEM, LEVELS, EXP -> true;
            default -> false;
        };
    }

    public void stop() {
        this.running = false;
    }

    public void reload() {
        this.running = false;

        this.queue.reload();

        this.running = true;
    }

    public void queueChest(Chest chest) {
        this.queue.addChest(chest);
    }

    public void updateChest(Chest chest, int newIntervalID) {
        this.queue.updateChestInterval(chest, newIntervalID);
    }

    public void removeFromQueue(Chest chest) {
        this.queue.removeChest(chest);
    }

    private long getCurrentTick() {
        return System.currentTimeMillis() / 1000L * 20L;
    }
}