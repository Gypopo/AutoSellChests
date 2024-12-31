package me.gypopo.autosellchests.database;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.objects.ChestLocation;
import me.gypopo.autosellchests.objects.ChestSettings;
import me.gypopo.autosellchests.util.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.*;

public class SQLite {

    private final String dbPath = AutoSellChests.getInstance().getDataFolder() + File.separator + "database.db";
    private Connection conn;

    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            //addSettingsColumn();
        } catch (Exception e) {
            AutoSellChests.getInstance().getLogger().warning("Error loading database...");
            e.printStackTrace();
            return false;
        }

        if (!this.isNewFormat())
            this.update();

        return true;
    }

    private boolean isNewFormat() {
        try {
            PreparedStatement stmt = conn.prepareStatement("PRAGMA table_info(chests);");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getString("name").equals("claimAble"))
                    return true;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void update() {
        try {
            if (this.isNewFormat())
                return;

            Logger.info("Database update required, migrating to new format...");
            Logger.debug("Backing up database to 'database-old.db'...");

            try {
                Files.copy(Paths.get(AutoSellChests.getInstance().getDataFolder() + "/","database.db"), Paths.get(AutoSellChests.getInstance().getDataFolder() + "/","database-old.db"), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Logger.warn("Failed to backup database");
                e.printStackTrace();
            }
            List<Chest> oldChests = (List<Chest>) this.getAllChestsOldFormat();

            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE chests;");
            stmt.close();

            this.createTables();

            int i = 0;
            for (Chest chest : oldChests) {
                this.addChest(chest.getLocation().toString(), chest.getOwner().toString(), chest.getItemsSold(), new ChestSettings());
                i++;
            }
            Logger.info("Successfully completed migrating and imported " + i + " chest(s) to new database.");
        } catch (SQLException e ) {
            Logger.warn("Failed to migrate database to new format with reason:");
            e.printStackTrace();
        }
    }

    private void addSettingsColumn() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(chests);");
            int i = 0;
            while (rs.next()) {
                i++;
            }
            if (i == 4)
                stmt.executeUpdate("ALTER TABLE chests \n" +
                        "ADD COLUMN settings TEXT;");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            AutoSellChests.getInstance().getLogger().warning("Error while closing database connection");
        }
    }

    public void createTables() {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chests " +
                    "(chest_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "location TEXT NOT NULL," +
                    "owner TEXT NOT NULL," +
                    "items INTEGER DEFAULT 0," +
                    "income TEXT," +
                    "claimAble TEXT," +
                    "settings TEXT," +
                    "name TEXT);");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void addChest(String location, String owner, int items, ChestSettings settings) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("REPLACE INTO chests(location, owner, items, settings) VALUES('" + location + "', '" + owner + "', " + items + ", '" + settings.toString() + "');");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void setChest(Chest chest) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("REPLACE INTO chests(chest_id, location, owner, items, income, claimAble, settings, name) " +
                    "VALUES(" + chest.getId() + ", '" + chest.getLocation().toString() + "', '" + chest.getOwner().toString() + "'," + chest.getItemsSold() + ",'" + chest.getIncomeRaw() + "','" + chest.getClaimAbleRaw() + "','" + chest.getSettings() + "','" + chest + "');");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void removeChest(String location) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM chests WHERE location = '" + location + "';");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void saveChest(Chest chest) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE chests SET items = '" + chest.getItemsSold() + "', income = '" + chest.getIncomeRaw() + "', claimAble = '" + chest.getClaimAbleRaw() + "', settings = '" + chest.getSettings() + "', name = '" + chest.getName() + "' WHERE location = '" + chest.getLocation().toString() + "';");
            stmt.close();
        } catch (SQLException e ) {
            Logger.warn("Exception occurred while saving chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getLeftLocation().getWorld().getName() + "', x" + chest.getLocation().getLeftLocation().getBlockX() + ", y" + chest.getLocation().getLeftLocation().getBlockY() + ", z" + chest.getLocation().getLeftLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome(null) + " | TotalItemsSold: " + chest.getItemsSold());
            Logger.debug(e.getMessage());
        }
    }

    public Chest loadChest(ChestLocation location) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM chests WHERE location = '" + location.toString() + "';");
            Chest chest = new Chest(rs.getInt("chest_id"), rs.getString("location"), rs.getString("owner"), rs.getInt("items"), rs.getString("income"), rs.getString("claimAble"), rs.getString("settings"), rs.getString("name"));
            rs.close();
            stmt.close();
            return chest;
        } catch (SQLException e ) {
            e.printStackTrace();
            return null;
        }
    }

    public Collection<Chest> getAllChests() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM chests;");
            Collection<Chest> chests = new ArrayList<>();
            while (rs.next()) {
                chests.add(new Chest(rs.getInt("chest_id"), rs.getString("location"), rs.getString("owner"), rs.getInt("items"), rs.getString("income"), rs.getString("claimAble"), rs.getString("settings"), rs.getString("name")));
            }
            rs.close();
            stmt.close();
            return chests;
        } catch (SQLException e ) {
            e.printStackTrace();
            return null;
        }
    }

    public Collection<Chest> getAllChestsOldFormat() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM chests;");
            Collection<Chest> chests = new ArrayList<>();
            int id = 0; // Todo: Better solution for id's since the chest may get a different id if one is removed
            while (rs.next()) {
                chests.add(new Chest(id, rs.getString("location"), rs.getString("owner"), rs.getInt("items"), "null", "null", rs.getString("settings"), null));
                id++;
            }
            rs.close();
            stmt.close();
            return chests;
        } catch (SQLException e ) {
            e.printStackTrace();
            return null;
        }
    }
}
