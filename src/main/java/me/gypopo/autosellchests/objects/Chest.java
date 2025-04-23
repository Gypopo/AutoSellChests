package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.upgrades.ChestInterval;
import me.gypopo.autosellchests.objects.upgrades.PriceMultiplier;
import me.gypopo.autosellchests.util.Logger;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Chest {

    private final int id;
    private String displayname;
    private final ChestLocation location;
    private final UUID owner;
    private boolean logging;
    private int itemsSold;
    private final Map<EcoType, Double> income;
    private final Map<EcoType, Double> claimAble;
    private int intervalUpgrade;
    private int multiplierUpgrade;

    private long interval; // The current recurring interval of this chest in millis
    private long nextInterval; // The time the chest is next sold in millis
    private double multiplier; // The current sell price multiplier for this chest

    public Chest(int id, String location, String owner, int itemsSold, String income, String claimAble, String settings, String displayname) {
        this.id = id;
        this.location = new ChestLocation(location);
        this.owner = UUID.fromString(owner);
        this.itemsSold = itemsSold;
        this.income = this.loadPrices(income);
        this.claimAble = this.loadPrices(claimAble);
        this.logging = settings == null || settings.split("\\|")[0].equals("1");
        this.displayname = displayname == null ? Lang.formatColors(Config.get().getString("default-chest-name").replace("%id%", String.valueOf(id)), null) : displayname.replace("%id%", String.valueOf(id));
        this.intervalUpgrade = settings == null ? 0 : this.getIntervalLevel(settings);
        this.multiplierUpgrade = settings == null ? 0 : this.getMultiplierLevel(settings);

        this.interval = UpgradeManager.getIntervals()[UpgradeManager.intervalUpgrades ? this.intervalUpgrade : 0];
        this.multiplier = UpgradeManager.getMultipliers()[UpgradeManager.multiplierUpgrades ? this.multiplierUpgrade : 0];
    }

    public Chest(int id, ChestLocation location, Player owner, int itemsSold, Map<EcoType, Double> income, Map<EcoType, Double> claimAble, boolean logging, int intervalUpgrade, int multiplierUpgrade/*, double multiplier*/, String displayname) {
        this.id = id;
        this.location = location;
        this.owner = owner.getUniqueId();
        this.itemsSold = itemsSold;
        this.income = income;
        this.claimAble = claimAble;
        this.logging = logging;
        this.displayname = displayname.replace("%id%", String.valueOf(id));
        this.intervalUpgrade = intervalUpgrade;
        this.multiplierUpgrade = multiplierUpgrade;

        this.interval = UpgradeManager.getIntervals()[UpgradeManager.intervalUpgrades ? this.intervalUpgrade : 0];
        this.multiplier = UpgradeManager.getMultipliers()[UpgradeManager.multiplierUpgrades ? this.multiplierUpgrade : 0];
    }

    private int getIntervalLevel(String settings) {
        try {
            return Integer.parseInt(settings.split("\\|")[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Logger.warn("Failed to load interval level for '" + settings + "' for chest " + this.id + ", using default...");
            return 0;
        }
    }

    private int getMultiplierLevel(String settings) {
        try {
            return Integer.parseInt(settings.split("\\|")[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Logger.warn("Failed to load multiplier level for '" + settings + "' for chest " + this.id + ", using default...");
            return 0;
        }
    }

    public void addItemsSold(int itemsSold) {
        this.itemsSold += itemsSold;
    }

    public void addIncome(Map<EcoType, Double> income) {
        income.forEach((key, value) -> this.income.put(key, this.income.getOrDefault(key, 0.0)+value));
    }

    public void addClaimAble(EcoType type, Double price) {
        this.claimAble.put(type, this.claimAble.getOrDefault(type, 0d) + price);
    }

    public void claim(EcoType claimed) {
        this.claimAble.remove(claimed);
    }

    public Map<EcoType, Double> getClaimAble() {
        return this.claimAble;
    }

    public String getClaimAble(String message) {
        return AutoSellChests.getInstance().formatPrices(this.claimAble, message);
    }

    public String getClaimAbleRaw() {
        if (this.claimAble.isEmpty())
            return "null";

        return this.income.keySet().stream().map(econ -> (econ.getCurrency() == null ? econ.getType().name() : econ.getType().name() + ":" + econ.getCurrency())
                + ";;" + this.income.get(econ)).collect(Collectors.joining(",,"));
    }

    public void setNextInterval(long nextInterval) {
        this.nextInterval = nextInterval;
    }

    public int getId() {
        return this.id;
    }

    public ChestLocation getLocation() {
        return this.location;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getItemsSold() {
        return itemsSold;
    }

    public ChestSettings getSettings() {
        return new ChestSettings(this.logging, this.intervalUpgrade, this.multiplierUpgrade);
    }

    public String getIncomeRaw() {
        if (this.income.isEmpty())
            return "null";

        return this.income.keySet().stream().map(econ -> (econ.getCurrency() == null ? econ.getType().name() : econ.getType().name() + ":" + econ.getCurrency())
                + ";;" + this.income.get(econ)).collect(Collectors.joining(",,"));
    }

    public String getIncome(String message) {
        if (this.income.isEmpty())
            return AutoSellChests.getInstance().formatPrices(null, 0.0, message);

        return AutoSellChests.getInstance().formatPrices(this.income, message);
    }

    public long getNextInterval() {
        return this.nextInterval;
    }

    public long getInterval() {
        return this.interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean isLogging() {return this.logging; }

    public void setLogging(boolean logging) { this.logging = logging;}

    public int getIntervalUpgrade() {
        return this.intervalUpgrade;
    }

    public void setIntervalUpgrade(int upgrade) {
        this.intervalUpgrade = upgrade;
    }

    public double getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public int getMultiplierUpgrade() {
        return this.multiplierUpgrade;
    }

    public void setMultiplierUpgrade(int upgrade) {
        this.multiplierUpgrade = upgrade;
    }

    public boolean isDoubleChest() {
        return this.location.isDoubleChest();
    }

    public String getName() {
        return this.displayname;
    }

    public void setName(String displayname) {
        this.displayname = displayname;
    }

    @Override
    public String toString() {
        return "{Id: " + this.id +
                ", Location: " + this.location +
                ", Owner: " + this.owner +
                ", ItemsSold: " + this.itemsSold +
                ", Income: " + this.getIncome(null) +
                ", Logging: " + this.logging +
                ", Interval upgrade: " + this.intervalUpgrade +
                ", Multiplier upgrade: " + this.multiplierUpgrade + "}";
    }

    private Map<EcoType, Double> loadPrices(String income) {
        Map<EcoType, Double> prices = new HashMap<>();
        if (income == null || income.isEmpty() || income.equals("null")) return prices;

        // Change SEPARATOR's as a single comma can still occur in a ITEM economy with NBT/component data
        final String SEPARATOR = AutoSellChests.getInstance().newPriceFormat ? ",," : ",";
        final String PART_SEPARATOR = AutoSellChests.getInstance().newPriceFormat ? ";;" : ";";

        Arrays.stream(income.split(SEPARATOR)).forEach(s -> {
            try {
                EcoType econ = EconomyType.getFromString(s.split(PART_SEPARATOR)[0]);
                if (EconomyShopGUIHook.getEcon(econ) == null) {
                    Logger.warn("Economy type such as " + s.split(PART_SEPARATOR)[0] + " is not enabled in EconomyShopGUI, skipping...");
                    return; // EconomyType not active
                }
                if (econ == null) {
                    Logger.warn("Failed to find economy type such as '" + s + "' for input string '" + income + "' for chest " + this.id);
                } else prices.put(econ, Double.parseDouble(s.split(PART_SEPARATOR)[1]));
            } catch (NumberFormatException | NullPointerException | ArrayIndexOutOfBoundsException e) {
                Logger.warn("Failed to load price amount for '" + s + "' and input string '" + income + "' for chest " + this.id);
            }
        });
        return prices;
    }

    /*
    private double getMultiplier(String settings) {
        try {
            Double.parseDouble(settings.split("\\|")[1]);
        } catch (NumberFormatException ex) {
            Logger.warn("Failed to parse a valid sell multiplier for '" + multiplier + "' for chest " + this);
            return 0;
        } catch (IndexOutOfBoundsException ex) {
            // No multiplier specified
            return 0;
        }
        return 0;
    }

     */
}
