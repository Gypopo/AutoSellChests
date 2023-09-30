package me.gypopo.autosellchests.util;

import me.gypopo.autosellchests.AutoSellChests;

public class Version {

    private final String ver;
    private final boolean dev;

    private int major = 0;
    private int minor = 0;
    private int patch = 0;
    private int beta = 0;

    public Version(String version) {
        this.ver = version;
        String[] parts;

        if (version.matches(".*[a-zA-Z].*")) {
            parts = version.replace("b", "")
                    .replace("a", "")
                    .replace("r", "")
                    .replace("-", "")
                    .split("\\.");
            this.dev = true;
        } else {
            parts = version.split("\\.");
            this.dev = false;
        }

        //System.out.println(Arrays.stream(parts).collect(Collectors.toList()));

        try {
            this.major = parts.length >= 1 ? Integer.parseInt(parts[0]) : 0;
            this.minor = parts.length >= 2 ? Integer.parseInt(parts[1]) : 0;
            this.patch = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
            this.beta = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
        } catch (NumberFormatException e) {
            AutoSellChests.getInstance().getServer().getLogger().warning("[AutoSellChests] Invalid version numbering for '" + version + "'");
        }
    }

    public boolean isGreater(Version ver) {
        return this.major > ver.major ||
                (this.major == ver.major && this.minor > ver.minor) ||
                (this.major == ver.major && this.minor == ver.minor && this.patch > ver.patch) ||
                (this.major == ver.major && this.minor == ver.minor && this.patch == ver.patch && this.beta > ver.beta);
    }

    public boolean isSmaller(Version ver) {
        return this.major < ver.major ||
                (this.major == ver.major && this.minor < ver.minor) ||
                (this.major == ver.major && this.minor == ver.minor && this.patch < ver.patch) ||
                (this.major == ver.major && this.minor == ver.minor && this.patch == ver.patch && this.beta < ver.beta);
    }

    public boolean isSame(Version ver) {
        return this.major == ver.major && this.minor == ver.minor && this.patch == ver.patch && this.beta == ver.beta;
    }

    public boolean isDev() {
        return this.dev;
    }

    public String getVer() {
        return this.ver;
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.patch;
    }
}