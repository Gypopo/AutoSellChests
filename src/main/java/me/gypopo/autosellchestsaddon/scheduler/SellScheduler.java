package me.gypopo.autosellchestsaddon.scheduler;

import jdk.jpackage.internal.Log;
import me.gypopo.autosellchestsaddon.AutosellChests;
import me.gypopo.autosellchestsaddon.files.Lang;
import me.gypopo.autosellchestsaddon.managers.ChestManager;
import me.gypopo.autosellchestsaddon.objects.Chest;
import me.gypopo.autosellchestsaddon.util.Logger;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SellScheduler {

    private final AutosellChests plugin;

    private BukkitTask task; // The current interval that is active

    private final long interval;
    private final long ticks; // The total interval for every chest

    private long start;

    private final ArrayList<Chest> chests = new ArrayList<>(); // All chests that have to be sold this interval
    private int next; // The interval between each chest
    private int amount = 1; // Amount of sell chests that should get sold in one tick

    private int items = 0;

    public SellScheduler(AutosellChests plugin, long interval) {
        this.plugin = plugin;
        this.interval = interval;

        this.ticks = interval / 1000L * 20L;

        // Give the server 2 minutes to fully start before starting the interval
        this.task = this.plugin.runTaskLater(this.startNextInterval(), /*2400L*/300L);
    }

    private Runnable startNextInterval() {
        return () -> {
            this.start = System.currentTimeMillis();
            // Get all chests in loaded chunks
            if (!this.chests.isEmpty()) this.chests.clear();
            for (Chest chest : this.plugin.getManager().getLoadedChests().values()) {
                //this.plugin.getLogger().info("Chest " + chest.getId() + " is loaded:" + chest.getLocation().getChunk().isLoaded());
                if (chest.getLocation().getChunk().isLoaded()) {
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
            if (Bukkit.getPlayer(chest.getOwner()) == null) {
                Logger.debug("Owner from chest " + chest.getId() + " is not online, skipping...");
                this.processNextChest(i);
                return;
            }
            if (!this.plugin.getManager().getLoadedChests().containsKey(chest.getLocation())) {
                Logger.debug("Did not found sell chest with id " + chest.getId() + " while executing the sell interval, skipping...");
                this.processNextChest(i);
                return;
            }
            chest.setNextInterval(this.interval);
            //Logger.debug("Starting sell interval for chest with id " + chest.getId());

            org.bukkit.block.Chest block = (org.bukkit.block.Chest) chest.getLocation().getBlock().getState();
            if (block.getBlockInventory().isEmpty()) {
                this.processNextChest(i);
                return;
            }

            int amount = 0;
            double totalPrice = 0.0;
            for (ItemStack item : block.getBlockInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    Double sellPrice = EconomyShopGUIHook.getItemSellPrice(item);
                    if (sellPrice != null && sellPrice > 0) {
                        totalPrice += sellPrice;
                        amount += item.getAmount();
                        block.getBlockInventory().remove(item);
                    }
                }
            }
            if (amount != 0) {
                this.items += amount;
                chest.addItemsSold(amount);
                chest.addIncome(totalPrice);
                this.plugin.getEconomy().depositBalance(Bukkit.getPlayer(chest.getOwner()), totalPrice);
                if (this.plugin.getManager().soldItemsLoggingPlayer && this.plugin.getServer().getPlayer(chest.getOwner()) != null)
                    Logger.sendPlayerMessage(this.plugin.getServer().getPlayer(chest.getOwner()), Lang.ITEMS_SOLD_PLAYER_LOG.get().replace("%amount%", String.valueOf(amount)).replace("%profit%", String.valueOf(totalPrice)));
                if (this.plugin.getManager().soldItemsLoggingConsole)
                    this.plugin.getLogger().info(Lang.ITEMS_SOLD_CONSOLE_LOG.get().replace("%player%", Bukkit.getPlayer(chest.getOwner()).getName()).replace("%location%", "world '" + chest.getLocation().getWorld().getName() + "', x" + chest.getLocation().getBlockX() + ", y" + chest.getLocation().getBlockY() + ", z" + chest.getLocation().getBlockZ()).replace("%amount%", String.valueOf(amount)).replace("%profit%", String.valueOf(totalPrice)));
            }

            //this.plugin.getLogger().info("Took " + (System.currentTimeMillis() - start) + "ms to sell the contents");
            this.processNextChest(i);
        };
    }

    private void processNextChest(int index) {
        try {
            index++;
            Chest next = this.chests.get(index);
            if (next.getLocation().getChunk().isLoaded()) { // See if the chest is still loaded
                for (int i = 0; i < this.amount; i++) { // Run the amount of chests that needs to be sold at once
                    this.plugin.runTaskLater(this.sellContents(next, index), this.next);
                }
            } else this.processNextChest(index); // Skip this chest
        } catch (IndexOutOfBoundsException e) {
            // Reached end of the loop, wait for next interval to start
            long finish = System.currentTimeMillis()-this.start;
            Logger.debug("Completed sell interval in " + finish + "ms(" + finish/1000*20 + " ticks)");
            if (finish > this.interval) {
                this.plugin.getLogger().warning("This sell interval finished " + (finish-this.interval) + "ms(" + ((finish-this.interval)/1000*20) + " ticks) to late/out of schedule, this might be caused by to many server lag/missing ticks. It is recommended that you increase the sell interval inside the config!");
                this.plugin.getLogger().info("Completed sell interval and sold all items for " + this.chests.size() + " chests, starting next interval now...");
                this.task = this.plugin.runTask(this.startNextInterval());
            } else {
                this.plugin.getLogger().info("Completed sell interval and sold '" + this.items + "' items for " + this.chests.size() + " chests, starting next interval in " + (this.interval-finish) + "ms(" + ((this.interval - finish)/1000*20) + " ticks)...");
                this.items = 0;
                this.task = this.plugin.runTaskLater(this.startNextInterval(), (this.interval - finish)/1000*20);
            }
        }
    }

    public void stop() {
        this.plugin.getLogger().info("Canceling sell interval...");
        this.task.cancel();
    }

}
