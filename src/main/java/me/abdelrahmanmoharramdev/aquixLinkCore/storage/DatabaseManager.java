package me.abdelrahmanmoharramdev.aquixLinkCore.storage;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private final AquixLinkCore plugin;
    private Connection connection;

    public DatabaseManager(AquixLinkCore plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "links.db");

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                // Links table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS links (
                        uuid TEXT PRIMARY KEY,
                        discord_id TEXT NOT NULL
                    );
                """);

                // Pending verifications table with created_at timestamp
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pending_verifications (
                        uuid TEXT PRIMARY KEY,
                        discord_id TEXT NOT NULL,
                        code TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection is null or closed. Attempting to reconnect...");
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check/reconnect database: " + e.getMessage());
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database: " + e.getMessage());
        }
    }
}
