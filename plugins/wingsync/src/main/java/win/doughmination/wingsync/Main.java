/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync;

// Bukkit
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

// dv8
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.requests.GatewayIntent;

// Internal
import win.doughmination.wingsync.listeners.StorageUtil;
import win.doughmination.wingsync.listeners.GeneralListener;
import win.doughmination.wingsync.commands.*;

// Java
import java.util.List;
import java.util.concurrent.TimeUnit;

// BStats
import org.bstats.bukkit.Metrics;


public class Main extends JavaPlugin {

    private JDA jda;
    private boolean botConnected = false;
    private StorageUtil storageUtil;

    // -------------------------------------------------------------------------
    // Inner class: PlayerData (shared across all classes via Main.PlayerData)
    // -------------------------------------------------------------------------

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

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void onEnable() {
        int pluginId = 29922;
        Metrics metrics = new Metrics(this, pluginId);

        saveDefaultConfig();

        // Initialise storage
        storageUtil = new StorageUtil(this);
        storageUtil.setup();

        getLogger().info("WingSync Enabling...");

        // Register the general listener (handles both API bans and vanilla /ban)
        getServer().getPluginManager().registerEvents(new GeneralListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("DoughminationAPI") != null) {
            getLogger().info("DoughminationAPI found - full ban sync enabled.");
        } else {
            getLogger().warning("DoughminationAPI not found - API ban events will not fire, but vanilla /ban sync is active.");
        }

        connectDiscordBot();
    }

    @Override
    public void onDisable() {
        storageUtil.shutdown();

        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLogger().warning("Discord bot shutdown interrupted: " + e.getMessage());
                jda.shutdownNow();
            }
        }
        getLogger().info("WingSync Disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return new MinecraftCommands(this).onCommand(sender, command, label, args);
    }

    // =========================================================================
    // Discord bot connection
    // =========================================================================

    /**
     * Attempts to (re)connect the Discord bot.
     * Returns true on success. Does not crash the plugin on failure.
     */
    public boolean connectDiscordBot() {
        if (jda != null) {
            getLogger().info("Disconnecting existing Discord bot...");
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
            jda = null;
            botConnected = false;
        }

        String token = getConfig().getString("discord.token");

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
                    .addEventListeners(new DiscordCommands(this))
                    .setAutoReconnect(true)
                    .build();

            jda.awaitReady();
            botConnected = true;
            getLogger().info("Discord bot connected successfully!");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().severe("Discord bot connection was interrupted! Use /wsreload to try again.");
            botConnected = false;
            return false;
        } catch (Exception e) {
            getLogger().severe("Failed to connect Discord bot: " + e.getMessage());
            getLogger().severe("Use /wsreload to try again after fixing the issue.");
            botConnected = false;
            return false;
        }
    }

    // =========================================================================
    // Discord ban/unban utilities
    // =========================================================================

    /**
     * Bans a player from the Discord server if sync_bans is enabled.
     * @param playerName The Minecraft username of the banned player
     */
    public void banUserFromDiscord(String playerName) {
        if (!getConfig().getBoolean("discord.sync_bans", true)) {
            getLogger().info("Discord ban sync disabled - skipping ban for " + playerName);
            return;
        }

        if (jda == null || !botConnected) {
            getLogger().warning("Cannot ban from Discord - bot is not connected!");
            return;
        }

        String discordId = storageUtil.getDiscordIdByUsername(playerName);
        if (discordId == null) {
            getLogger().info("No Discord account linked for " + playerName + " - cannot ban from Discord");
            return;
        }

        String guildId = getConfig().getString("discord.guild_id");
        if (guildId == null || guildId.isEmpty() || guildId.equals("YOUR_DISCORD_GUILD_ID")) {
            getLogger().warning("discord.guild_id not configured! Cannot ban from Discord.");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            getLogger().warning("Could not find Discord guild with ID: " + guildId);
            return;
        }

        guild.ban(UserSnowflake.fromId(discordId), 0, TimeUnit.DAYS)
                .reason("Banned in Minecraft server")
                .queue(
                        success -> getLogger().info("Banned " + playerName + " from Discord server."),
                        error   -> getLogger().warning("Failed to ban " + playerName + " from Discord: " + error.getMessage())
                );
    }

    /**
     * Unbans a player from the Discord server.
     * @param playerName The Minecraft username
     */
    public void unbanUserFromDiscord(String playerName) {
        if (jda == null || !botConnected) {
            getLogger().warning("Cannot unban " + playerName + " from Discord - bot not connected.");
            return;
        }

        String discordId = storageUtil.getDiscordIdByUsername(playerName);
        if (discordId == null) {
            getLogger().warning("Cannot unban " + playerName + " - no Discord ID found.");
            return;
        }

        String guildId = getConfig().getString("discord.guild_id");
        if (guildId == null || guildId.isEmpty()) {
            getLogger().warning("discord.guild_id not configured! Cannot unban from Discord.");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            getLogger().warning("Could not find Discord guild with ID: " + guildId);
            return;
        }

        guild.unban(UserSnowflake.fromId(discordId))
                .queue(
                        success -> getLogger().info("Unbanned " + playerName + " from Discord server."),
                        error   -> getLogger().warning("Failed to unban " + playerName + " from Discord: " + error.getMessage())
                );
    }

    // =========================================================================
    // Delegated storage accessors (used by DiscordCommands, MinecraftCommands)
    // =========================================================================

    public void storePlayerData(String uuid, String username, String discordId, String discordUsername) {
        storageUtil.storePlayerData(uuid, username, discordId, discordUsername);
    }

    public void removePlayerData(String uuid) {
        storageUtil.removePlayerData(uuid);
    }

    public void removePlayerDataByName(String username) {
        storageUtil.removePlayerDataByName(username);
    }

    public String getDiscordIdByUuid(String uuid) {
        return storageUtil.getDiscordIdByUuid(uuid);
    }

    public String getDiscordIdByUsername(String username) {
        return storageUtil.getDiscordIdByUsername(username);
    }

    public List<String> getUsernamesByDiscordId(String discordId) {
        return storageUtil.getUsernamesByDiscordId(discordId);
    }

    public String getDiscordUsernameByMinecraftUsername(String username) {
        return storageUtil.getDiscordUsernameByMinecraftUsername(username);
    }

    // =========================================================================
    // Storage management (used by MinecraftCommands for /wsreload)
    // =========================================================================

    /**
     * Returns true if the active storage backend is MySQL.
     */
    public boolean isUseMysql() {
        return storageUtil.getStorageType() == StorageUtil.StorageType.MYSQL;
    }

    /**
     * No-op — storage type is driven entirely by config (storage.type).
     * Update config.yml and call setupDatabase() to switch backends.
     */
    public void setUseMysql(boolean useMysql) {
        // Intentional no-op: StorageUtil.setup() reads storage.type from config
    }

    /**
     * Shuts down the current storage backend and re-initialises from config.
     * Safe to call on /wsreload after reloadConfig().
     */
    public void setupDatabase() {
        storageUtil.shutdown();
        storageUtil.setup();
    }

    /**
     * Gracefully closes the active database connection (or flushes file storage).
     */
    public void closeDatabaseConnection() {
        storageUtil.shutdown();
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public StorageUtil getStorageUtil() {
        return storageUtil;
    }

    public boolean isBotConnected() {
        return botConnected;
    }
}