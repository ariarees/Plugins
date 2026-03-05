/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync;

import net.dv8tion.jda.api.entities.UserSnowflake;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import win.doughmination.wingsync.listeners.ApiListener;
import win.doughmination.wingsync.listeners.BanListener;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class Main extends JavaPlugin {

    private Connection connection;
    private JDA jda;
    private boolean useMysql;
    private File dataFile;
    private Map<String, PlayerData> playerDataMap = new HashMap<>();
    private Gson gson = new Gson();
    private boolean botConnected = false;

    // Inner class to store player data
    public static class PlayerData {
        public String uuid;
        public String username;
        public String discordId;
        public String discordUsername;
        public long linkedAt;

        public PlayerData(String uuid, String username, String discordId, String discordUsername) {
            this.uuid = uuid;
            this.username = username;
            this.discordId = discordId;
            this.discordUsername = discordUsername;
            this.linkedAt = System.currentTimeMillis();
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Check if MySQL is enabled
        useMysql = getConfig().getBoolean("mysql.enabled", false);

        if (useMysql) {
            setupDatabase();
            getLogger().info("MySQL database enabled and configured.");
        } else {
            setupFileStorage();
            getLogger().info("File-based storage enabled. MySQL disabled.");
        }

        getLogger().info("WingSync Enabling...");

        // Register DoughminationAPI listener for ban events
        if (Bukkit.getPluginManager().getPlugin("DoughminationAPI") != null) {
            getServer().getPluginManager().registerEvents(new ApiListener(this), this);
            getLogger().info("DoughminationAPI integration enabled - bans will auto-remove from whitelist.");
        } else {
            getLogger().warning("DoughminationAPI not found! Ban integration will not work.");
        }

        // Register vanilla ban listener for /ban command
        getServer().getPluginManager().registerEvents(new BanListener(this), this);
        getLogger().info("Vanilla ban listener registered - /ban will sync to Discord.");

        // Try to connect the Discord bot (gracefully handle failure)
        connectDiscordBot();
    }

    /**
     * Attempts to connect the Discord bot.
     * Returns true if successful, false otherwise.
     * Does not crash the plugin on failure.
     */
    private boolean connectDiscordBot() {
        // Disconnect existing bot if connected
        if (jda != null) {
            getLogger().info("Disconnecting existing Discord bot...");
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
            }
            jda = null;
            botConnected = false;
        }

        String token = getConfig().getString("discord.token");

        // Check if token is not set or is still the default placeholder
        if (token == null || token.isEmpty() || token.equals("YOUR_DISCORD_BOT_TOKEN")) {
            getLogger().warning("========================================");
            getLogger().warning("Discord bot token is not configured!");
            getLogger().warning("Please edit plugins/WingSync/config.yml");
            getLogger().warning("and set your Discord bot token.");
            getLogger().warning("Then use /wsreload to connect the bot.");
            getLogger().warning("========================================");
            botConnected = false;
            return false;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new DiscordSlashCommandListener())
                    .setAutoReconnect(true)
                    .build();

            jda.awaitReady();
            botConnected = true;
            getLogger().info("Discord bot connected successfully!");
            return true;
        } catch (net.dv8tion.jda.api.exceptions.InvalidTokenException e) {
            getLogger().severe("========================================");
            getLogger().severe("Invalid Discord bot token!");
            getLogger().severe("Please check your token in config.yml");
            getLogger().severe("and use /wsreload to reconnect.");
            getLogger().severe("========================================");
            botConnected = false;
            return false;
        } catch (InterruptedException e) {
            getLogger().severe("Discord bot connection was interrupted!");
            getLogger().severe("Use /wsreload to try again.");
            botConnected = false;
            return false;
        } catch (Exception e) {
            getLogger().severe("Failed to connect Discord bot: " + e.getMessage());
            getLogger().severe("Use /wsreload to try again after fixing the issue.");
            botConnected = false;
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (useMysql) {
            closeDatabaseConnection();
        } else {
            saveFileData();
        }

        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                getLogger().warning("Discord bot shutdown interrupted: " + e.getMessage());
                jda.shutdownNow();
            }
        }
        getLogger().info("WingSync Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("wsreload")) {
            reloadConfig();

            // Reload MySQL setting
            boolean newUseMysql = getConfig().getBoolean("mysql.enabled", false);

            // If switching storage methods, handle migration
            if (newUseMysql != useMysql) {
                if (newUseMysql) {
                    sender.sendMessage("\u00a7eSwitching to MySQL storage...");
                    // Save current file data before switching
                    if (!useMysql) {
                        saveFileData();
                    }
                    setupDatabase();
                    sender.sendMessage("\u00a7aMySQL storage enabled!");
                } else {
                    sender.sendMessage("\u00a7eSwitching to file-based storage...");
                    // Close database before switching
                    if (useMysql) {
                        closeDatabaseConnection();
                    }
                    setupFileStorage();
                    sender.sendMessage("\u00a7aFile-based storage enabled!");
                }
                useMysql = newUseMysql;
            } else {
                // Refresh current storage
                if (useMysql) {
                    closeDatabaseConnection();
                    setupDatabase();
                } else {
                    loadFileData();
                }
            }

            // Attempt to reconnect the Discord bot
            sender.sendMessage("\u00a7eReconnecting Discord bot...");
            boolean connected = connectDiscordBot();
            if (connected) {
                sender.sendMessage("\u00a7aWingSync reloaded successfully! Discord bot connected.");
            } else {
                sender.sendMessage("\u00a7cWingSync reloaded, but Discord bot failed to connect. Check console for details.");
            }

            return true;
        }
        return false;
    }

    // Setup file storage
    private void setupFileStorage() {
        dataFile = new File(getDataFolder(), "playerdata.json");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                saveFileData(); // Save empty map
            } catch (IOException e) {
                getLogger().severe("Failed to create playerdata.json: " + e.getMessage());
            }
        }
        loadFileData();
    }

    // Load data from file
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
            getLogger().info("Loaded " + playerDataMap.size() + " player records from file.");
        } catch (IOException e) {
            getLogger().severe("Failed to load playerdata.json: " + e.getMessage());
            playerDataMap = new HashMap<>();
        }
    }

    // Save data to file
    private void saveFileData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            String json = gson.toJson(playerDataMap);
            writer.write(json);
            getLogger().info("Saved " + playerDataMap.size() + " player records to file.");
        } catch (IOException e) {
            getLogger().severe("Failed to save playerdata.json: " + e.getMessage());
        }
    }

    // Setup MySQL database
    private void setupDatabase() {
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String username = getConfig().getString("mysql.username");
        String password = getConfig().getString("mysql.password");

        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            connection = DriverManager.getConnection(url, username, password);

            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS wingsync_players (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "username VARCHAR(16) NOT NULL, " +
                            "discord_id VARCHAR(20) NOT NULL, " +
                            "discord_username VARCHAR(32) NOT NULL, " +
                            "linked_at BIGINT NOT NULL)"
            )) {
                statement.executeUpdate();
            }

            getLogger().info("MySQL database connected and table ensured.");
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            getLogger().severe("Falling back to file-based storage.");
            useMysql = false;
            setupFileStorage();
        }
    }

    // Close database connection
    private void closeDatabaseConnection() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("MySQL database connection closed.");
            } catch (SQLException e) {
                getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }

    // Store player data (UUID + Discord info)
    private void storePlayerData(String uuid, String username, String discordId, String discordUsername) {
        PlayerData data = new PlayerData(uuid, username, discordId, discordUsername);

        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO wingsync_players (uuid, username, discord_id, discord_username, linked_at) VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE username = ?, discord_id = ?, discord_username = ?, linked_at = ?"
            )) {
                statement.setString(1, uuid);
                statement.setString(2, username);
                statement.setString(3, discordId);
                statement.setString(4, discordUsername);
                statement.setLong(5, data.linkedAt);
                statement.setString(6, username);
                statement.setString(7, discordId);
                statement.setString(8, discordUsername);
                statement.setLong(9, data.linkedAt);
                statement.executeUpdate();
            } catch (SQLException e) {
                getLogger().warning("Failed to store player data in MySQL: " + e.getMessage());
            }
        } else {
            playerDataMap.put(uuid, data);
            saveFileData();
        }
    }

    // Remove player data by UUID
    private void removePlayerData(String uuid) {
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM wingsync_players WHERE uuid = ?"
            )) {
                statement.setString(1, uuid);
                statement.executeUpdate();
            } catch (SQLException e) {
                getLogger().warning("Failed to remove player data from MySQL: " + e.getMessage());
            }
        } else {
            playerDataMap.remove(uuid);
            saveFileData();
        }
    }

    // Remove player data by username
    public void removePlayerDataByName(String username) {
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM wingsync_players WHERE username = ?"
            )) {
                statement.setString(1, username);
                statement.executeUpdate();
            } catch (SQLException e) {
                getLogger().warning("Failed to remove player data from MySQL: " + e.getMessage());
            }
        } else {
            playerDataMap.values().removeIf(data -> data.username.equalsIgnoreCase(username));
            saveFileData();
        }
    }

    // Get Discord ID by UUID
    private String getDiscordIdByUuid(String uuid) {
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT discord_id FROM wingsync_players WHERE uuid = ?"
            )) {
                statement.setString(1, uuid);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }
            } catch (SQLException e) {
                getLogger().warning("MySQL Error: " + e.getMessage());
                throw new RuntimeException("Database error", e);
            }
        } else {
            PlayerData data = playerDataMap.get(uuid);
            return data != null ? data.discordId : null;
        }

        return null;
    }

    // Get Discord ID by username
    public String getDiscordIdByUsername(String username) {
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT discord_id FROM wingsync_players WHERE username = ?"
            )) {
                statement.setString(1, username);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }
            } catch (SQLException e) {
                getLogger().warning("MySQL Error: " + e.getMessage());
                return null;
            }
        } else {
            for (PlayerData data : playerDataMap.values()) {
                if (data.username.equalsIgnoreCase(username)) {
                    return data.discordId;
                }
            }
        }

        return null;
    }

    // Get all usernames linked to a Discord ID
    private List<String> getUsernamesByDiscordId(String discordId) {
        List<String> usernames = new ArrayList<>();

        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT username FROM wingsync_players WHERE discord_id = ?"
            )) {
                statement.setString(1, discordId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        usernames.add(rs.getString("username"));
                    }
                }
            } catch (SQLException e) {
                getLogger().warning("MySQL Error: " + e.getMessage());
            }
        } else {
            for (PlayerData data : playerDataMap.values()) {
                if (data.discordId.equals(discordId)) {
                    usernames.add(data.username);
                }
            }
        }

        return usernames;
    }

    // Get Discord username by Minecraft username
    private String getDiscordUsernameByMinecraftUsername(String username) {
        if (useMysql) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT discord_username FROM wingsync_players WHERE username = ?"
            )) {
                statement.setString(1, username);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_username");
                    }
                }
            } catch (SQLException e) {
                getLogger().warning("MySQL Error: " + e.getMessage());
                throw new RuntimeException("Database error", e);
            }
        } else {
            for (PlayerData data : playerDataMap.values()) {
                if (data.username.equalsIgnoreCase(username)) {
                    return data.discordUsername;
                }
            }
        }

        return null;
    }

    /**
     * Ban a user from Discord server if sync_bans is enabled
     * @param playerName The Minecraft username of the banned player
     */
    public void banUserFromDiscord(String playerName) {
        // Check if ban syncing is enabled
        if (!getConfig().getBoolean("discord.sync_bans", true)) {
            getLogger().info("Discord ban sync is disabled - skipping ban for " + playerName);
            return;
        }

        // Check if bot is connected
        if (jda == null || !botConnected) {
            getLogger().warning("Cannot ban from Discord - bot is not connected!");
            return;
        }

        // Get the Discord ID for this player
        String discordId = getDiscordIdByUsername(playerName);
        if (discordId == null) {
            getLogger().info("No Discord account linked for " + playerName + " - cannot ban from Discord");
            return;
        }

        // Get the guild ID from config
        String guildId = getConfig().getString("discord.guild_id");
        if (guildId == null || guildId.isEmpty() || guildId.equals("YOUR_DISCORD_GUILD_ID")) {
            getLogger().warning("Discord guild_id not configured! Cannot ban from Discord.");
            getLogger().warning("Please set discord.guild_id in config.yml");
            return;
        }

        // Get the guild
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            getLogger().warning("Could not find Discord guild with ID: " + guildId);
            return;
        }

        // Ban the user
        guild.ban(UserSnowflake.fromId(discordId), 0, TimeUnit.DAYS)
                .reason("Banned in Minecraft server")
                .queue(
                        success -> {
                            getLogger().info("Successfully banned " + playerName + " from Discord server");
                        },
                        error -> {
                            getLogger().warning("Failed to ban " + playerName + " from Discord: " + error.getMessage());
                        }
                );
    }

    /**
     * Unbans a user from the Discord server
     * @param playerName The Minecraft username
     */
    public void unbanUserFromDiscord(String playerName) {
        if (jda == null || !botConnected) {
            getLogger().warning("Cannot unban " + playerName + " from Discord - bot not connected");
            return;
        }

        // Get player's Discord ID from database
        String discordId = getDiscordIdByUsername(playerName);
        if (discordId == null) {
            getLogger().warning("Cannot unban " + playerName + " - no Discord ID found");
            return;
        }

        String guildId = getConfig().getString("discord.guild_id");
        if (guildId == null || guildId.isEmpty()) {
            getLogger().warning("Cannot unban from Discord - guild_id not configured");
            getLogger().warning("Please set discord.guild_id in config.yml");
            return;
        }

        // Get the guild
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            getLogger().warning("Could not find Discord guild with ID: " + guildId);
            return;
        }

        // Unban the user
        guild.unban(UserSnowflake.fromId(discordId))
                .queue(
                        success -> {
                            getLogger().info("Successfully unbanned " + playerName + " from Discord server");
                        },
                        error -> {
                            getLogger().warning("Failed to unban " + playerName + " from Discord: " + error.getMessage());
                        }
                );
    }

    public class DiscordSlashCommandListener extends ListenerAdapter {

        @Override
        public void onReady(ReadyEvent event) {
            // Register slash commands when bot is ready
            event.getJDA().updateCommands().addCommands(
                    Commands.slash("register", "WingSync: Add a player to the Minecraft server whitelist")
                            .addOption(OptionType.STRING, "player", "The Minecraft username to add to whitelist", true),

                    Commands.slash("remove", "WingSync: Remove a player from the Minecraft server whitelist")
                            .addOption(OptionType.STRING, "player", "The Minecraft username to remove from whitelist", true),

                    Commands.slash("listwhitelist", "WingSync: Display all players currently on the whitelist"),

                    Commands.slash("whois", "WingSync: Find which Minecraft accounts are linked to a Discord user")
                            .addOption(OptionType.USER, "user", "The Discord user to check", true),

                    Commands.slash("whomc", "WingSync: Find which Discord user is linked to a Minecraft username")
                            .addOption(OptionType.STRING, "username", "The Minecraft username to check", true),

                    Commands.slash("storage", "WingSync: Check the current storage method being used"),

                    Commands.slash("pardon", "WingSync: Pardon a banned player and restore Discord access")
                            .addOption(OptionType.STRING, "player", "The Minecraft username to pardon", true)
            ).queue(success -> {
                getLogger().info("Successfully registered WingSync slash commands!");
            }, error -> {
                getLogger().severe("Failed to register WingSync slash commands: " + error.getMessage());
            });
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            switch (event.getName()) {
                case "whois":
                    handleWhoisCommand(event);
                    break;
                case "whomc":
                    handleWhomcCommand(event);
                    break;
                case "register":
                    handleWhitelistCommand(event);
                    break;
                case "remove":
                    handleUnwhitelistCommand(event);
                    break;
                case "listwhitelist":
                    handleListWhitelistCommand(event);
                    break;
                case "storage":
                    handleStorageCommand(event);
                    break;
                case "pardon":
                    handlePardonCommand(event);
                    break;
            }
        }

        private void handleStorageCommand(SlashCommandInteractionEvent event) {
            String storageType = useMysql ? "MySQL Database" : "File-based Storage";
            String details = useMysql ?
                    "Connected to: " + getConfig().getString("mysql.host") + ":" + getConfig().getInt("mysql.port") :
                    "Data file: " + dataFile.getName() + " (" + playerDataMap.size() + " records)";

            event.reply("**Storage Information**\n" +
                    "Type: " + storageType + "\n" +
                    "Details: " + details).queue();
        }

        private void handleWhoisCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            String discordId = event.getOption("user").getAsUser().getId();

            try {
                List<String> usernames = getUsernamesByDiscordId(discordId);
                StringBuilder response = new StringBuilder("Minecraft accounts linked to <@" + discordId + ">: ");

                if (!usernames.isEmpty()) {
                    response.append(String.join(", ", usernames));
                } else {
                    response.append("None");
                }

                event.getHook().sendMessage(response.toString()).queue();
            } catch (Exception e) {
                event.getHook().sendMessage("[ERROR] Failed to fetch data. Please try again later.").queue();
                getLogger().severe("Error in whois command: " + e.getMessage());
            }
        }

        private void handleWhomcCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            String username = event.getOption("username").getAsString();

            try {
                String discordUsername = getDiscordUsernameByMinecraftUsername(username);

                if (discordUsername != null) {
                    event.getHook().sendMessage("**" + discordUsername + "** is linked to Minecraft username **" + username + "**").queue();
                } else {
                    event.getHook().sendMessage("[ERROR] No Discord user is linked to Minecraft username **" + username + "**").queue();
                }
            } catch (Exception e) {
                event.getHook().sendMessage("[ERROR] Failed to fetch data. Please try again later.").queue();
                getLogger().severe("Error in whomc command: " + e.getMessage());
            }
        }

        private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            String playerName = event.getOption("player").getAsString();
            String discordId = event.getUser().getId();
            String discordUsername = event.getUser().getName();

            Bukkit.getScheduler().runTask(Main.this, () -> {
                try {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                    UUID uuid = player.getUniqueId();

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + playerName);

                    storePlayerData(uuid.toString(), playerName, discordId, discordUsername);

                    event.getHook().sendMessage("[OK] Player **" + playerName + "** has been added to the whitelist!").queue();
                } catch (Exception e) {
                    getLogger().warning("Error adding player: " + e.getMessage());
                    event.getHook().sendMessage("[ERROR] Failed to add player to whitelist.").queue();
                }
            });
        }

        private void handleUnwhitelistCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            String playerName = event.getOption("player").getAsString();
            String discordId = event.getUser().getId();
            String adminDiscordId = getConfig().getString("discord.admin_id");

            Bukkit.getScheduler().runTask(Main.this, () -> {
                try {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                    UUID uuid = player.getUniqueId();

                    String playerDiscordId = getDiscordIdByUuid(uuid.toString());

                    if (playerDiscordId != null && (playerDiscordId.equals(discordId) || discordId.equals(adminDiscordId))) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);
                        removePlayerData(uuid.toString());
                        event.getHook().sendMessage("[OK] Player **" + playerName + "** has been removed from the whitelist.").queue();
                    } else {
                        event.getHook().sendMessage("[ERROR] You do not have permission to unwhitelist this player.").queue();
                    }
                } catch (Exception e) {
                    getLogger().warning("Error removing player: " + e.getMessage());
                    event.getHook().sendMessage("[ERROR] Failed to remove player from whitelist.").queue();
                }
            });
        }

        private void handleListWhitelistCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            Bukkit.getScheduler().runTask(Main.this, () -> {
                StringBuilder response = new StringBuilder("**Whitelisted Players:**\n```\n");
                for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
                    response.append("- ").append(player.getName()).append("\n");
                }

                if (response.toString().equals("**Whitelisted Players:**\n```\n")) {
                    response.append("No players are currently whitelisted.");
                }

                response.append("```");

                event.getHook().sendMessage(response.toString()).queue();
            });
        }

        private void handlePardonCommand(SlashCommandInteractionEvent event) {
            event.deferReply().queue();

            String playerName = event.getOption("player").getAsString();
            String discordId = event.getUser().getId();
            String adminDiscordId = getConfig().getString("discord.admin_id");

            // Check if user is admin
            if (!discordId.equals(adminDiscordId)) {
                event.getHook().sendMessage("[ERROR] You do not have permission to use this command. Only the admin can pardon players.").queue();
                return;
            }

            Bukkit.getScheduler().runTask(Main.this, () -> {
                try {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

                    // Unban from Minecraft
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + playerName);

                    // Unban from Discord
                    try {
                        unbanUserFromDiscord(playerName);
                    } catch (Exception e) {
                        getLogger().warning("Failed to unban " + playerName + " from Discord: " + e.getMessage());
                    }

                    event.getHook().sendMessage("[OK] Player **" + playerName + "** has been pardoned!\n" +
                            "- Minecraft ban removed\n" +
                            "- Discord ban removed (if they were linked)").queue();

                    getLogger().info(event.getUser().getName() + " pardoned player: " + playerName);

                } catch (Exception e) {
                    getLogger().warning("Error pardoning player: " + e.getMessage());
                    event.getHook().sendMessage("[ERROR] Failed to pardon player. Please check the logs.").queue();
                }
            });
        }
    }
}