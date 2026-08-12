package me.gypopo.autosellchests.protection;

import com.gpplugins.gplib.protection.ProtectionFlag;
import com.gpplugins.gplib.protection.ProtectionService;
import com.gpplugins.gplib.protection.hooks.*;
import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.protection.hooks.WorldEditHook;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.autosellchests.files.Lang;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.util.Arrays;

public class ProtectionManager {

    private static final ProtectionFlag PLACE_CHEST = ProtectionFlag.builder("place-sell-chests")
            .defaultAllow(false)
            .displayName("Place sell chests")
            .description(Arrays.asList("§7Allow placement of auto sell chests?"))
            .icon(Material.CHEST)
            .build();

    private final AutoSellChests plugin;
    private final ProtectionService service = new ProtectionService();

    public ProtectionManager(AutoSellChests plugin) {
        this.plugin = plugin;
        this.onLoad();
    }

    private void onLoad() {
        this.service.registerFlags(PLACE_CHEST);

        PluginManager pm = this.plugin.getServer().getPluginManager();
        if (pm.getPlugin("WorldGuard") != null)
            this.service.addHook(new WorldGuardProtection());
        if (pm.getPlugin("Lands") != null)
            this.service.addHook(new LandsProtection(this.plugin));
    }

    public void init() {
        PluginManager pm = this.plugin.getServer().getPluginManager();
        if (pm.getPlugin("WorldGuard") != null) {
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "WorldGuard"));
        }

        if (pm.getPlugin("Lands") != null) {
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "Lands"));
        }

        if (pm.getPlugin("ChestProtect") != null) {
            this.service.addHook(new ChestLockProtection());
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "ChestProtect"));
        }

        if (pm.getPlugin("Towny") != null) {
            this.service.addHook(new TownyProtection(
                    Config.get().getBoolean("protection.towny.allow-wild-chest-placement"),
                    Config.get().getBoolean("protection.towny.only-allow-chests-in-shop-plots")));
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "Towny"));
        }

        if (Config.get().getBoolean("protection.use-place-fallback-integration")) {
            this.service.addHook(new FallbackProtection(
                    Config.get().getBoolean("protection.use-place-fallback-integration"), false));
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "Fallback"));
        }

        // Not really a protection plugin, but listen to block changes and see if the chest got replaced to prevent 'ghost' chests
        if (pm.getPlugin("WorldEdit") != null || pm.getPlugin("FastAsyncWorldEdit") != null) {
            new WorldEditHook(this.plugin);
            Logger.info(Lang.PROTECTION_PLUGIN_LOADED.get().replace("%plugin%", "WorldEdit"));
        }
    }

    // Returns if the player can place a sell chest at this location
    public boolean canPlace(Player p, Block block) {
        return this.service.testCreate(p, block, PLACE_CHEST);
    }
}