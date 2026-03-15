/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.wingsync.listeners;

import win.doughmination.doughutils.Main;
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

public class StorageUtil {

    public enum StorageType { FILE, MYSQL, POSTGRESQL }

    private final Main plugin;
    private StorageType storageType;

    private File dataFile;
    private Map<String, PlayerData> playerDataMap = new HashMap<>();
    private final Gson gson = new Gson();

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
    // Lifecycle
    // =========================================================================

    public void setup() {
        String type = plugin.getConfig().getString("wingsync.storage.type", "json").toLowerCase();
        switch (type) {
            case "postgresql", "postgres" -> { storageType = StorageType.POSTGRESQL; setupPostgresql(); }
            case "mysql"                  -> { storageType = StorageType.MYSQL; setupMysql(); }
            default -> {
                if (!type.equals("json"))
                    plugin.getLogger().warning("Unknown WingSync storage type '" + type + "' - falling back to JSON.");
                storageType = StorageType.FILE;
                setupFileStorage();
            }
        }
    }

    public void shutdown() {
        if (storageType == StorageType.FILE) saveFileData();
        else closePool();
    }

    // =========================================================================
    // File storage
    // =========================================================================

    private void setupFileStorage() {
        dataFile = new File(plugin.getDataFolder(), "wingsync_playerdata.json");
        if (!dataFile.exists()) {
            try { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); saveFileData(); }
            catch (IOException e) { plugin.getLogger().severe("Failed to create wingsync_playerdata.json: " + e.getMessage()); }
        }
        loadFileData();
    }

    private void loadFileData() {
        try {
            if (dataFile.length() == 0) { playerDataMap = new HashMap<>(); return; }
            String json = new String(Files.readAllBytes(dataFile.toPath()));
            Type type = new TypeToken<HashMap<String, PlayerData>>(){}.getType();
            playerDataMap = gson.fromJson(json, type);
            if (playerDataMap == null) playerDataMap = new HashMap<>();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load wingsync_playerdata.json: " + e.getMessage());
            playerDataMap = new HashMap<>();
        }
    }

    private void saveFileData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(gson.toJson(playerDataMap));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save wingsync_playerdata.json: " + e.getMessage());
        }
    }

    // =========================================================================
    // HikariCP
    // =========================================================================

    private void setupMysql() {
        String host = plugin.getConfig().getString("wingsync.storage.host", "localhost");
        int port    = plugin.getConfig().getInt("wingsync.storage.port", 3306);
        String db   = plugin.getConfig().getString("wingsync.storage.database", "minecraft");
        String user = plugin.getConfig().getString("wingsync.storage.username", "root");
        String pass = plugin.getConfig().getString("wingsync.storage.password", "");
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(user); config.setPassword(pass);
            applyCommonPoolConfig(config);
            dataSource = new HikariDataSource(config);
            ensureTable();
            plugin.getLogger().info("WingSync: MySQL connected.");
        } catch (Exception e) {
            closePool();
            plugin.getLogger().severe("WingSync: MySQL failed — falling back to JSON. " + e.getMessage());
            storageType = StorageType.FILE; setupFileStorage();
        }
    }

    private void setupPostgresql() {
        String host = plugin.getConfig().getString("wingsync.storage.host", "localhost");
        int port    = plugin.getConfig().getInt("wingsync.storage.port", 5432);
        String db   = plugin.getConfig().getString("wingsync.storage.database", "minecraft");
        String user = plugin.getConfig().getString("wingsync.storage.username", "postgres");
        String pass = plugin.getConfig().getString("wingsync.storage.password", "");
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
            config.setUsername(user); config.setPassword(pass);
            applyCommonPoolConfig(config);
            dataSource = new HikariDataSource(config);
            ensureTable();
            plugin.getLogger().info("WingSync: PostgreSQL connected.");
        } catch (Exception e) {
            closePool();
            plugin.getLogger().severe("WingSync: PostgreSQL failed — falling back to JSON. " + e.getMessage());
            storageType = StorageType.FILE; setupFileStorage();
        }
    }

    private void applyCommonPoolConfig(HikariConfig config) {
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setKeepaliveTime(60_000);
    }

    private void ensureTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CREATE_TABLE_SQL)) {
            stmt.executeUpdate();
        }
    }

    private void closePool() {
        if (dataSource != null && !dataSource.isClosed()) { dataSource.close(); }
        dataSource = null;
    }

    private boolean isUsingDatabase() {
        return storageType == StorageType.MYSQL || storageType == StorageType.POSTGRESQL;
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    public void storePlayerData(String uuid, String username, String discordId, String discordUsername) {
        PlayerData data = new PlayerData(uuid, username, discordId, discordUsername);
        if (isUsingDatabase()) {
            String sql = storageType == StorageType.MYSQL
                ? "INSERT INTO wingsync_players (uuid,username,discord_id,discord_username,linked_at) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE username=VALUES(username),discord_id=VALUES(discord_id),discord_username=VALUES(discord_username),linked_at=VALUES(linked_at)"
                : "INSERT INTO wingsync_players (uuid,username,discord_id,discord_username,linked_at) VALUES(?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET username=EXCLUDED.username,discord_id=EXCLUDED.discord_id,discord_username=EXCLUDED.discord_username,linked_at=EXCLUDED.linked_at";
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid); stmt.setString(2, username);
                stmt.setString(3, discordId); stmt.setString(4, discordUsername);
                stmt.setLong(5, data.linkedAt); stmt.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().warning("WingSync: failed to store player data: " + e.getMessage()); }
        } else { playerDataMap.put(uuid, data); saveFileData(); }
    }

    public void removePlayerData(String uuid) {
        if (isUsingDatabase()) executeDelete("DELETE FROM wingsync_players WHERE uuid = ?", uuid);
        else { playerDataMap.remove(uuid); saveFileData(); }
    }

    public void removePlayerDataByName(String username) {
        if (isUsingDatabase()) executeDelete("DELETE FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        else { playerDataMap.values().removeIf(d -> d.username.equalsIgnoreCase(username)); saveFileData(); }
    }

    private void executeDelete(String sql, String param) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param); stmt.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().warning("WingSync: delete error: " + e.getMessage()); }
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    public String getDiscordIdByUuid(String uuid) {
        if (isUsingDatabase()) return querySingleString("SELECT discord_id FROM wingsync_players WHERE uuid = ?", uuid);
        PlayerData d = playerDataMap.get(uuid); return d != null ? d.discordId : null;
    }

    public String getDiscordIdByUsername(String username) {
        if (isUsingDatabase()) return querySingleString("SELECT discord_id FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        for (PlayerData d : playerDataMap.values()) if (d.username.equalsIgnoreCase(username)) return d.discordId;
        return null;
    }

    public List<String> getUsernamesByDiscordId(String discordId) {
        List<String> list = new ArrayList<>();
        if (isUsingDatabase()) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT username FROM wingsync_players WHERE discord_id = ?")) {
                stmt.setString(1, discordId);
                try (ResultSet rs = stmt.executeQuery()) { while (rs.next()) list.add(rs.getString("username")); }
            } catch (SQLException e) { plugin.getLogger().warning("WingSync: query error: " + e.getMessage()); }
        } else {
            for (PlayerData d : playerDataMap.values()) if (d.discordId.equals(discordId)) list.add(d.username);
        }
        return list;
    }

    public String getDiscordUsernameByMinecraftUsername(String username) {
        if (isUsingDatabase()) return querySingleString("SELECT discord_username FROM wingsync_players WHERE LOWER(username) = LOWER(?)", username);
        for (PlayerData d : playerDataMap.values()) if (d.username.equalsIgnoreCase(username)) return d.discordUsername;
        return null;
    }

    private String querySingleString(String sql, String param) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) return rs.getString(1); }
        } catch (SQLException e) { plugin.getLogger().warning("WingSync: query error: " + e.getMessage()); }
        return null;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public StorageType getStorageType()              { return storageType; }
    public File getDataFile()                        { return dataFile; }
    public Map<String, PlayerData> getPlayerDataMap(){ return playerDataMap; }

    // =========================================================================
    // Inner class
    // =========================================================================

    public static class PlayerData {
        public String uuid, username, discordId, discordUsername;
        public long linkedAt;

        public PlayerData(String uuid, String username, String discordId, String discordUsername) {
            this.uuid = uuid; this.username = username;
            this.discordId = discordId; this.discordUsername = discordUsername;
            this.linkedAt = System.currentTimeMillis();
        }
    }
}
