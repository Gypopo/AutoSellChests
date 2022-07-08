package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.scheduler.SellScheduler;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.methodes.SendMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ChestManager {

    public static String chestName;

    private AutoSellChests plugin;

    private final SellScheduler scheduler;

    private final long sellInterval;
    public static int maxSellChestsPlayer;

    public final boolean soldItemsLoggingPlayer;
    public final boolean soldItemsLoggingConsole;

    private Map<Location, Chest> loadedChests = new HashMap<>();
    // Only for quick access
    private Map<UUID, ArrayList<Chest>> loadedChestsByPlayer = new HashMap<>();

    public ChestManager(AutoSellChests plugin) {
        this.plugin = plugin;

        this.soldItemsLoggingPlayer = Config.get().getBoolean("sold-items-logging-player");
        this.soldItemsLoggingConsole = Config.get().getBoolean("sold-items-logging-console");
        ChestManager.maxSellChestsPlayer = Config.get().getInt("player-max-sellchests");

        this.sellInterval = this.getSellInterval();
        chestName = Lang.formatColors(Config.get().getString("sellchest-name"));

        this.loadChests();

        this.scheduler = new SellScheduler(plugin, this.sellInterval);
    }

    public Map<Location, Chest> getLoadedChests() {
        return this.loadedChests;
    }

    public ArrayList<Chest> getChestsByPlayer(UUID uuid) {
        return this.loadedChestsByPlayer.getOrDefault(uuid, new ArrayList<>());
    }

    public Chest getChestByLocation(Location loc) {
        return this.loadedChests.get(loc);
    }

    public Chest getChestByID(int id) {
        for (Chest chest : this.loadedChests.values()) {
            if (chest.getId() == id) return chest;
        }
        return null;
    }

    private void loadChests() {
        long start = System.currentTimeMillis();
        int i = 0;
        for (Chest chest : this.plugin.getDatabase().getAllChests()) {
            this.loadedChests.put(chest.getLocation(), chest);
            if (this.loadedChestsByPlayer.containsKey(chest.getOwner())) {
                List<Chest> chests = this.loadedChestsByPlayer.get(chest.getOwner());
                chests.add(chest);
                this.loadedChestsByPlayer.put(chest.getOwner(), this.loadedChestsByPlayer.get(chest.getOwner()));
            } else this.loadedChestsByPlayer.put(chest.getOwner(), new ArrayList<>(Collections.singletonList(chest)));
            i++;
        }
        Logger.debug("Took " + (System.currentTimeMillis()-start) + "ms to load " + i + " sell chests from the database");
    }

    public void addChest(Location loc, Player p) {
        this.plugin.getDatabase().setChest(loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ(), p.getUniqueId().toString(), 0, 0.0);
        Chest chest = new Chest(this.loadedChests.size() + 1, loc, p, 0, 0.0);
        this.loadedChests.put(loc, chest);
        if (this.loadedChestsByPlayer.containsKey(p.getUniqueId())) {
            this.loadedChestsByPlayer.get(p.getUniqueId()).add(chest);
        } else this.loadedChestsByPlayer.put(p.getUniqueId(), new ArrayList<>(Collections.singletonList(chest)));
        Logger.debug("Added SellChest for '" + p.getUniqueId() + "' on location: " + "World '" + loc.getWorld().getName() + "', x" + loc.getBlockX() + ", y" + loc.getBlockY() + ", z" + loc.getBlockZ());
    }

    public void removeChest(Chest chest) {
        this.plugin.getDatabase().removeChest(chest.getLocation().getWorld().getName() + ":" + chest.getLocation().getBlockX() + ":" + chest.getLocation().getBlockY() + ":" + chest.getLocation().getBlockZ());
        this.loadedChests.remove(chest.getLocation());
        if (this.loadedChestsByPlayer.containsKey(chest.getOwner())) {
            this.loadedChestsByPlayer.get(chest.getOwner()).remove(chest);
        } else this.loadedChestsByPlayer.remove(chest.getOwner());
        Logger.debug("Removed SellChest from '" + chest.getOwner() + "' on location: " + "World '" + chest.getLocation().getWorld().getName() + "', x" + chest.getLocation().getBlockX() + ", y" + chest.getLocation().getBlockY() + ", z" + chest.getLocation().getBlockZ());
    }

    public ItemStack getChest(int amount) {
        ItemStack chest = new ItemStack(Material.CHEST, amount);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(chestName);
        meta.setLore(Config.get().getStringList("sellchest-lore").stream().map(s -> Lang.formatColors(s.replace("%interval%", this.plugin.getTimeUtils().getReadableTime(this.sellInterval)))).collect(Collectors.toList()));
        chest.setItemMeta(meta);
        return chest;
    }

    private long getSellInterval() {
        try {
            return this.plugin.getTimeUtils().getTime(Config.get().getString("autosell-interval"));
        } catch (ParseException | NullPointerException e) {
            this.plugin.getLogger().warning("Could not read the 'autosell-interval' from config, using 10 minutes as default.");
            e.printStackTrace();
            return 600000; // Default to ten minutes
        }
    }

    private void saveChests() {
        Logger.debug("Saving '" + this.loadedChests.size() + "' chests...");
        for (Chest chest : this.loadedChests.values()) {
            Logger.debug("Chest has items " + chest.getItemsSold());
            this.plugin.getDatabase().saveChest(chest);
        }
    }

    public void disable() {
        this.saveChests();
        if (!this.loadedChests.isEmpty()) this.loadedChests.clear();
        if (!this.loadedChestsByPlayer.isEmpty()) this.loadedChestsByPlayer.clear();
        this.scheduler.stop();
    }
}
