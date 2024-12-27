package me.gypopo.autosellchests.util.exceptions;

public class UpgradeLoadException extends Exception {

    public final Exception e;

    public UpgradeLoadException(String reason, Exception e) {
        super(reason);
        this.e = e;
    }
}
