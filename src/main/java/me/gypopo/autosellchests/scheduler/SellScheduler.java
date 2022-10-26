package me.gypopo.autosellchests.scheduler;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

public class SellScheduler {

    private final AutoSellChests plugin;

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
        this.intervalLogging = Config.get().getBoolean("sell-interval-logging");

        // Give the server 2 minutes to fully start before starting the interval
        Logger.info("Starting first sell interval in 15 seconds");
        this.task = this.plugin.runTaskLater(this.startNextInterval(), /*2400L*/300L);
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
                if (block.getInventory().isEmpty()/* ||
                        (block instanceof DoubleChest && ((org.bukkit.block.Chest)chest.getLocation().getRightLocation().getBlock().getState()).getInventory())*/) {
                    this.processNextChest(i);
                    return;
                }

                int amount = 0;
                double totalPrice = 0.0;
                for (ItemStack item : block.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        Double sellPrice = owner.isOnline() ? EconomyShopGUIHook.getItemSellPrice(owner.getPlayer(), item) : EconomyShopGUIHook.getItemSellPrice(item);
                        if (sellPrice != null && sellPrice > 0) {
                            totalPrice += sellPrice;
                            amount += item.getAmount();
                            block.getInventory().remove(item);
                            EconomyShopGUIHook.sellItem(item, amount);
                        }
                    }
                }
                if (amount != 0) {
                    this.items += amount;
                    chest.addItemsSold(amount);
                    chest.addIncome(totalPrice);
                    this.plugin.getEconomy().depositBalance(owner, totalPrice);
                    if (this.plugin.getManager().soldItemsLoggingPlayer && owner.isOnline())
                        Logger.sendPlayerMessage(owner.getPlayer(), Lang.ITEMS_SOLD_PLAYER_LOG.get().replace("%amount%", String.valueOf(amount)).replace("%profit%", this.plugin.formatPrice(totalPrice)));
                    if (this.plugin.getManager().soldItemsLoggingConsole) {
                        Logger.info(Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%player%", owner.getName())
                                .replace("%location%", "world '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ())
                                .replace("%amount%", String.valueOf(amount)).replace("%profit%", this.plugin.formatPrice(totalPrice)));
                    }
                }
            } catch (Exception e) {
                Logger.warn("Exception occurred while processing chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome() + " | TotalItemsSold: " + chest.getItemsSold());
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
                if (this.intervalLogging && this.items > 0) this.plugin.getLogger().info("Completed sell interval and sold '" + this.items + "' items for " + this.chests.size() + " chests, starting next interval in " + (this.interval-finish) + "ms(" + ((this.interval - finish)/1000*20) + " ticks)...");
                this.items = 0;
                this.task = this.plugin.runTaskLater(this.startNextInterval(), (this.interval - finish)/1000*20);
            }
        }
    }

    public long getStart() {
        return this.start;
    }

    public void stop() {
        this.plugin.getLogger().info("Canceling sell interval...");
        this.task.cancel();
    }

}
