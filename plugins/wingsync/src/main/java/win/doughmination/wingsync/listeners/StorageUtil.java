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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.sql.Connection;
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
 * MySQL and PostgreSQL use HikariCP connection pooling.
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

    // Database storage — HikariCP pool shared by MySQL and PostgreSQL
    private HikariDataSource dataSource;

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
                break;
            case "mysql":
                storageType = StorageType.MYSQL;
                setupMysql();
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
     * Gracefully closes any open pool or flushes file storage.
     * Call this from Main#onDisable and before switching backends.
     */
    public void shutdown() {
        if (storageType == StorageType.FILE) {
            saveFileData();
        } else {
            closePool();
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
            if (playerDataMap == null) playerDataMap = new HashMap<>();
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
    // HikariCP pool setup
    // =========================================================================

    private void setupMysql() {
        String host     = plugin.getConfig().getString("storage.host", "localhost");
        int    port     = plugin.getConfig().getInt("storage.port", 3306);
        String database = plugin.getConfig().getString("storage.database", "minecraft");
        String username = plugin.getConfig().getString("storage.username", "root");
        String password = plugin.getConfig().getString("storage.password", "");

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            applyCommonPoolConfig(config);

            dataSource = new HikariDataSource(config);
            ensureTable();
            plugin.getLogger().info("MySQL connected via HikariCP.");
        } catch (Exception e) {
            closePool();
            plugin.getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
            plugin.getLogger().severe("Falling back to file-based storage.");
            storageType = StorageType.FILE;
            setupFileStorage();
        }
    }

    private void setupPostgresql() {
        String host     = plugin.getConfig().getString("storage.host", "localhost");
        int    port     = plugin.getConfig().getInt("storage.port", 5432);
        String database = plugin.getConfig().getString("storage.database", "minecraft");
        String username = plugin.getConfig().getString("storage.username", "postgres");
        String password = plugin.getConfig().getString("storage.password", "");

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            applyCommonPoolConfig(config);

            dataSource = new HikariDataSource(config);
            ensureTable();
            plugin.getLogger().info("PostgreSQL connected via HikariCP.");
        } catch (Exception e) {
            closePool();
            plugin.getLogger().severe("Failed to connect to PostgreSQL: " + e.getMessage());
            plugin.getLogger().severe("Falling back to file-based storage.");
            storageType = StorageType.FILE;
            setupFileStorage();
        }
    }

    /**
     * Applies settings common to both MySQL and PostgreSQL pools.
     */
    private void applyCommonPoolConfig(HikariConfig config) {
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);   // 5 s — fail fast on bad config
        config.setIdleTimeout(600_000);        // 10 min
        config.setMaxLifetime(1_800_000);      // 30 min — rotate before server kills idle conns
        config.setKeepaliveTime(60_000);       // 1 min — keep idle connections alive
    }

    // =========================================================================
    // Shared database helpers
    // =========================================================================

    private void ensureTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE_SQL)) {
            stmt.executeUpdate();
        }
        plugin.getLogger().info("Database table ensured.");
    }

    private void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database pool closed.");
        }
        dataSource = null;
    }

    private boolean isUsingDatabase() {
        return storageType == StorageType.MYSQL || storageType == StorageType.POSTGRESQL;
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
            // MySQL uses ON DUPLICATE KEY UPDATE; PostgreSQL uses ON CONFLICT … DO UPDATE
            String upsertSql = storageType == StorageType.MYSQL
                    ? "INSERT INTO wingsync_players (uuid, username, discord_id, discord_username, linked_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "username = VALUES(username), discord_id = VALUES(discord_id), " +
                    "discord_username = VALUES(discord_username), linked_at = VALUES(linked_at)"
                    : "INSERT INTO wingsync_players (uuid, username, discord_id, discord_username, linked_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON CONFLICT (uuid) DO UPDATE SET " +
                    "username = EXCLUDED.username, discord_id = EXCLUDED.discord_id, " +
                    "discord_username = EXCLUDED.discord_username, linked_at = EXCLUDED.linked_at";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                stmt.setString(1, uuid);
                stmt.setString(2, username);
                stmt.setString(3, discordId);
                stmt.setString(4, discordUsername);
                stmt.setLong(5, data.linkedAt);
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
            executeDelete("DELETE FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        } else {
            playerDataMap.values().removeIf(d -> d.username.equalsIgnoreCase(username));
            saveFileData();
        }
    }

    private void executeDelete(String sql, String param) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
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
            return querySingleString(
                    "SELECT discord_username FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        }
        for (PlayerData data : playerDataMap.values()) {
            if (data.username.equalsIgnoreCase(username)) return data.discordUsername;
        }
        return null;
    }

    private String querySingleString(String sql, String param) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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