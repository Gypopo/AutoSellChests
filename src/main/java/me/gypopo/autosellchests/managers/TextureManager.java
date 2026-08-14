package me.gypopo.autosellchests.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.ChunkLoc;
import me.gypopo.autosellchests.objects.Location;
import me.gypopo.autosellchests.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Hides the real sell chest block from players using ProtocolLib and shows a fake
// block/item using DisplayItem
public class TextureManager {

    private final AutoSellChests plugin;
    private static final NamespacedKey KEY = new NamespacedKey(AutoSellChests.getInstance(), "custom-chest-texture");
    private final static WrappedBlockData FAKE_BLOCK = WrappedBlockData.createData(Material.BARRIER);
    private final static WrappedBlockData REAL_BLOCK = WrappedBlockData.createData(Material.AIR);

    private boolean enabled;
    private ItemStack displayItem;
    private ItemDisplay.ItemDisplayTransform transform;
    private float scale;
    private float yaw;
    private float pitch;

    // Just for quick access
    private final Map<ChestLocation, Chest> chestsByPosition = new HashMap<>();
    // Stores the display entities that are currently loaded for the chests
    private final Map<Integer, ItemDisplay> loadedDisplays = new HashMap<>();

    public TextureManager(AutoSellChests plugin) {
        this.plugin = plugin;
        this.displayItem = this.buildDisplayItem();
        if (this.displayItem == null)
            return;

        String transformName = Config.get().getString("sell-chest-item.custom-block-texture.transform", "FIXED");
        try {
            this.transform = ItemDisplay.ItemDisplayTransform.valueOf(transformName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid transform '" + transformName + "' for sell-chest-item.custom-block-texture.transform, defaulting to FIXED");
            this.transform = ItemDisplay.ItemDisplayTransform.FIXED;
        }

        // If its a item like PAPER, make it have a scale of only 0.5 while blocks have a scale of 2
        this.scale = (float) Config.get().getDouble("sell-chest-item.custom-block-texture.scale", this.displayItem.getType().isBlock() ? 2 : 0.5);
        // If the custom texture item is a block, use a rotation of 0:0, while items(like PAPER) have a rotation of 90:-90
        String rotation = Config.get().getString("sell-chest-item.custom-block-texture.rotation", this.displayItem.getType().isBlock() ? "0:0" : "90:-90");
        try {
            this.yaw = Float.parseFloat(rotation.split(":")[0]);
            this.pitch = Float.parseFloat(rotation.split(":")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Logger.warn("Failed to load rotation of custom chest texture for " + rotation + ", should be '<yaw>:<pitch>'");
            this.yaw = 0;
            this.pitch = 0;
        }

        this.enabled = true;
        this.registerPacketListeners();

        this.plugin.runTaskLater(this::cleanupDisplays, 1L);

        Logger.info("Successfully enabled custom chest textures using ProtocolLib");
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void reload() {
        if (!this.enabled)
            return;

        this.displayItem = this.buildDisplayItem();
        if (this.displayItem == null) {
            this.enabled = false;
            return;
        }

        String transformName = Config.get().getString("sell-chest-item.custom-block-texture.transform", "FIXED");
        try {
            this.transform = ItemDisplay.ItemDisplayTransform.valueOf(transformName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid transform '" + transformName + "' for sell-chest-item.custom-block-texture.transform, defaulting to FIXED");
            this.transform = ItemDisplay.ItemDisplayTransform.FIXED;
        }

        // If its a item like PAPER, make it have a scale of only 0.5 while blocks have a scale of 2
        this.scale = (float) Config.get().getDouble("sell-chest-item.custom-block-texture.scale", this.displayItem.getType().isBlock() ? 2 : 0.5);
        // If the custom texture item is a block, use a rotation of 0:0, while items(like PAPER) have a rotation of 90:-90
        String rotation = Config.get().getString("sell-chest-item.custom-block-texture.rotation", this.displayItem.getType().isBlock() ? "0:0" : "90:-90");
        try {
            this.yaw = Float.parseFloat(rotation.split(":")[0]);
            this.pitch = Float.parseFloat(rotation.split(":")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Logger.warn("Failed to load rotation of custom chest texture for " + rotation + ", should be '<yaw>:<pitch>'");
            this.yaw = 0;
            this.pitch = 0;
        }

        this.chestsByPosition.clear();

        for (ItemDisplay display : this.loadedDisplays.values()) {
            display.remove();
        }

        this.loadedDisplays.clear();

        for (Chest chest : this.plugin.getManager().getLoadedChests().values()) {
            if (chest.isLoaded()) {
                this.showTexture(chest);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.sendTexturePacket(player, chest);
                }
            }
        }
    }

    // Called whenever a chest becomes active (placed, or its chunk loads)
    public void showTexture(Chest chest) {
        if (!this.enabled)
            return;

        this.chestsByPosition.put(chest.getLocation(), chest);

        if (!this.loadedDisplays.containsKey(chest.getId()))
            this.attachDisplay(chest);

        /*
        this.plugin.getScheduler().runTaskForRegionLater(chest.getLocation().getLeftLocation().toLoc(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.sendTexturePacket(player, chest);
            }
        }, 1L);

         */
    }

    // Called whenever a chest stops being an active sell chest (broken, exploded, cleaned up)
    public void removeTexture(Chest chest) {
        if (!this.enabled)
            return;

        this.chestsByPosition.remove(chest.getLocation());

        ItemDisplay display = this.loadedDisplays.remove(chest.getId());
        if (display != null)
            this.plugin.runTaskLater(display::remove, chest.getLocation().getLeftLocation().toLoc(), 1L);

        this.plugin.getScheduler().runTaskForRegionLater(chest.getLocation().getLeftLocation().toLoc(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.hideTexturePacket(player, chest);
            }
        }, 1L);
    }

    /*
    public void removeTexture(ChestLocation loc) {
        if (!this.enabled)
            return;

        this.chestsByPosition.remove(loc);

        ItemDisplay display = this.loadedDisplays.remove(chest.getId());
        if (display != null)
            this.plugin.runTaskLater(display::remove, loc.toLoc(), 1L);
    }

     */

    // TODO: Do ItemDisplay entities cause the chunck to stay loaded? Else might destroy the entity and recreate it
    public void unloadDisplay(Chest chest) {
        if (!this.enabled) return;

        this.loadedDisplays.remove(chest.getId());
    }

    private void registerPacketListeners() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.MONITOR, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (chestsByPosition.isEmpty())
                    return;

                Integer chunkX = event.getPacket().getIntegers().readSafely(0);
                Integer chunkZ = event.getPacket().getIntegers().readSafely(1);
                if (chunkX == null || chunkZ == null)
                    return;

                Player player = event.getPlayer();
                ChunkLoc chunkLoc = new ChunkLoc(player.getWorld().getName(), chunkX, chunkZ);
                for (Chest chest : chestsByPosition.values()) {
                    if (chunkLoc.contains(chest.getLocation()))
                        sendTexturePacketLater(player, chest);
                }
            }
        });

        manager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.MONITOR, PacketType.Play.Server.BLOCK_CHANGE, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (chestsByPosition.isEmpty())
                    return;

                WrappedBlockData bData = event.getPacket().getBlockData().readSafely(0);
                if (bData == null || bData.getType() != Material.CHEST)
                    return; // Only check packets for chests

                Player player = event.getPlayer();
                if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                    BlockPosition pos = event.getPacket().getBlockPositionModifier().readSafely(0);
                    if (pos == null)
                        return;

                    Chest chest = chestsByPosition.get(new ChestLocation(player.getWorld().getName(), pos.getX(), pos.getY(), pos.getZ()));
                    if (chest != null)
                        sendTexturePacketLater(player, chest);
                } else {
                    MultiBlockChangeInfo[] data = event.getPacket().getMultiBlockChangeInfoArrays().readSafely(0);
                    if (data == null)
                        return;

                    String world = player.getWorld().getName();
                    for (MultiBlockChangeInfo info : data) {
                        Chest chest = chestsByPosition.get(new ChestLocation(world, info.getAbsoluteX(), info.getY(), info.getAbsoluteZ()));
                        if (chest != null)
                            sendTexturePacketLater(player, chest);
                    }
                }
            }
        });
    }

    private void sendTexturePacketLater(Player player, Chest chest) {
        this.plugin.getScheduler().runTaskForRegionLater(chest.getLocation().getLeftLocation().toLoc(), () -> {
            this.sendTexturePacket(player, chest);
        }, 1L);
    }

    // Hides the actual chest texture by sending a packet to a player making it think that the chest is a barrier
    private void sendTexturePacket(Player player, Chest chest) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        ChestLocation loc = chest.getLocation();

        PacketContainer packet = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(loc.getLeftLocation().x, loc.getLeftLocation().y, loc.getLeftLocation().z));
        packet.getBlockData().write(0, FAKE_BLOCK);

        PacketContainer doubleChestPacket = null;
        if (chest.isDoubleChest()) {
            doubleChestPacket = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            doubleChestPacket.getBlockPositionModifier().write(0, new BlockPosition(loc.getRightLocation().x, loc.getRightLocation().y, loc.getRightLocation().z));
            doubleChestPacket.getBlockData().write(0, FAKE_BLOCK);
        }

        try {
            manager.sendServerPacket(player, packet);
            if (doubleChestPacket != null)
                manager.sendServerPacket(player, doubleChestPacket);
            System.out.println("Send texture packet of chest " + chest.getLocation().toString() + " for " + player.getName());
        } catch (Exception e) {
            Logger.error("Failed to send custom chest texture packet(s) to '" + player.getName() + "': " + e.getMessage());
        }
    }

    private void hideTexturePacketLater(Player player, Chest chest) {
        this.plugin.getScheduler().runTaskForRegionLater(chest.getLocation().getLeftLocation().toLoc(), () -> {
            this.hideTexturePacket(player, chest);
        }, 1L);
    }

    private void hideTexturePacket(Player player, Chest chest) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        ChestLocation loc = chest.getLocation();

        PacketContainer packet = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(loc.getLeftLocation().x, loc.getLeftLocation().y, loc.getLeftLocation().z));
        packet.getBlockData().write(0, REAL_BLOCK);

        PacketContainer doubleChestPacket = null;
        if (chest.isDoubleChest()) {
            doubleChestPacket = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
            doubleChestPacket.getBlockPositionModifier().write(0, new BlockPosition(loc.getRightLocation().x, loc.getRightLocation().y, loc.getRightLocation().z));
            doubleChestPacket.getBlockData().write(0, REAL_BLOCK);
        }

        try {
            manager.sendServerPacket(player, packet);
            if (doubleChestPacket != null)
                manager.sendServerPacket(player, doubleChestPacket);
        } catch (Exception e) {
            Logger.error("Failed to hide custom chest texture packet(s) from '" + player.getName() + "': " + e.getMessage());
        }
    }

    private void attachDisplay(Chest chest) {
        ItemDisplay display = this.getDisplay(chest);
        if (display != null) {
            this.loadedDisplays.put(chest.getId(), display);
            return;
        }

        Location left = chest.getLocation().getLeftLocation();
        org.bukkit.Location loc = left.toLoc().add(0.5, this.displayItem.getType().isBlock() ? 0.5 : 0, 0.5);

        World world = loc.getWorld();
        if (world == null)
            return;

        ItemDisplay itemDisplay = world.spawn(loc, ItemDisplay.class, entity -> {
            entity.setItemStack(this.displayItem);
            entity.setItemDisplayTransform(this.transform);
            Transformation t = entity.getTransformation();
            entity.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(this.scale, this.scale, this.scale),
                    t.getRightRotation()));
            entity.setRotation(this.yaw, this.pitch);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, chest.getId());
        });

        this.loadedDisplays.put(chest.getId(), itemDisplay);
    }

    private ItemDisplay getDisplay(Chest chest) {
        Location loc = chest.getLocation().getLeftLocation();
        World world = Bukkit.getWorld(loc.world);
        if (world == null || !world.isChunkLoaded(loc.x >> 4, loc.z >> 4))
            return null;

        Chunk chunk = world.getChunkAt(loc.x >> 4, loc.z >> 4);
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof ItemDisplay)) continue;

            Integer id = entity.getPersistentDataContainer().get(KEY, PersistentDataType.INTEGER);
            if (id != null && id == chest.getId())
                return (ItemDisplay) entity;
        }
        return null;
    }

    // Removes any ItemDisplay entities that weren't removed correctly when the chest got removed or server was offline
    private void cleanupDisplays() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof ItemDisplay)) continue;

                    Integer chestId = entity.getPersistentDataContainer().get(KEY, PersistentDataType.INTEGER);
                    if (chestId == null || this.loadedDisplays.containsKey(chestId)) continue;

                    if (this.plugin.getManager().getChestByID(chestId) == null) {
                        entity.remove();
                        removed++;
                    } else {
                        this.loadedDisplays.put(chestId, (ItemDisplay) entity);
                    }
                }
            }
        }

        if (removed > 0)
            Logger.debug("Removed " + removed + " custom chest texture entities from sell chests that no longer exist");
    }

    private ItemStack buildDisplayItem() {
        String materialName = Config.get().getString("sell-chest-item.custom-block-texture.material", "CRYING_OBSIDIAN");
        Material material = Material.getMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null) {
            Logger.error("Invalid material '" + materialName + "' for sell-chest-item.custom-block-texture.material, disabling feature...");
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String itemModel = Config.get().getString("sell-chest-item.custom-block-texture.item-model");
            if (itemModel != null && !itemModel.isEmpty()) {
                NamespacedKey model = NamespacedKey.fromString(itemModel.toLowerCase(Locale.ROOT));
                if (model != null) {
                    meta.setItemModel(model);
                } else
                    Logger.warn("Invalid item-model '" + itemModel + "' for sell-chest-item.custom-block-texture.item-model, ignoring it");
            }

            int customModelData = Config.get().getInt("sell-chest-item.custom-block-texture.CustomModelData", -1);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}