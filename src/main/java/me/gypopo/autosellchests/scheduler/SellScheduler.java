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
import me.gypopo.economyshopgui.api.objects.BuyPrice;
import me.gypopo.economyshopgui.api.objects.SellPrice;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
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
            this.plugin.runTask(this.sellContents(this.chests.get(0), 0));
        };
    }

    private Runnable sellContents(Chest chest, int i) {
        return () -> {
            try {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(chest.getOwner());
                if (this.onlineOwner && !owner.isOnline()) {
                    //Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
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

                Map<EcoType, Double> prices = new HashMap<>();
                for (ItemStack item : block.getInventory().getContents()) {
                    if (item == null || item.getType() == Material.AIR)
                        continue;

                    Optional<SellPrice> optional = EconomyShopGUIHook.getSellPrice(owner, item);
                    if (!optional.isPresent())
                        continue;
                    // The item was able to be sold, according all item limits
                    SellPrice price = optional.get();

                    price.updateLimits(); // Update all item limits set on items
                    for (Map.Entry<EcoType, Double> entry : price.getPrices().entrySet()) {
                        prices.put(entry.getKey(), prices.getOrDefault(entry.getKey(), 0d) + entry.getValue());
                    }
                }

                if (!prices.isEmpty()) {
                    int total = 0;
                    this.items += total;
                    chest.addItemsSold(total);
                    chest.addIncome(prices);
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
                        Logger.info(this.plugin.formatPrices(prices, Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%chest-name%", ChatColor.stripColor(chest.getName()).replace("%player%", owner.getName())
                                .replace("%location%", "world '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ())
                                .replace("%amount%", String.valueOf(total)).replace("%id%", String.valueOf(chest.getId())))));
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

    private void processNextChest(int index) {
        try {
            index++;
            Chest next = this.chests.get(index);
            if (next.getLocation().getLeftLocation().getWorld().isChunkLoaded(next.getLocation().getLeftLocation().getBlockX() >> 4, next.getLocation().getLeftLocation().getBlockZ() >> 4)) { // See if the chest is still loaded
                for (int i = 0; i < this.amount; i++) { // Run the amount of chests that needs to be sold at once
                    this.plugin.runTaskLater(this.sellContents(next, index), this.next);
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
