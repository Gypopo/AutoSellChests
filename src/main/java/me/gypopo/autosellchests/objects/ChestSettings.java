package me.gypopo.autosellchests.objects;

import me.gypopo.autosellchests.util.Logger;

public class ChestSettings {

    public final boolean logging;
    public final int interval;
    public final int multiplier;

    public ChestSettings(String settings) {
        String[] parts = settings.split("\\|");

        this.logging = parts[0].equals("1");
        this.interval = this.getInt(settings, 1);
        this.multiplier = this.getInt(settings, 2);
    }

    public ChestSettings(boolean logging, int interval, int multiplier) {
        this.logging = logging;
        this.interval = interval;
        this.multiplier = multiplier;
    }

    public ChestSettings() {
        this.logging = true;
        this.interval = 0;
        this.multiplier = 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.logging ? "1" : "0").append("|");
        builder.append(this.interval).append("|");
        builder.append(this.multiplier).append("|");

        return builder.toString();
    }

    private int getInt(String settings, int path) {
        try {
            return Integer.parseInt(settings.split("\\|")[path]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Logger.warn("Failed to load upgrade level for '" + settings + "' for chest, using default...");
            return 0;
        }
    }
}
