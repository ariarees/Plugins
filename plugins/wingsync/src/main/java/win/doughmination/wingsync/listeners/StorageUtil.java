/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync.listeners;

import win.doughmination.wingsync.Main;
import win.doughmination.wingsync.Main.PlayerData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Handles all player data storage for WingSync.
 * Supports file-based (JSON), MySQL, and PostgreSQL backends.
 * The active backend is determined by config at startup.
 */
public class StorageUtil {

    public enum StorageType {
        FILE, MYSQL, POSTGRESQL
    }

    private final Main plugin;
    private StorageType storageType;

    // File storage
    private File dataFile;
    private Map<String, PlayerData> playerDataMap = new HashMap<>();
    private final Gson gson = new Gson();

    // Database storage (shared connection for MySQL and PostgreSQL)
    private Connection connection;

    // DDL is identical for MySQL and PostgreSQL
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS wingsync_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16) NOT NULL, " +
                    "discord_id VARCHAR(20) NOT NULL, " +
                    "discord_username VARCHAR(32) NOT NULL, " +
                    "linked_at BIGINT NOT NULL)";

    public StorageUtil(Main plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * Reads config and initialises the appropriate storage backend.
     * Call this from Main#onEnable and after /wsreload.
     */
    public void setup() {
        String type = plugin.getConfig().getString("storage.type", "json").toLowerCase();

        switch (type) {
            case "postgresql":
            case "postgres":
                storageType = StorageType.POSTGRESQL;
                setupPostgresql();
                plugin.getLogger().info("PostgreSQL storage enabled and configured.");
                break;
            case "mysql":
                storageType = StorageType.MYSQL;
                setupMysql();
                plugin.getLogger().info("MySQL storage enabled and configured.");
                break;
            default:
                if (!type.equals("json")) {
                    plugin.getLogger().warning("Unknown storage type '" + type + "' - falling back to JSON.");
                }
                storageType = StorageType.FILE;
                setupFileStorage();
                plugin.getLogger().info("File-based (JSON) storage enabled.");
                break;
        }
    }

    /**
     * Gracefully closes any open database connection.
     * Call this from Main#onDisable and before switching backends.
     */
    public void shutdown() {
        if (storageType == StorageType.FILE) {
            saveFileData();
        } else {
            closeConnection();
        }
    }

    // =========================================================================
    // File storage
    // =========================================================================

    private void setupFileStorage() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.json");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                saveFileData();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create playerdata.json: " + e.getMessage());
            }
        }
        loadFileData();
    }

    private void loadFileData() {
        try {
            if (dataFile.length() == 0) {
                playerDataMap = new HashMap<>();
                return;
            }
            String json = new String(Files.readAllBytes(dataFile.toPath()));
            Type type = new TypeToken<HashMap<String, PlayerData>>(){}.getType();
            playerDataMap = gson.fromJson(json, type);
            if (playerDataMap == null) {
                playerDataMap = new HashMap<>();
            }
            plugin.getLogger().info("Loaded " + playerDataMap.size() + " player records from file.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load playerdata.json: " + e.getMessage());
            playerDataMap = new HashMap<>();
        }
    }

    private void saveFileData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(gson.toJson(playerDataMap));
            plugin.getLogger().info("Saved " + playerDataMap.size() + " player records to file.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playerdata.json: " + e.getMessage());
        }
    }

    // =========================================================================
    // MySQL storage
    // =========================================================================

    private void setupMysql() {
        String host     = plugin.getConfig().getString("storage.host", "localhost");
        int    port     = plugin.getConfig().getInt("storage.port", 3306);
        String database = plugin.getConfig().getString("storage.database", "minecraft");
        String username = plugin.getConfig().getString("storage.username", "root");
        String password = plugin.getConfig().getString("storage.password", "");

        Connection newConnection = null;
        try {
            // autoReconnect handles stale connections; connectTimeout prevents indefinite hangs
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?autoReconnect=true&useSSL=false&connectTimeout=5000";
            newConnection = DriverManager.getConnection(url, username, password);
            connection = newConnection;
            ensureTable();
            plugin.getLogger().info("MySQL connected.");
        } catch (SQLException e) {
            // Ensure the connection is closed if ensureTable() failed after opening
            if (newConnection != null) {
                try { newConnection.close(); } catch (SQLException ignored) {}
            }
            connection = null;
            plugin.getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            plugin.getLogger().severe("Falling back to file-based storage.");
            storageType = StorageType.FILE;
            setupFileStorage();
        }
    }

    // =========================================================================
    // PostgreSQL storage
    // =========================================================================

    private void setupPostgresql() {
        String host     = plugin.getConfig().getString("storage.host", "localhost");
        int    port     = plugin.getConfig().getInt("storage.port", 5432);
        String database = plugin.getConfig().getString("storage.database", "minecraft");
        String username = plugin.getConfig().getString("storage.username", "postgres");
        String password = plugin.getConfig().getString("storage.password", "");

        Connection newConnection = null;
        try {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + database
                    + "?connectTimeout=5";
            newConnection = DriverManager.getConnection(url, username, password);
            connection = newConnection;
            ensureTable();
            plugin.getLogger().info("PostgreSQL connected.");
        } catch (SQLException e) {
            // Ensure the connection is closed if ensureTable() failed after opening
            if (newConnection != null) {
                try { newConnection.close(); } catch (SQLException ignored) {}
            }
            connection = null;
            plugin.getLogger().severe("Failed to connect to PostgreSQL: " + e.getMessage());
            plugin.getLogger().severe("Falling back to file-based storage.");
            storageType = StorageType.FILE;
            setupFileStorage();
        }
    }

    // =========================================================================
    // Shared database helpers
    // =========================================================================

    private void ensureTable() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(CREATE_TABLE_SQL)) {
            stmt.executeUpdate();
        }
        plugin.getLogger().info("Database table ensured.");
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
            connection = null;
        }
    }

    private boolean isUsingDatabase() {
        return storageType == StorageType.MYSQL || storageType == StorageType.POSTGRESQL;
    }

    /**
     * Validates the connection is still alive and reconnects if not.
     * Called before every database operation to prevent stale-connection failures.
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                plugin.getLogger().warning("Database connection lost - reconnecting...");
                if (storageType == StorageType.MYSQL) {
                    setupMysql();
                } else {
                    setupPostgresql();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to validate database connection: " + e.getMessage());
        }
    }

    // =========================================================================
    // Player data: write operations
    // =========================================================================

    /**
     * Stores or updates a player's linked data.
     */
    public void storePlayerData(String uuid, String username, String discordId, String discordUsername) {
        PlayerData data = new PlayerData(uuid, username, discordId, discordUsername);

        if (isUsingDatabase()) {
            ensureConnection();
            String upsertSql = storageType == StorageType.MYSQL
                    ? "INSERT INTO wingsync_players (uuid, username, discord_id, discord_username, linked_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "username = ?, discord_id = ?, discord_username = ?, linked_at = ?"
                    : "INSERT INTO wingsync_players (uuid, username, discord_id, discord_username, linked_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON CONFLICT (uuid) DO UPDATE SET " +
                    "username = EXCLUDED.username, discord_id = EXCLUDED.discord_id, " +
                    "discord_username = EXCLUDED.discord_username, linked_at = EXCLUDED.linked_at";

            try (PreparedStatement stmt = connection.prepareStatement(upsertSql)) {
                stmt.setString(1, uuid);
                stmt.setString(2, username);
                stmt.setString(3, discordId);
                stmt.setString(4, discordUsername);
                stmt.setLong(5, data.linkedAt);

                // MySQL needs the extra UPDATE params; PostgreSQL upsert does not
                if (storageType == StorageType.MYSQL) {
                    stmt.setString(6, username);
                    stmt.setString(7, discordId);
                    stmt.setString(8, discordUsername);
                    stmt.setLong(9, data.linkedAt);
                }

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to store player data: " + e.getMessage());
            }
        } else {
            playerDataMap.put(uuid, data);
            saveFileData();
        }
    }

    /**
     * Removes a player's data by UUID.
     */
    public void removePlayerData(String uuid) {
        if (isUsingDatabase()) {
            ensureConnection();
            executeDelete("DELETE FROM wingsync_players WHERE uuid = ?", uuid);
        } else {
            playerDataMap.remove(uuid);
            saveFileData();
        }
    }

    /**
     * Removes a player's data by Minecraft username (case-insensitive).
     */
    public void removePlayerDataByName(String username) {
        if (isUsingDatabase()) {
            ensureConnection();
            executeDelete("DELETE FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        } else {
            playerDataMap.values().removeIf(data -> data.username.equalsIgnoreCase(username));
            saveFileData();
        }
    }

    private void executeDelete(String sql, String param) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, param);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete player data: " + e.getMessage());
        }
    }

    // =========================================================================
    // Player data: read operations
    // =========================================================================

    /**
     * Returns the Discord ID linked to a given UUID, or null if not found.
     */
    public String getDiscordIdByUuid(String uuid) {
        if (isUsingDatabase()) {
            ensureConnection();
            return querySingleString(
                    "SELECT discord_id FROM wingsync_players WHERE uuid = ?", uuid);
        }
        PlayerData data = playerDataMap.get(uuid);
        return data != null ? data.discordId : null;
    }

    /**
     * Returns the Discord ID linked to a given Minecraft username, or null if not found.
     */
    public String getDiscordIdByUsername(String username) {
        if (isUsingDatabase()) {
            ensureConnection();
            return querySingleString(
                    "SELECT discord_id FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        }
        for (PlayerData data : playerDataMap.values()) {
            if (data.username.equalsIgnoreCase(username)) return data.discordId;
        }
        return null;
    }

    /**
     * Returns all Minecraft usernames linked to a given Discord ID.
     */
    public List<String> getUsernamesByDiscordId(String discordId) {
        List<String> usernames = new ArrayList<>();

        if (isUsingDatabase()) {
            ensureConnection();
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT username FROM wingsync_players WHERE discord_id = ?")) {
                stmt.setString(1, discordId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) usernames.add(rs.getString("username"));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("DB error in getUsernamesByDiscordId: " + e.getMessage());
            }
        } else {
            for (PlayerData data : playerDataMap.values()) {
                if (data.discordId.equals(discordId)) usernames.add(data.username);
            }
        }

        return usernames;
    }

    /**
     * Returns the Discord username linked to a Minecraft username, or null if not found.
     */
    public String getDiscordUsernameByMinecraftUsername(String username) {
        if (isUsingDatabase()) {
            ensureConnection();
            return querySingleString(
                    "SELECT discord_username FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        }
        for (PlayerData data : playerDataMap.values()) {
            if (data.username.equalsIgnoreCase(username)) return data.discordUsername;
        }
        return null;
    }

    private String querySingleString(String sql, String param) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("DB query error: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Accessors used by Main / DiscordCommands
    // =========================================================================

    public StorageType getStorageType() {
        return storageType;
    }

    public File getDataFile() {
        return dataFile;
    }

    public Map<String, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }
}