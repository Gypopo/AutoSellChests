package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.managers.HologramManager;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.*;
import me.gypopo.autosellchests.util.ChestConfirmation;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.util.SimpleParticle;
import me.gypopo.autosellchests.util.TimeUtils;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.util.EcoType;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PlayerListener implements Listener {

    private final AutoSellChests plugin;
    private ChestConfirmation chestConfirmation;
    private final boolean compatibilityMode;
    private final Sound placeSound;
    private final Sound breakSound;
    private final Sound upgradeSound;
    private float soundVolume = 1F;
    private float soundPitch = 1F;
    //private final boolean v1_20_R4;

    public PlayerListener(AutoSellChests plugin) {
        this.plugin = plugin;

        // Sounds
        try {
            this.soundVolume = Float.parseFloat(Config.get().getString("sound-effects.volume", "1"));
            this.soundPitch = Float.parseFloat(Config.get().getString("sound-effects.pitch", "1"));
        } catch (NumberFormatException e) {
            Logger.warn("Failed to load sound volume settings in config.yml, should be a number");
        }
        this.placeSound = this.getSound("sound-effects.place-chest");
        this.breakSound = this.getSound("sound-effects.pickup-chest");
        this.upgradeSound = this.getSound("sound-effects.upgrade-chest");

        // Chest confirmation effect
        try {
            this.chestConfirmation = ChestConfirmation.valueOf(Config.get().getString("chest-confirmation-effect"));
        } catch (IllegalArgumentException e) {
            this.chestConfirmation = ChestConfirmation.BOSS_BAR;
            Logger.warn("Could not find a valid confirmation effect with name '" + Config.get().getString("chest-confirmation-effect") + "'");
        }

        // Compatibility mode which enables support for placing chests created before 2.4.0
        this.compatibilityMode = Config.get().getBoolean("compatibility-mode");
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        Block clicked = e.getClickedBlock();

        if (clicked == null || clicked.getType() != Material.CHEST) {
            return;
        }

        Chest chest = this.plugin.getManager().getChestByLocation(clicked.getLocation());
        if (chest == null) return;
        if (!chest.getOwner().equals(e.getPlayer().getUniqueId()) && !e.getPlayer().hasPermission("autosellchests.show.all")) {
            e.getPlayer().sendMessage(Lang.NO_PERMISSIONS.get());
            return;
        }

        new InformationScreen(chest, clicked.getLocation()).open(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getBlockPlaced().getType().equals(Material.CHEST)) {
            return;
        }

        // 1.20.5/6 requires the item to be retrieved from the event the same tick, or else it will be AIR
        if (!this.isChest(e.getItemInHand())) {
            this.checkPlacement(e.getPlayer(), e.getBlockPlaced());
            return;
        }

        if (!e.getPlayer().hasPermission("autosellchests.place")) {
            Logger.sendPlayerMessage(e.getPlayer(), Lang.NO_PERMISSIONS.get());
            e.setCancelled(true);
            return;
        }

        ChestSettings settings = this.getSettings(e.getItemInHand());
        int max = this.plugin.getManager().getMaxSell(e.getPlayer());
        if (!e.getPlayer().hasPermission("autosellchests.maxchests.override") &&
                this.plugin.getManager().getOwnedChests(e.getPlayer()) >= max) {
            Logger.sendPlayerMessage(e.getPlayer(), Lang.MAX_SELLCHESTS_REACHED.get().replace("%maxSellChests%", String.valueOf(max)));
            e.setCancelled(true);
            return;
        }

        // Run task on 1 tick delay to check whether this forms a double chest
        Location loc = e.getBlockPlaced().getLocation();
        this.plugin.runTaskLater(() -> {
            if (((org.bukkit.block.Chest) e.getBlockPlaced().getState()).getInventory() instanceof DoubleChestInventory inv) {
                Location original = inv.getLeftSide().getLocation().equals(loc) ? inv.getRightSide().getLocation() : inv.getLeftSide().getLocation();
                Chest left = this.plugin.getManager().getChestByLocation(original);

                // Match the chest owners
                if (left == null || !left.getOwner().equals(e.getPlayer().getUniqueId())) {
                    if (left == null) {
                        Logger.sendPlayerMessage(e.getPlayer(), Lang.CANNOT_FORM_DOUBLE_CHEST.get());
                    } else Logger.sendPlayerMessage(e.getPlayer(), Lang.CANNOT_PLACE_SELL_CHEST_HERE.get());

                    loc.add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, this.plugin.getManager().getChest(settings,1));
                    loc.getBlock().setType(Material.AIR);
                    return;
                }

                // Match the chest settings
                if (!left.getSettings().equals(settings)) {
                    Logger.sendPlayerMessage(e.getPlayer(), Lang.CANNOT_FORM_DOUBLE_CHEST_MISMATCH.get());

                    loc.add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, this.plugin.getManager().getChest(settings,1));
                    loc.getBlock().setType(Material.AIR);
                    return;
                }

                this.plugin.getManager().addChest(loc, left, settings, e.getPlayer());
            } else this.plugin.getManager().addChest(loc, null, settings, e.getPlayer());

            loc.add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(SimpleParticle.WITCH.get(), loc, 10);
            loc.getWorld().spawnParticle(SimpleParticle.DUST.get(), loc, 10, new Particle.DustOptions(Color.RED, 2F));
            if (this.placeSound != null)
                loc.getWorld().playSound(loc, this.placeSound, this.soundVolume, this.soundPitch);
            Logger.sendPlayerMessage(e.getPlayer(), Lang.SELLCHEST_PLACED.get().replace("%chest-name%", this.plugin.getManager().getDefaultChestName()));
            this.chestConfirmation.playEffect(e.getPlayer());

            loc.subtract(0.5, 0.5, 0.5); // This line is needed, it caused me some headaches :/
        }, loc, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.CHEST) {
            return;
        }

        Chest chest = this.plugin.getManager().getChestByLocation(e.getBlock().getLocation());
        if (chest == null) {
            //Logger.debug("Chest is null for location: " + e.getBlock().getLocation());
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        if (e.getClickedInventory().getHolder() instanceof InformationScreen inv) {
            Chest chest = inv.getChest();
            Location loc = inv.getSelectedChest();
            if (e.getSlot() == this.plugin.getInventoryManager().getMainInv().getSlot("destroy-item")) {
                if (!e.getWhoClicked().hasPermission("autosellchests.pickup")) {
                    Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.NO_PERMISSIONS.get());
                    e.setCancelled(true);
                    return;
                }

                if (chest.getOwner().equals(e.getWhoClicked().getUniqueId()) || e.getWhoClicked().hasPermission("autosellchests.break")) {
                    e.getWhoClicked().closeInventory();
                    this.plugin.getManager().removeChest(new ChestLocation(loc));
                    Arrays.stream(((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory().getContents()).forEach(item -> {
                        if (item != null && item.getType() != Material.AIR) loc.getWorld().dropItemNaturally(loc, item);
                    });
                    loc.add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, this.plugin.getManager().getChest(chest, 1));
                    // In 1.16.5, Block#setType(AIR) causes the chest to also drop its contents, so make sure its empty or the items will be duplicated
                    if (this.plugin.version == 116)
                        ((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory().clear();
                    loc.getBlock().setType(Material.AIR);
                    loc.getWorld().spawnParticle(SimpleParticle.CLOUD.get(), loc, 15);
                    if (this.breakSound != null)
                        loc.getWorld().playSound(loc, this.breakSound, this.soundVolume, this.soundPitch);
                    Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.SELLCHEST_BROKEN.get().replace("%chest-name%", this.plugin.getManager().getDefaultChestName()));
                } else {
                    Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.CANNOT_REMOVE_SELL_CHEST.get());
                }
            } else if (e.getSlot() == this.plugin.getInventoryManager().getMainInv().getSlot("settings-item")) {
                if (chest.getOwner().equals(e.getWhoClicked().getUniqueId())) {
                    new SettingsScreen(chest, loc).open((Player) e.getWhoClicked());
                } else e.getWhoClicked().sendMessage(Lang.NO_PERMISSIONS.get());
            } else if (e.getSlot() == this.plugin.getInventoryManager().getMainInv().getSlot("upgrade-item")) {
                if (chest.getOwner().equals(e.getWhoClicked().getUniqueId())) {
                    new UpgradeScreen(chest, loc).open((Player) e.getWhoClicked());
                } else e.getWhoClicked().sendMessage(Lang.NO_PERMISSIONS.get());
            } else if (e.getSlot() == this.plugin.getInventoryManager().getMainInv().getSlot("claimable-item")) {
                if (chest.getOwner().equals(e.getWhoClicked().getUniqueId())) {
                    if (!chest.getClaimAble().isEmpty())
                        new ClaimProfitsScreen(chest, loc).open((Player) e.getWhoClicked());
                } else e.getWhoClicked().sendMessage(Lang.NO_PERMISSIONS.get());
            }
            e.setCancelled(true);
        } else if (e.getClickedInventory().getHolder() instanceof SettingsScreen inv) {
            Chest chest = inv.getChest();
            if (e.getSlot() == this.plugin.getInventoryManager().getSettingsInv().getSlot("logging-item")) {
                chest.setLogging(!chest.isLogging());
                inv.updateInventory((Player) e.getWhoClicked());
            } else if (e.getSlot() == this.plugin.getInventoryManager().getSettingsInv().getSlot("rename-item")) {
                inv.update();
                try {
                    new AnvilGUI.Builder()
                            .onClick((i, state) -> {
                                if (!state.getText().isEmpty())
                                    chest.setName(Lang.formatColors(state.getText(), null));

                                this.plugin.getHologramManager().updateHologram(chest);
                                inv.updateInventory((Player) e.getWhoClicked());
                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            })
                            .text(chest.getName())
                            .itemLeft(new ItemStack(Material.PAPER))
                            .title(Lang.ENTER_NAME_MENU_TITLE.get())
                            .plugin(this.plugin)
                            .open((Player) e.getWhoClicked());
                } catch (Exception ex) {
                    Logger.warn("Failed to open rename chest GUI, your server version(" + Bukkit.getBukkitVersion() + ") is not supported!");
                }
            } else if (e.getSlot() == this.plugin.getInventoryManager().getSettingsInv().getSlot("hologram-item")) {
                this.plugin.getHologramManager().removeHologram(chest);
                chest.setHologram(!chest.isHologram());
                inv.updateInventory((Player) e.getWhoClicked());
            }
            e.setCancelled(true);
        } else if (e.getClickedInventory().getHolder() instanceof UpgradeScreen inv) {
            Player p = (Player) e.getWhoClicked();
            Chest chest = inv.getChest();
            if (e.getSlot() == this.plugin.getInventoryManager().getUpgradeInv().getSlot("interval-upgrade-item")) {
                int nextInterval = chest.getIntervalUpgrade()+1;
                ChestUpgrade nextUpgrade = UpgradeManager.getIntervalUpgrade(nextInterval);
                if (nextUpgrade != null && nextUpgrade.buy(p, chest.isDoubleChest())) {
                    this.plugin.getManager().updateChestInterval(chest, nextInterval);
                    if (this.plugin.getHologramManager().isEnabled() && chest.isHologram())
                        this.plugin.getHologramManager().updateHologram(chest);

                    inv.updateInventory(p);

                    Logger.sendPlayerMessage(p, Lang.CHEST_INTERVAL_UPGRADED.get()
                            .replace("%upgrade-name%", nextUpgrade.getName())
                            .replace("%upgrade-cost%", nextUpgrade.getPrice(chest.isDoubleChest()))
                            .replace("%interval%", TimeUtils.getReadableTime(UpgradeManager.getIntervals()[nextInterval])));

                    inv.getSelectedChest().getWorld().spawnParticle(SimpleParticle.WITCH.get(), inv.getSelectedChest(), 15);
                    if (this.upgradeSound != null)
                        inv.getSelectedChest().getWorld().playSound(inv.getSelectedChest(), this.upgradeSound, this.soundVolume, this.soundPitch);
                }
            } else if (e.getSlot() == this.plugin.getInventoryManager().getUpgradeInv().getSlot("multiplier-upgrade-item")) {
                int nextMultiplier = chest.getMultiplierUpgrade()+1;
                ChestUpgrade nextUpgrade = UpgradeManager.getMultiplierUpgrade(nextMultiplier);
                if (nextUpgrade != null && nextUpgrade.buy(p, chest.isDoubleChest())) {
                    chest.setMultiplierUpgrade(nextMultiplier);
                    chest.setMultiplier(UpgradeManager.getMultipliers()[nextMultiplier]);
                    if (this.plugin.getHologramManager().isEnabled() && chest.isHologram())
                        this.plugin.getHologramManager().updateHologram(chest);

                    inv.updateInventory(p);

                    Logger.sendPlayerMessage(p, Lang.CHEST_MULTIPLIER_UPGRADED.get()
                            .replace("%upgrade-name%", nextUpgrade.getName())
                            .replace("%upgrade-cost%", nextUpgrade.getPrice(chest.isDoubleChest()))
                            .replace("%multiplier%", String.valueOf(UpgradeManager.getMultipliers()[nextMultiplier])));

                    inv.getSelectedChest().getWorld().spawnParticle(SimpleParticle.WITCH.get(), inv.getSelectedChest(), 15);
                    if (this.upgradeSound != null)
                        inv.getSelectedChest().getWorld().playSound(inv.getSelectedChest(), this.upgradeSound, this.soundVolume, this.soundPitch);
                }
            }
            e.setCancelled(true);
        } else if (e.getClickedInventory().getHolder() instanceof ClaimProfitsScreen inv) {
            Chest chest = inv.getChest();
            if (chest.getClaimAble().size() >= (e.getRawSlot() + 1)) {
                EcoType type = new ArrayList<>(chest.getClaimAble().keySet()).get(e.getRawSlot());
                if (EconomyShopGUIHook.getEcon(type).getType().equals(type)) {
                    EconomyShopGUIHook.getEcon(type).depositBalance((Player) e.getWhoClicked(), chest.getClaimAble().get(type));
                    chest.claim(type);
                    if (chest.getClaimAble().isEmpty()) {
                        inv.update();
                        new InformationScreen(chest, inv.getSelectedChest()).open((Player) e.getWhoClicked());
                    } else
                        inv.updateInventory((Player) e.getWhoClicked());
                } else Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.CANNOT_CLAIM_PROFIT.get()); // EconomyType not active/not found
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ChestInventory inv))
            return;

        if (inv.isUpdatingInventory())
            return;

        this.plugin.runTaskLater(() -> {
            new InformationScreen(inv.getChest(), inv.getSelectedChest()).open((Player) e.getPlayer());

            ((Player) e.getPlayer()).updateInventory();
        }, 1L);
    }

    private Sound getSound(String sound) {
        if (!Config.get().getString(sound).isEmpty()) {
            try {
                return this.getSoundByName(Config.get().getString(sound));
            } catch (IllegalArgumentException | NullPointerException e) {
                // No sound found
                Logger.warn("Failed to find a sound effect with name '" + Config.get().getString(sound) + "'");
                return this.getSoundByName(Config.get().getDefaults().getString(sound));
            }
        }
        return null;
    }

    private Sound getSoundByName(String sound) throws IllegalArgumentException {
        try {
            Class<?> clazz = Class.forName("org.bukkit.Sound");
            Method c = clazz.getDeclaredMethod("valueOf", String.class);
            c.setAccessible(true);

            return (Sound) c.invoke(null, sound);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException)
                throw (IllegalArgumentException) e;

            Logger.warn("Failed to load sound effect for " + sound);
            return null;
        }
    }

    private SoundCategory getSoundCategory(Sound sound) {
        if (sound != null) {
            try {
                SoundCategory.valueOf(sound.getKey().getKey().split("\\.")[0]);
            } catch (IllegalArgumentException e) {
                // No sound category found
                //Logger.warn("Failed to find a sound category for sound effect '" + sound.name() + "'");
            }
        }
        return SoundCategory.AMBIENT;
    }

    private ChestSettings getSettings(ItemStack stack) {
        if (stack.hasItemMeta()) {
            String data = stack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(AutoSellChests.getInstance(), "autosell-data"), PersistentDataType.STRING);
            if (data == null)
                return new ChestSettings();

            return new ChestSettings(data);
        } else return new ChestSettings();
    }

    private boolean isChest(ItemStack item) {
        if (!item.hasItemMeta())
            return false;

        if (this.compatibilityMode) {
            return item.getItemMeta().getDisplayName().equals(ChestManager.chestName);
        } else {
            try {
                return item.getItemMeta().getPersistentDataContainer().get(this.plugin.getKey(), PersistentDataType.INTEGER) == 1;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void checkPlacement(Player player, Block block) {
        Location loc = block.getLocation();
        this.plugin.runTaskLater(() -> {
            if (((org.bukkit.block.Chest) block.getState()).getInventory() instanceof DoubleChestInventory) {
                DoubleChestInventory inv = (DoubleChestInventory) ((org.bukkit.block.Chest) block.getState()).getInventory();
                Location original = inv.getLeftSide().getLocation().equals(loc) ? inv.getRightSide().getLocation() : inv.getLeftSide().getLocation();
                if (this.plugin.getManager().getChestByLocation(original) != null) {
                    Logger.sendPlayerMessage(player, Lang.CANNOT_PLACE_CHEST_AGAINST_SELL_CHEST.get());
                    block.breakNaturally();
                }
            }

        }, loc, 1L);
    }
}
