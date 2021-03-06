package me.gypopo.autosellchests.database;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.objects.Chest;
import me.gypopo.autosellchests.util.Logger;

import java.io.File;
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
            return true;
        } catch (Exception e) {
            AutoSellChests.getInstance().getLogger().warning("Error loading database...");
            e.printStackTrace();
            return false;
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
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS chests (location TEXT NOT NULL PRIMARY KEY,owner TEXT NOT NULL,items INTEGER,income REAL);");
            stmt.close();
        } catch (SQLException e ) {
            e.printStackTrace();
        }
    }

    public void setChest(String location, String owner, int items, double income) {
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("REPLACE INTO chests(location, owner, items, income) VALUES('" + location + "', '" + owner + "', " + items + ", " + income + ");");
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
            stmt.executeUpdate("UPDATE chests SET items = '" + chest.getItemsSold() + "', income = '" + chest.getIncome() + "' WHERE location = '" + chest.getLocation().getWorld().getName() + ":" + chest.getLocation().getBlockX() + ":" + chest.getLocation().getBlockY() + ":" + chest.getLocation().getBlockZ() + "';");
            stmt.close();
        } catch (SQLException e ) {
            Logger.warn("Exception occurred while saving chest: ID: " + chest.getId() + " | Location: World '" + chest.getLocation().getWorld().getName() + "', x" + chest.getLocation().getBlockX() + ", y" + chest.getLocation().getBlockY() + ", z" + chest.getLocation().getBlockZ() + " | TotalProfit: $" + chest.getIncome() + " | TotalItemsSold: " + chest.getItemsSold());
            Logger.debug(e.getMessage());
        }
    }

    public Collection<Chest> getAllChests() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM chests;");
            Collection<Chest> chests = new ArrayList<>();
            int id = 0; // Todo: Better solution for id's since the chest may get a different id if one is removed
            while (rs.next()) {
                chests.add(new Chest(id, rs.getString("location"), rs.getString("owner"), rs.getInt("items"), rs.getDouble("income")));
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
