package me.gypopo.autosellchests.managers;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.scheduler.MainScheduler;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.TimeUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class ChestManager {

    public static String chestName;

    private AutoSellChests plugin;

    private final UpgradeManager upgradeManager;
    private MainScheduler scheduler;

    private final int maxSellChestsPlayer;
    private final String defaultChestName;

    public final boolean soldItemsLoggingPlayer;
    public final boolean soldItemsLoggingConsole;

    private static final ItemStack fillItem = ChestManager.createFillItem();

    private final Map<String, Integer> maxChests = new LinkedHashMap<>();

    private Map<ChestLocation, Chest> loadedChests = new HashMap<>();
    // Only for quick access
    private Map<UUID, ArrayList<Chest>> loadedChestsByPlayer = new HashMap<>();

    public ChestManager(AutoSellChests plugin) {
        this.plugin = plugin;
        this.upgradeManager = new UpgradeManager(plugin);

        this.soldItemsLoggingPlayer = Config.get().getBoolean("sold-items-logging-player");
        this.soldItemsLoggingConsole = Config.get().getBoolean("sold-items-logging-console");
        this.maxSellChestsPlayer = Config.get().getInt("max-sellchests.default");

        defaultChestName = Lang.formatColors(Config.get().getString("default-chest-name"), null);
        chestName = Lang.formatColors(Config.get().getString("sellchest-name"), null);

        this.loadChests();
        this.loadMaximumChests();

        this.plugin.runTaskLater(this::startIntervalWhenReady, 100L);
    }

    // Schedule the start of the loop 5 seconds after the server loads
    // ensuring the server has finished loading most plugins/data
    private void startIntervalWhenReady() {
        this.scheduler = new MainScheduler(this.plugin);
    }

    public String getDefaultChestName() {
        return this.defaultChestName;
    }

    public Map<ChestLocation, Chest> getLoadedChests() {
        return this.loadedChests;
    }

    public ArrayList<Chest> getChestsByPlayer(UUID uuid) {
        return this.loadedChestsByPlayer.getOrDefault(uuid, new ArrayList<>());
    }

    public static ItemStack getFillItem() {
        return ChestManager.fillItem;
    }

    public Chest getChestByLocation(Location loc) {
        return this.loadedChests.get(new ChestLocation(loc));
    }

    public void updateChestInterval(Chest chest, int newIntervalID) {
        this.scheduler.updateChest(chest, newIntervalID);
    }

    private void loadMaximumChests() {
        Map<String, Integer> maxChests = new LinkedHashMap<>();
        for (String perm : Config.get().getConfigurationSection("max-sellchests.override").getKeys(false)) {
            String max = Config.get().getString("max-sellchests.override." + perm);
            try {
                maxChests.put(perm, Integer.parseInt(max));
            } catch (NumberFormatException e) {
                Logger.warn("Failed to load max sellchest override like " + perm + "." + max + " inside config.yml");
            } catch (NullPointerException ignored) {}
        }

        if (!this.maxChests.isEmpty())
            this.maxChests.clear();
        if (!maxChests.isEmpty()) {
            // Load the maximums from highest to lowest since the highest maximum will override any lower values
            List<Map.Entry<String, Integer>> list = new ArrayList<>(maxChests.entrySet());

            list.sort((Comparator) (o1, o2) -> ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue()));

            list.forEach(e -> {
                this.maxChests.put(e.getKey(), e.getValue());
            });
            Logger.info("Completed loading " + this.maxChests.size() + " max chest overrides");
        }
    }

    public int getMaxSell(Player player) {
        for (String perm : this.maxChests.keySet()) {
            if (player.hasPermission("autosellchests.maxchests." + perm))
                return this.maxChests.get(perm);
        }
        return this.maxSellChestsPlayer;
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

    public void addChest(ChestLocation loc, Player p) {
        Chest chest = this.loadedChests.get(loc);
        if (chest != null) { // Double chest
            chest.getLocation().addLocation(loc.getRightLocation());
            this.plugin.getDatabase().setChest(chest);
        } else {
            this.plugin.getDatabase().addChest(loc.toString(), p.getUniqueId().toString(), 0);
            chest = this.plugin.getDatabase().loadChest(loc);

            this.scheduler.queueChest(chest);
            this.loadedChests.put(chest.getLocation(), chest);
            if (this.loadedChestsByPlayer.containsKey(p.getUniqueId())) {
                this.loadedChestsByPlayer.get(p.getUniqueId()).add(chest);
            } else this.loadedChestsByPlayer.put(p.getUniqueId(), new ArrayList<>(Collections.singletonList(chest)));
        }
        Logger.debug("Added SellChest for '" + p.getUniqueId() + "' on location: " + "World '" + loc.getLeftLocation().getWorld().getName() + "', x" + loc.getLeftLocation().getBlockX() + ", y" + loc.getLeftLocation().getBlockY() + ", z" + loc.getLeftLocation().getBlockZ());
    }

    public void removeChest(ChestLocation loc) {
        Chest chest = this.loadedChests.get(loc);
        if (chest.getLocation().isDoubleChest()) {
            chest.getLocation().removeLocation(loc.getLeftLocation());
            this.plugin.getDatabase().setChest(chest);
        } else {
            this.scheduler.removeFromQueue(chest);
            this.plugin.getDatabase().removeChest(loc.toString());
            this.loadedChests.remove(loc);
            if (this.loadedChestsByPlayer.get(chest.getOwner()).size() > 1) {
                this.loadedChestsByPlayer.get(chest.getOwner()).remove(chest);
            } else this.loadedChestsByPlayer.remove(chest.getOwner());
        }
        Logger.debug("Removed SellChest from '" + chest.getOwner() + "' on location: " + "World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ());
    }

    public void removeChest(Chest chest) {
        this.plugin.getDatabase().removeChest(chest.getLocation().toString());
        this.loadedChests.remove(chest.getLocation());
        this.scheduler.removeFromQueue(chest);
        if (this.loadedChestsByPlayer.containsKey(chest.getOwner())) {
            this.loadedChestsByPlayer.get(chest.getOwner()).remove(chest);
        } else this.loadedChestsByPlayer.remove(chest.getOwner());
        Logger.debug("Removed SellChest from '" + chest.getOwner() + "' on location: " + "World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ());
    }

    public ItemStack getChest(int amount) {
        return this.getChest(null, amount);
    }

    public ItemStack getChest(Chest chest, int amount) {
        ItemStack item = new ItemStack(Material.CHEST, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(chestName);
        meta.setLore(Config.get().getStringList("sellchest-lore").stream().map(s -> Lang.formatColors(s.replace("%interval%", TimeUtils.getReadableTime(chest != null ? chest.getInterval() : UpgradeManager.getIntervals()[0])), null)).collect(Collectors.toList()));
        meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "autosell"), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    private void saveChests() {
        Logger.debug("Saving '" + this.loadedChests.size() + "' chests...");
        for (Chest chest : this.loadedChests.values()) {
            this.plugin.getDatabase().saveChest(chest);
        }
    }

    public void disable() {
        this.saveChests();
        if (!this.loadedChests.isEmpty()) this.loadedChests.clear();
        if (!this.loadedChestsByPlayer.isEmpty()) this.loadedChestsByPlayer.clear();
        this.scheduler.stop();
    }

    public int getOwnedChests(Player p) {
        int i = 0;
        for (Chest chest : this.loadedChestsByPlayer.getOrDefault(p.getUniqueId(), new ArrayList<>())) {
            i += chest.getLocation().isDoubleChest() ? 2 : 1;
        }
        return i;
    }

    private static ItemStack createFillItem() {
        Material mat = Material.GRAY_STAINED_GLASS_PANE;

        String material = Config.get().getString("fill-item.material");
        if (material != null) {
            if (Material.getMaterial(material) != null) {
                mat = Material.getMaterial(material);
            } else Logger.warn(String.format("Invalid material for fill-item %s, using default...", material));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String name = Config.get().getString("fill-item.name");
        meta.setDisplayName(name != null ? ChatColor.translateAlternateColorCodes('&', name) : " ");

        item.setItemMeta(meta);
        return item;
    }
}
