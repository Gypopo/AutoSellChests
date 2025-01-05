package me.gypopo.autosellchests.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.UpgradeManager;
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
import java.util.stream.Collectors;

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

        this.logger = Config.get().getString("interval-logs.interval", "").isEmpty() ? null : new IntervalLogger(this.SCHEDULER_THREAD);

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
            this.plugin.runTask(chest, () -> this.sellContents(chest));

            nextChest = this.queue.peek();
        }
    }

    private void sellContents(Chest chest) {
        try {
            if (!chest.getLocation().isLoaded()) {// If chest is in unloaded chunk, skip
                Logger.debug("Tried to process chest " + chest.getId() + " but is in unloaded chunk, skipping...");
                return;
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
            if (this.onlineOwner && !owner.isOnline()) {
                Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
                return;
            } else if (this.afkDetection && this.plugin.getAFKManager().isAFK(owner.getUniqueId())) {
                Logger.debug("Owner from chest " + chest.getId() + " is afk, skipping...");
                return;
            }
            //Logger.debug("Starting sell interval for chest with id " + chest.getId());

            org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getLeftLocation().toLoc().getBlock().getState();
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

                if (UpgradeManager.multiplierUpgrades) // Apply chest sell multiplier
                    prices = prices.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() * chest.getMultiplier()));

                chest.addItemsSold(total);
                chest.addIncome(prices);
                transaction.updateLimits(); // Update DynamicPricing, limited stock and sell limits **in sync** | Should be fairly safe to call synchronous, unless MySQL is used which performs direct database calls instead of cache
                prices.forEach((type, price) -> {
                    if (!this.isClaimableCurrency(type)) {
                        EconomyShopGUIHook.getEcon(type).depositBalance(owner, price);
                    } else chest.addClaimAble(type, price);
                });
                this.handleLogs(chest, owner, prices, total);
            }
        } catch (Exception e) {
            Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().world + "', x" + chest.getLocation().getLeftLocation().x + ", y" + chest.getLocation().getLeftLocation().y + ", z" + chest.getLocation().getLeftLocation().z + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
            if (e instanceof ClassCastException) {
                Logger.warn("The chest at this location does not longer exist, removing chest from database...");
                this.plugin.getManager().removeChest(new ChestLocation(chest.getLocation().getLeftLocation().toLoc()));
            }
            if (this.plugin.debug) e.printStackTrace();
        }
        //this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
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
                        .replace("%location%", "world '" + chest.getLocation().getLeftLocation().world + "', x" + chest.getLocation().getLeftLocation().x + ", y" + chest.getLocation().getLeftLocation().y + ", z" + chest.getLocation().getLeftLocation().z)
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
        this.SCHEDULER_THREAD.shutdownNow();
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

    //    // Sell the chest contents using API methods available in EconomyShopGUI v6.2.5/EconomyShopGUI Premium v5.5.3 and earlier versions(API v1.6.2 and earlier)
//    private void sellContents_old(Chest chest, int i) {
//        try {
//            if (!chest.getLocation().isLoaded()) {// If chest is in unloaded chunk, skip
//                Logger.debug("Tried to process chest " + chest.getId() + " but is in unloaded chunk, skipping...");
//                return;
//            }
//
//            OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
//            if (this.onlineOwner && !owner.isOnline()) {
//                //Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
//                return;
//            } else if (this.afkDetection && this.plugin.getAFKManager().isAFK(owner.getUniqueId())) {
//                //Logger.debug("Owner from chest " + chest.getId() + " is afk, skipping...");
//                return;
//            }
//            //Logger.debug("Starting sell interval for chest with id " + chest.getId());
//
//            org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getLeftLocation().toLoc().getBlock().getState();
//            if (block.getInventory().isEmpty()) {
//                return;
//            }
//
//            int amount = 0;
//            Map<ShopItem, Integer> items = new HashMap<>();
//            Map<EcoType, Double> prices = new HashMap<>();
//            for (ItemStack item : block.getInventory().getContents()) {
//                if (item != null && item.getType() != Material.AIR) {
//                    ShopItem shopItem = !owner.isOnline() ? EconomyShopGUIHook.getShopItem(item) :
//                            EconomyShopGUIHook.getShopItem((Player) owner, item);
//                    if (shopItem == null) continue; // Shop item not found/Not inside shop
//
//                    int limit = this.getMaxSell(shopItem, item.getAmount(), items.getOrDefault(shopItem, 0));
//                    if (limit == -1) continue; // Maximum amount reached
//
//                    limit = this.getSellLimit(shopItem, owner.getUniqueId(), limit);
//                    if (limit == -1) continue; // Sell limit reached
//
//                    if (EconomyShopGUIHook.isSellAble(shopItem)) {
//                        ItemStack stack = new ItemStack(item);
//                        stack.setAmount(limit); // Set the final amount to the item to get the sell price
//
//                        this.calculateSellPrice(prices, shopItem, owner, item, limit, amount);
//
//                        amount += limit;
//                        if (limit < item.getAmount()) {
//                            item.setAmount(item.getAmount() - limit);
//                        } else block.getInventory().remove(item);
//                        items.put(shopItem, items.getOrDefault(shopItem, 0) + limit);
//                    }
//                }
//            }
//
//            if (amount != 0) {
//                chest.addItemsSold(amount);
//                chest.addIncome(prices);
//                this.sellItems(items, owner.getUniqueId()); // Update DynamicPricing, limited stock and sell limits in Async
//                prices.forEach((type, price) -> {
//                    if (!this.isClaimableCurrency(type)) {
//                        EconomyShopGUIHook.getEcon(type).depositBalance(owner, price);
//                    } else chest.addClaimAble(type, price);
//                });
//                if (chest.isLogging() && this.plugin.getManager().soldItemsLoggingPlayer && owner.isOnline()) {
//                    Logger.sendPlayerMessage((Player) owner, this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_PLAYER_LOG.get()
//                                    .replace("%chest-name%", chest.getName()).replace("%amount%", String.valueOf(amount)))
//                            .replace("%id%", String.valueOf(chest.getId())));
//                }
//                if (this.plugin.getManager().soldItemsLoggingConsole) {
//                    Logger.info(this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%chest-name%", ChatColor.stripColor(chest.getName()).replace("%player%", owner.getName())
//                            .replace("%location%", "world '" + chest.getLocation().getLeftLocation().world + "', x" + chest.getLocation().getLeftLocation().x + ", y" + chest.getLocation().getLeftLocation().y + ", z" + chest.getLocation().getLeftLocation().z)
//                            .replace("%amount%", String.valueOf(amount)).replace("%id%", String.valueOf(chest.getId())))));
//                }
//            }
//        } catch (Exception e) {
//            Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().world + "', x" + chest.getLocation().getLeftLocation().x + ", y" + chest.getLocation().getLeftLocation().y + ", z" + chest.getLocation().getLeftLocation().z + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
//            if (e instanceof ClassCastException) {
//                Logger.warn("The chest at this location does not longer exist, removing chest from database...");
//                this.plugin.getManager().removeChest(new ChestLocation(chest.getLocation().getLeftLocation().toLoc()));
//            }
//            if (this.plugin.debug) e.printStackTrace();
//        }
//        //this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
//    }
//
//    /**
//     * A simple helper method to calculates the sell price of a shop item.
//     * <p>
//     * Since EconomyShopGUI-Premium v4.9.0 allows shop items to have multiple sell prices at once, we should check for that.
//     *
//     * @param prices The Map that stores the price(s) of the item
//     * @param shopItem The shopItem to calculate the price for
//     * @param p The player selling the item
//     * @param item The {@link ItemStack} that is being sold
//     * @param amount The amount that is being sold
//     * @param sold The amount of items that have been sold this batch(Useful for DynamicPricing)
//     */
//    private void calculateSellPrice(Map<EcoType, Double> prices, ShopItem shopItem, OfflinePlayer p, ItemStack item, int amount, int sold) {
//        if (EconomyShopGUIHook.hasMultipleSellPrices(shopItem)) {
//            // Get the sell price of every single registered sell price this item has
//            AdvancedSellPrice sellPrice = EconomyShopGUIHook.getMultipleSellPrices(shopItem);
//
//            if (p.isOnline()) { // Check discounts of player only when they are online
//                sellPrice.getSellPrices(sellPrice.giveAll() ? null : sellPrice.getSellTypes().get(0), (Player) p, item, amount, sold)
//                        .forEach((type, price) -> prices.put(type, prices.getOrDefault(type, 0d) + price));
//            } else sellPrice.getSellPrices(sellPrice.giveAll() ? null : sellPrice.getSellTypes().get(0), item)
//                    .forEach((type, price) -> prices.put(type, prices.getOrDefault(type, 0d) + price));
//
//        } else {
//            // The shop item only has one sell price
//            double sellPrice = !p.isOnline() ? EconomyShopGUIHook.getItemSellPrice(shopItem, item) :
//                    EconomyShopGUIHook.getItemSellPrice(shopItem, item, (Player) p, amount, sold);
//
//            prices.put(shopItem.getEcoType(), prices.getOrDefault(shopItem.getEcoType(), 0d) + sellPrice);
//        }
//    }
//
//    /**
//     * Check for the maximum sell limit <b>per transaction</b>
//     *
//     * @return The amount of items which can be sold before reaching the limit
//     * @since EconomyShopGUI v5.2.0 || EconomyShopGUI-Premium v4.4.0
//     */
//    private int getMaxSell(ShopItem shopItem, int qty, int alreadySold) {
//        if (shopItem.isMaxSell(alreadySold + qty)) {
//            if (alreadySold >= shopItem.getMaxSell())
//                return -1; // Item already reached max sell for this transaction
//            qty = shopItem.getMaxSell() - alreadySold;
//        }
//        return qty;
//    }
//
//    /**
//     * Check for item sell stock
//     * <p>
//     * This is a Premium only feature but calls to the API can still be made
//     * on the free version without it throwing an error
//     *
//     * @return The amount of items which can be sold before reaching the limit
//     * @since EconomyShopGUI-Premium v4.1.0
//     */
//    private int getSellLimit(ShopItem shopItem, UUID playerUUID, int limit) {
//        if (shopItem.getLimitedSellMode() != 0) { // Check for sell limits
//            int stock = EconomyShopGUIHook.getSellLimit(shopItem, playerUUID);
//            if (stock <= 0) {
//                return -1; // Sell limit reached
//            } else if (stock < limit) {
//                limit = stock;
//            }
//        }
//        return limit;
//    }
//
//    /**
//     * Update the item's DynamicPricing and stock limit in async
//     * <p>
//     * This is a Premium only feature but calls to the API can still be made
//     * on the free version without it throwing an error
//     *
//     * @since EconomyShopGUI-Premium v4.1.0
//     */
//    private void sellItems(Map<ShopItem, Integer> items, UUID playerUUID) {
//        this.plugin.runTaskAsync(() -> {
//            for (ShopItem item : items.keySet()) {
//                if (item.isRefillStock())
//                    EconomyShopGUIHook.sellItemStock(item, playerUUID, items.get(item));
//                if (item.getLimitedSellMode() != 0)
//                    EconomyShopGUIHook.sellItemLimit(item, playerUUID, items.get(item));
//                if (item.isDynamicPricing())
//                    EconomyShopGUIHook.sellItem(item, items.get(item));
//            }
//        });
//    }
}