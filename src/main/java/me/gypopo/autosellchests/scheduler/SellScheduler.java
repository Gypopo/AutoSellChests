package me.gypopo.autosellchests.scheduler;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.IntervalLogger;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.api.objects.SellPrices;
import me.gypopo.economyshopgui.api.prices.AdvancedSellPrice;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SellScheduler {

    private final AutoSellChests plugin;

    private final IntervalLogger logger;

    private BukkitTask task; // The current interval that is active

    private final long interval;
    private final long ticks; // The total interval for every chest

    private long start = System.currentTimeMillis();

    private final boolean onlineOwner;
    private final boolean intervalLogging;
    private final boolean supportsNewAPI; // Whether we can use EconomyShopGUI API v1.7.0+
    private final boolean afkDetection;

    private final ArrayList<Chest> chests = new ArrayList<>(); // All chests that have to be sold this interval
    private int next; // The interval between each chest
    private int amount = 1; // Amount of sell chests that should get sold in one tick

    private int items = 0;

    public SellScheduler(AutoSellChests plugin, long interval) {
        this.plugin = plugin;
        this.interval = interval;

        this.ticks = interval / 1000L * 20L;
        this.onlineOwner = Config.get().getBoolean("online-chest-owner", true);
        this.intervalLogging = Config.get().getBoolean("interval-logs.enable");
        this.supportsNewAPI = this.plugin.supportsNewAPI;
        this.afkDetection = plugin.getAFKManager() != null;

        // Give the server 2 minutes to fully start before starting the interval
        Logger.info("Starting first sell interval in 15 seconds");
        this.task = this.plugin.runTaskLater(this.startNextInterval(), /*2400L*/300L);

        this.logger = Config.get().getString("interval-logs.interval", "").isEmpty() ? null : new IntervalLogger(plugin);
    }

    private Runnable startNextInterval() {
        return () -> {
            this.start = System.currentTimeMillis();
            // Get all chests in loaded chunks
            if (!this.chests.isEmpty()) this.chests.clear();
            for (Chest chest : this.plugin.getManager().getLoadedChests().values()) {
                //this.plugin.getLogger().info("Chest " + chest.getId() + " is loaded:" + chest.getLocation().getWorld().isChunkLoaded(chest.getLocation().getBlockX() >> 4, chest.getLocation().getBlockZ() >> 4));
                if (chest.getLocation().getLeftLocation().getWorld()
                        .isChunkLoaded(chest.getLocation().getLeftLocation().getBlockX() >> 4, chest.getLocation().getLeftLocation().getBlockZ() >> 4)) {
                    chests.add(chest);
                }
            }
            if (this.chests.isEmpty()) {
                Logger.debug("Skipping sell interval because no loaded chests were found...");
                this.plugin.runTaskLater(this.startNextInterval(), this.ticks);
                return;
            }
            // Calculate the best time for when to sell a chest
            double i = Math.ceil(this.ticks / this.chests.size());
            if (i < 1) {
                // We don't want the server to sell all chests at once, there for we can specify how many chests to do per tick
                this.amount = (int) Math.floor(this.chests.size() / this.ticks);
            }
            this.next = Math.max(1, (int) i); // This makes it so not all chests get sold at once, but spread over the full interval
            // Start the actual loop
            this.plugin.runTask(this.supportsNewAPI ? this.sellContents1_7(this.chests.get(0), 0) : this.sellContents1_6(this.chests.get(0), 0));
        };
    }

    // Sell the chest contents using API methods available in EconomyShopGUI v6.2.6/EconomyShopGUI Premium v5.5.4 and older versions(API v1.7.0 and above)
    private Runnable sellContents1_7(Chest chest, int i) {
        return () -> {
            try {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
                if (this.onlineOwner && !owner.isOnline()) {
                    //Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
                    this.processNextChest(i);
                    return;
                } else if (this.afkDetection && this.plugin.getAFKManager().isAFK(owner.getUniqueId())) {
                    //Logger.debug("Owner from chest " + chest.getId() + " is afk, skipping...");
                    this.processNextChest(i);
                    return;
                }
                if (!this.plugin.getManager().getLoadedChests().containsKey(chest.getLocation())) {
                    Logger.debug("Did not found sell chest with id " + chest.getId() + " while executing the sell interval, skipping...");
                    this.processNextChest(i);
                    return;
                }
                chest.setNextInterval(System.currentTimeMillis() + this.interval);
                //Logger.debug("Starting sell interval for chest with id " + chest.getId());

                org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getLeftLocation().getBlock().getState();
                if (block.getInventory().isEmpty()) {
                    this.processNextChest(i);
                    return;
                }

                ItemStack[] items = block.getInventory().getContents(); // Retrieve the items from the inventory
                SellPrices transaction = EconomyShopGUIHook.getCutSellPrices(owner, items, true); // Get the sell prices of the items, and modify the array

                if (!transaction.isEmpty()) {
                    int total = transaction.getItems().values().stream().mapToInt(Integer::intValue).sum();
                    Map<EcoType, Double> prices = owner.isOnline() ? this.callPreTransactionEvent(transaction, total) : transaction.getPrices();
                    if (prices == null) {
                        this.processNextChest(i);
                        return; // Transaction cancelled by PreTransactionEvent
                    }

                    block.getInventory().setContents(items); // Update the inventory with the updated array of items

                    this.items += total;
                    chest.addItemsSold(total);
                    chest.addIncome(transaction.getPrices());
                    transaction.updateLimits(); // Update DynamicPricing, limited stock and sell limits **in sync** | Should be fairly safe to call synchronous
                    prices.forEach((type, price) -> {
                        if (!this.isClaimableCurrency(type)) {
                            EconomyShopGUIHook.getEcon(type).depositBalance(owner, price);
                        } else chest.addClaimAble(type, price);
                    });
                    if (chest.isLogging() && this.plugin.getManager().soldItemsLoggingPlayer && owner.isOnline()) {
                        Logger.sendPlayerMessage((Player) owner, this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_PLAYER_LOG.get()
                                        .replace("%chest-name%", chest.getName()).replace("%amount%", String.valueOf(total)))
                                .replace("%id%", String.valueOf(chest.getId())));
                    }
                    if (this.plugin.getManager().soldItemsLoggingConsole) {
                        Logger.info(this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%chest-name%", ChatColor.stripColor(chest.getName()).replace("%player%", owner.getName()))
                                .replace("%location%", "world '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ())
                                .replace("%amount%", String.valueOf(total)).replace("%id%", String.valueOf(chest.getId()))));
                    }
                }
            } catch (Exception e) {
                Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
                if (e instanceof ClassCastException) {
                    Logger.warn("The chest at this location does not longer exist, removing chest from database...");
                    this.plugin.getManager().removeChest(new ChestLocation(chest.getLocation().getLeftLocation()));
                }
                if (this.plugin.debug) e.printStackTrace();
            }
            //this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
            this.processNextChest(i);
        };
    }

    // Sell the chest contents using API methods available in EconomyShopGUI v6.2.5/EconomyShopGUI Premium v5.5.3 and earlier versions(API v1.6.2 and earlier)
    private Runnable sellContents1_6(Chest chest, int i) {
        return () -> {
            try {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
                if (this.onlineOwner && !owner.isOnline()) {
                    //Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
                    this.processNextChest(i);
                    return;
                } else if (this.afkDetection && this.plugin.getAFKManager().isAFK(owner.getUniqueId())) {
                    //Logger.debug("Owner from chest " + chest.getId() + " is afk, skipping...");
                    this.processNextChest(i);
                    return;
                }
                if (!this.plugin.getManager().getLoadedChests().containsKey(chest.getLocation())) {
                    Logger.debug("Did not found sell chest with id " + chest.getId() + " while executing the sell interval, skipping...");
                    this.processNextChest(i);
                    return;
                }
                chest.setNextInterval(System.currentTimeMillis() + this.interval);
                //Logger.debug("Starting sell interval for chest with id " + chest.getId());

                org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getLeftLocation().getBlock().getState();
                if (block.getInventory().isEmpty()) {
                    this.processNextChest(i);
                    return;
                }

                int amount = 0;
                Map<ShopItem, Integer> items = new HashMap<>();
                Map<EcoType, Double> prices = new HashMap<>();
                for (ItemStack item : block.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        ShopItem shopItem = !owner.isOnline() ? EconomyShopGUIHook.getShopItem(item) :
                                EconomyShopGUIHook.getShopItem((Player) owner, item);
                        if (shopItem == null) continue; // Shop item not found/Not inside shop

                        int limit = this.getMaxSell(shopItem, item.getAmount(), items.getOrDefault(shopItem, 0));
                        if (limit == -1) continue; // Maximum amount reached

                        limit = this.getSellLimit(shopItem, owner.getUniqueId(), limit);
                        if (limit == -1) continue; // Sell limit reached

                        if (EconomyShopGUIHook.isSellAble(shopItem)) {
                            ItemStack stack = new ItemStack(item);
                            stack.setAmount(limit); // Set the final amount to the item to get the sell price

                            this.calculateSellPrice(prices, shopItem, owner, item, limit, amount);

                            amount += limit;
                            if (limit < item.getAmount()) {
                                item.setAmount(item.getAmount() - limit);
                            } else block.getInventory().remove(item);
                            items.put(shopItem, items.getOrDefault(shopItem, 0) + limit);
                        }
                    }
                }

                if (amount != 0) {
                    this.items += amount;
                    chest.addItemsSold(amount);
                    chest.addIncome(prices);
                    this.sellItems(items, owner.getUniqueId()); // Update DynamicPricing, limited stock and sell limits in Async
                    prices.forEach((type, price) -> {
                        if (!this.isClaimableCurrency(type)) {
                            EconomyShopGUIHook.getEcon(type).depositBalance(owner, price);
                        } else chest.addClaimAble(type, price);
                    });
                    if (chest.isLogging() && this.plugin.getManager().soldItemsLoggingPlayer && owner.isOnline()) {
                        Logger.sendPlayerMessage((Player) owner, this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_PLAYER_LOG.get()
                                        .replace("%chest-name%", chest.getName()).replace("%amount%", String.valueOf(amount)))
                                        .replace("%id%", String.valueOf(chest.getId())));
                    }
                    if (this.plugin.getManager().soldItemsLoggingConsole) {
                        Logger.info(this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%chest-name%", ChatColor.stripColor(chest.getName()).replace("%player%", owner.getName())
                                .replace("%location%", "world '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ())
                                .replace("%amount%", String.valueOf(amount)).replace("%id%", String.valueOf(chest.getId())))));
                    }
                }
            } catch (Exception e) {
                Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
                if (e instanceof ClassCastException) {
                    Logger.warn("The chest at this location does not longer exist, removing chest from database...");
                    this.plugin.getManager().removeChest(new ChestLocation(chest.getLocation().getLeftLocation()));
                }
                if (this.plugin.debug) e.printStackTrace();
            }
            //this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
            this.processNextChest(i);
        };
    }

    /**
     * A simple helper method to calculates the sell price of a shop item.
     * <p>
     * Since EconomyShopGUI-Premium v4.9.0 allows shop items to have multiple sell prices at once, we should check for that.
     *
     * @param prices The Map that stores the price(s) of the item
     * @param shopItem The shopItem to calculate the price for
     * @param p The player selling the item
     * @param item The {@link ItemStack} that is being sold
     * @param amount The amount that is being sold
     * @param sold The amount of items that have been sold this batch(Useful for DynamicPricing)
     */
    private void calculateSellPrice(Map<EcoType, Double> prices, ShopItem shopItem, OfflinePlayer p, ItemStack item, int amount, int sold) {
        if (EconomyShopGUIHook.hasMultipleSellPrices(shopItem)) {
            // Get the sell price of every single registered sell price this item has
            AdvancedSellPrice sellPrice = EconomyShopGUIHook.getMultipleSellPrices(shopItem);

            if (p.isOnline()) { // Check discounts of player only when they are online
                sellPrice.getSellPrices(sellPrice.giveAll() ? null : sellPrice.getSellTypes().get(0), (Player) p, item, amount, sold)
                        .forEach((type, price) -> prices.put(type, prices.getOrDefault(type, 0d) + price));
            } else sellPrice.getSellPrices(sellPrice.giveAll() ? null : sellPrice.getSellTypes().get(0), item)
                    .forEach((type, price) -> prices.put(type, prices.getOrDefault(type, 0d) + price));

        } else {
            // The shop item only has one sell price
            double sellPrice = !p.isOnline() ? EconomyShopGUIHook.getItemSellPrice(shopItem, item) :
                        EconomyShopGUIHook.getItemSellPrice(shopItem, item, (Player) p, amount, sold);

            prices.put(shopItem.getEcoType(), prices.getOrDefault(shopItem.getEcoType(), 0d) + sellPrice);
        }
    }

    /**
     * Check for the maximum sell limit <b>per transaction</b>
     *
     * @return The amount of items which can be sold before reaching the limit
     * @since EconomyShopGUI v5.2.0 || EconomyShopGUI-Premium v4.4.0
     */
    private int getMaxSell(ShopItem shopItem, int qty, int alreadySold) {
        if (shopItem.isMaxSell(alreadySold + qty)) {
            if (alreadySold >= shopItem.getMaxSell())
                return -1; // Item already reached max sell for this transaction
            qty = shopItem.getMaxSell() - alreadySold;
        }
        return qty;
    }

    /**
     * Check for item sell stock
     * <p>
     * This is a Premium only feature but calls to the API can still be made
     * on the free version without it throwing an error
     *
     * @return The amount of items which can be sold before reaching the limit
     * @since EconomyShopGUI-Premium v4.1.0
     */
    private int getSellLimit(ShopItem shopItem, UUID playerUUID, int limit) {
        if (shopItem.getLimitedSellMode() != 0) { // Check for sell limits
            int stock = EconomyShopGUIHook.getSellLimit(shopItem, playerUUID);
            if (stock <= 0) {
                return -1; // Sell limit reached
            } else if (stock < limit) {
                limit = stock;
            }
        }
        return limit;
    }

    /**
     * Update the item's DynamicPricing and stock limit in async
     * <p>
     * This is a Premium only feature but calls to the API can still be made
     * on the free version without it throwing an error
     *
     * @since EconomyShopGUI-Premium v4.1.0
     */
    private void sellItems(Map<ShopItem, Integer> items, UUID playerUUID) {
        this.plugin.runTaskAsync(() -> {
            for (ShopItem item : items.keySet()) {
                if (item.isRefillStock())
                    EconomyShopGUIHook.sellItemStock(item, playerUUID, items.get(item));
                if (item.getLimitedSellMode() != 0)
                    EconomyShopGUIHook.sellItemLimit(item, playerUUID, items.get(item));
                if (item.isDynamicPricing())
                    EconomyShopGUIHook.sellItem(item, items.get(item));
            }
        });
    }

    private boolean isClaimableCurrency(EcoType ecoType) {
        return switch (ecoType.getType().name()) {
            case "ITEM", "LEVELS", "EXP" -> true;
            default -> false;
        };
    }

    private Map<EcoType, Double> callPreTransactionEvent(SellPrices prices, int amount) {
        PreTransactionEvent preTransactionEvent = new PreTransactionEvent(prices.getItems(), prices.getPrices(), (Player) prices.getPlayer(), amount, Transaction.Type.AUTO_SELL_CHEST);
        Bukkit.getPluginManager().callEvent(preTransactionEvent);
        return preTransactionEvent.isCancelled() ? null : preTransactionEvent.getPrices();
    }

    private void processNextChest(int index) {
        try {
            index++;
            Chest next = this.chests.get(index);
            if (next.getLocation().getLeftLocation().getWorld().isChunkLoaded(next.getLocation().getLeftLocation().getBlockX() >> 4, next.getLocation().getLeftLocation().getBlockZ() >> 4)) { // See if the chest is still loaded
                for (int i = 0; i < this.amount; i++) { // Run the amount of chests that needs to be sold at once
                    this.plugin.runTaskLater(this.supportsNewAPI ? this.sellContents1_7(next, index) : this.sellContents1_6(next, index), this.next);
                }
            } else this.processNextChest(index); // Skip this chest
        } catch (IndexOutOfBoundsException e) {
            // Reached end of the loop, wait for next interval to start
            long finish = System.currentTimeMillis()-this.start;
            Logger.debug("Completed sell interval in " + finish + "ms(" + finish/1000*20 + " ticks)");
            if (finish > this.interval) {
                if (this.items > 0) {
                    this.plugin.getLogger().warning("This sell interval finished " + (finish - this.interval) + "ms(" + ((finish - this.interval) / 1000 * 20) + " ticks) to late/out of schedule, this might be caused by to many server lag/missing ticks. It is recommended that you increase the sell interval inside the config!");
                    this.plugin.getLogger().info("Completed sell interval and sold all items for " + this.chests.size() + " chests, starting next interval now...");
                }
                this.items = 0;
                this.task = this.plugin.runTask(this.startNextInterval());
            } else {
                if (this.intervalLogging && this.items > 0) {
                    if (this.logger == null) {
                        // Log every interval
                        this.plugin.getLogger().info("Completed sell interval and sold '" + this.items + "' items for " + this.chests.size() + " chests, starting next interval in " + (this.interval - finish) + "ms(" + ((this.interval - finish) / 1000 * 20) + " ticks)...");
                    } else this.logger.addContents(this.items, this.chests.stream().map(Chest::getId).collect(Collectors.toSet()));
                }
                this.items = 0;
                this.task = this.plugin.runTaskLater(this.startNextInterval(), (this.interval - finish)/1000*20);
            }
        }
    }

    private String getCurrencyDisplay(Map<EcoType, Double> prices) {
        String color = ChatColor.getLastColors(Lang.ITEMS_SOLD_PLAYER_LOG.get().split("%profit%")[0]);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<EcoType, Double> entry : prices.entrySet()) {
            sb.append(plugin.formatPrice(entry.getKey(), entry.getValue()));

            sb.append("§r");
            if (i != prices.size()-1) sb.append(", ");
            i++;
        }
        return sb.toString();
    }

    public long getStart() {
        return this.start;
    }

    public void stop() {
        this.plugin.getLogger().info("Canceling sell interval...");
        this.task.cancel();
        if (this.logger != null)
            this.logger.stop();
    }

}
