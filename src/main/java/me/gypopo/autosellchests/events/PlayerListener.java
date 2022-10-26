package me.gypopo.autosellchests.events;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.ChestManager;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.InformationScreen;
import me.gypopo.autosellchests.util.ChestConfirmation;
import me.gypopo.autosellchests.util.Logger;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;

public class PlayerListener implements Listener {

    private AutoSellChests plugin;
    private ChestConfirmation chestConfirmation;
    private final Sound placeSound;
    private final Sound breakSound;
    private final long soundVolume;
    private final long soundPitch;

    public PlayerListener(AutoSellChests plugin) {
        this.plugin = plugin;

        // Sounds
        this.soundVolume = Config.get().getLong("sound-effects.volume");
        this.soundPitch = Config.get().getLong("sound-effects.pitch");
        this.placeSound = this.getSound("sound-effects.place-chest");
        this.breakSound = this.getSound("sound-effects.pickup-chest");

        // Chest confirmation effect
        try {
            this.chestConfirmation = ChestConfirmation.valueOf(Config.get().getString("chest-confirmation-effect"));
        } catch (IllegalArgumentException e) {
            this.chestConfirmation = ChestConfirmation.BOSS_BAR;
            Logger.warn("Could not find a valid confirmation effect with name '" + Config.get().getString("chest-confirmation-effect") + "'");
        }
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
        if (chest == null) {
            return;
        }

        new InformationScreen(chest, clicked.getLocation()).open(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getBlockPlaced().getType().equals(Material.CHEST)) {
            return;
        }

        if (!e.getItemInHand().hasItemMeta() || !e.getItemInHand().getItemMeta().getDisplayName().equals(ChestManager.chestName)) {
            return;
        }

        if (this.plugin.getManager().getOwnedChests(e.getPlayer()) >= ChestManager.maxSellChestsPlayer && !e.getPlayer().hasPermission("autosellchests.maxchests.override")) {
            Logger.sendPlayerMessage(e.getPlayer(), Lang.MAX_SELLCHESTS_REACHED.get().replace("%maxSellChests%", String.valueOf(ChestManager.maxSellChestsPlayer)));
            e.setCancelled(true);
            return;
        }

        Location loc = e.getBlockPlaced().getLocation();
        this.plugin.runTaskLater(() -> {
            if (((org.bukkit.block.Chest)e.getBlockPlaced().getState()).getInventory() instanceof DoubleChestInventory) {
                DoubleChestInventory inv = (DoubleChestInventory) ((org.bukkit.block.Chest)e.getBlockPlaced().getState()).getInventory();
                Location original = inv.getLeftSide().getLocation().equals(loc) ? inv.getRightSide().getLocation() : inv.getLeftSide().getLocation();
                System.out.println(original);
                if (this.plugin.getManager().getChestByLocation(original) == null) {
                    Logger.sendPlayerMessage(e.getPlayer(), Lang.CANNOT_FORM_DOUBLE_CHEST.get());
                    return;
                }
                this.plugin.getManager().addChest(new ChestLocation(original, loc), e.getPlayer());
            } else this.plugin.getManager().addChest(new ChestLocation(loc), e.getPlayer());

            loc.add(0.5, 0.5, 0.5);
            loc.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 10);
            loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, new Particle.DustOptions(Color.RED, 2F));
            if (this.placeSound != null) loc.getWorld().playSound(loc, this.placeSound, this.getSoundCategory(this.placeSound), this.soundVolume, this.soundPitch);
            Logger.sendPlayerMessage(e.getPlayer(), Lang.SELLCHEST_PLACED.get());
            this.chestConfirmation.playEffect(e.getPlayer());

            loc.subtract(0.5, 0.5, 0.5); // This line is needed, it caused me some headaches :/
        }, 1);
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
        if (e.getClickedInventory() != null && e.getClickedInventory().getHolder() instanceof InformationScreen) {
            e.setCancelled(true);
            if (e.getSlot() == 31) {
                Chest chest = ((InformationScreen) e.getClickedInventory().getHolder()).getChest();
                if (chest.getOwner().equals(e.getWhoClicked().getUniqueId()) || e.getWhoClicked().hasPermission("autosellchests.break")) {
                    e.getWhoClicked().closeInventory();
                    Location loc = ((InformationScreen) e.getClickedInventory().getHolder()).getSelectedChest();
                    this.plugin.getManager().removeChest(new ChestLocation(loc));
                    Arrays.stream(((org.bukkit.block.Chest) loc.getBlock().getState()).getBlockInventory().getContents()).forEach(item -> {
                        if (item != null && item.getType() != Material.AIR) loc.getWorld().dropItemNaturally(loc, item);
                    });
                    loc.add(0.5, 0.5, 0.5);
                    loc.getWorld().dropItemNaturally(loc, this.plugin.getManager().getChest(1));
                    loc.getBlock().setType(Material.AIR);
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc, 15);
                    if (this.breakSound != null) loc.getWorld().playSound(loc, this.breakSound, this.getSoundCategory(this.breakSound), this.soundVolume, this.soundPitch);
                    Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.SELLCHEST_BROKEN.get());
                } else {
                    Logger.sendPlayerMessage((Player) e.getWhoClicked(), Lang.CANNOT_REMOVE_SELL_CHEST.get());
                }
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof InformationScreen) {
            this.plugin.runTaskLater(() -> ((Player) e.getPlayer()).updateInventory(), 1);
        }
    }

    private Sound getSound(String sound) {
        if (!Config.get().getString(sound).isEmpty()) {
            try {
                return Sound.valueOf(Config.get().getString(sound));
            } catch (IllegalArgumentException | NullPointerException e) {
                // No sound found
                Logger.warn("Failed to find a sound effect with name '" + Config.get().getString(sound) + "'");
                return Sound.valueOf(Config.get().getDefaults().getString(sound));
            }
        }
        return null;
    }

    private SoundCategory getSoundCategory(Sound sound) {
        if (sound != null) {
            try {
                SoundCategory.valueOf(sound.getKey().getKey().split("\\.")[0]);
            } catch (IllegalArgumentException e) {
                // No sound category found
                Logger.warn("Failed to find a sound category for sound effect '" + sound.name() + "'");
            }
        }
        return SoundCategory.AMBIENT;
    }
}
