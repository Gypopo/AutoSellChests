package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.files.Config;
import me.gypopo.autosellchests.files.Lang;
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
    //private double multiplier = 0;
    private long nextInterval;

    public Chest(int id, String location, String owner, int itemsSold, String income, String claimAble, String settings, String displayname) {
        this.id = id;
        this.location = new ChestLocation(location);
        this.owner = UUID.fromString(owner);
        this.itemsSold = itemsSold;
        this.income = this.loadPrices(income);
        this.claimAble = this.loadPrices(claimAble);
        this.logging = settings == null || settings.split("\\|")[0].equals("1");
        this.displayname = displayname == null ? Lang.formatColors(Config.get().getString("default-chest-name").replace("%id%", String.valueOf(id)), null) : displayname.replace("%id%", String.valueOf(id));
        //this.multiplier = settings == null ? 0.0 : this.getMultiplier(settings);
    }

    public Chest(int id, ChestLocation location, Player owner, int itemsSold, Map<EcoType, Double> income, Map<EcoType, Double> claimAble, boolean logging/*, double multiplier*/, String displayname) {
        this.id = id;
        this.location = location;
        this.owner = owner.getUniqueId();
        this.itemsSold = itemsSold;
        this.income = income;
        this.claimAble = claimAble;
        this.logging = logging;
        this.displayname = displayname.replace("%id%", String.valueOf(id));
        //this.multiplier = multiplier;
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

        String s = this.claimAble.keySet().stream().map(econ -> (econ.getCurrency() == null ? econ.getType().name() : econ.getType().name() + ":" + econ.getCurrency())
                + ";" + this.claimAble.get(econ)).collect(Collectors.toList()).toString();
        return s.substring(1, s.length()-1).replace(" ", "");
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

    public String getSettings() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.logging ? "1" : "0").append("|");

        return builder.toString();
    }

    public String getIncomeRaw() {
        if (this.income.isEmpty())
            return "null";

        String s = this.income.keySet().stream().map(econ -> (econ.getCurrency() == null ? econ.getType().name() : econ.getType().name() + ":" + econ.getCurrency())
                + ";" + this.income.get(econ)).collect(Collectors.toList()).toString();
        return s.substring(1, s.length()-1).replace(" ", "");
    }

    public String getIncome(String message) {
        if (this.income.isEmpty())
            return AutoSellChests.getInstance().formatPrices(null, 0.0, message);

        return AutoSellChests.getInstance().formatPrices(this.income, message);
    }

    public long getNextInterval() {
        return this.nextInterval;
    }

    public boolean isLogging() {return this.logging; }

    public void setLogging(boolean logging) { this.logging = logging;}

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
                ", Logging: " + this.logging + "}";
    }

    private Map<EcoType, Double> loadPrices(String income) {
        Map<EcoType, Double> prices = new HashMap<>();
        if (income == null || income.isEmpty() || income.equals("null")) return prices;

        Arrays.stream(income.split(",")).forEach(s -> {
            try {
                EcoType econ = EconomyType.getFromString(s.split(";")[0]);
                if (!EconomyShopGUIHook.getEcon(econ).getType().equals(econ))
                    return; // EconomyType not active
                if (econ == null) {
                    Logger.warn("Failed to load economy type as '" + s + "' for input string '" + income + "'");
                } else prices.put(econ, Double.parseDouble(s.split(";")[1]));
            } catch (NumberFormatException | NullPointerException e) {
                Logger.warn("Failed to load price amount for '" + s + "' and input string '" + income + "'");
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
