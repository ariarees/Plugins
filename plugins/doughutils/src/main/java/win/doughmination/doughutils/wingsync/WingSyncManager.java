/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.wingsync;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.requests.GatewayIntent;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.wingsync.commands.DiscordCommands;
import win.doughmination.doughutils.wingsync.listeners.StorageUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages the WingSync Discord bot and storage.
 * Lifecycle (enable/disable) is driven by Main.
 */
public class WingSyncManager {

    private final Main plugin;
    private JDA jda;
    private boolean botConnected = false;
    private StorageUtil storageUtil;

    public WingSyncManager(Main plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public void onEnable() {
        storageUtil = new StorageUtil(plugin);
        storageUtil.setup();
        connectDiscordBot();
        plugin.getLogger().info("WingSync enabled.");
    }

    public void onDisable() {
        if (storageUtil != null) storageUtil.shutdown();
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) jda.shutdownNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
        }
        plugin.getLogger().info("WingSync disabled.");
    }

    // =========================================================================
    // Discord bot
    // =========================================================================

    public boolean connectDiscordBot() {
        if (jda != null) {
            jda.shutdown();
            try { if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) jda.shutdownNow(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); jda.shutdownNow(); }
            jda = null;
            botConnected = false;
        }

        String token = plugin.getConfig().getString("wingsync.discord.token", "");
        if (token.isEmpty() || token.equals("YOUR_DISCORD_BOT_TOKEN")) {
            plugin.getLogger().warning("WingSync: Discord bot token not configured. Use /wsreload after setting it.");
            botConnected = false;
            return false;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(new DiscordCommands(plugin))
                    .setAutoReconnect(true)
                    .build();
            jda.awaitReady();
            botConnected = true;
            plugin.getLogger().info("WingSync: Discord bot connected.");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().severe("WingSync: Bot connection interrupted.");
            botConnected = false;
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("WingSync: Failed to connect bot — " + e.getMessage());
            botConnected = false;
            return false;
        }
    }

    // =========================================================================
    // Ban / unban Discord sync
    // =========================================================================

    public void banUserFromDiscord(String playerName) {
        if (!plugin.getConfig().getBoolean("wingsync.discord.sync_bans", true)) return;
        if (jda == null || !botConnected) { plugin.getLogger().warning("WingSync: bot not connected, cannot ban " + playerName); return; }

        String discordId = storageUtil.getDiscordIdByUsername(playerName);
        if (discordId == null) return;

        String guildId = plugin.getConfig().getString("wingsync.discord.guild_id", "");
        if (guildId.isEmpty() || guildId.equals("YOUR_DISCORD_GUILD_ID")) { plugin.getLogger().warning("WingSync: guild_id not set."); return; }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) { plugin.getLogger().warning("WingSync: guild not found: " + guildId); return; }

        guild.ban(UserSnowflake.fromId(discordId), 0, TimeUnit.DAYS)
                .reason("Banned on Minecraft server")
                .queue(
                    s -> plugin.getLogger().info("WingSync: Banned " + playerName + " from Discord."),
                    e -> plugin.getLogger().warning("WingSync: Failed to ban " + playerName + ": " + e.getMessage())
                );
    }

    public void unbanUserFromDiscord(String playerName) {
        if (jda == null || !botConnected) return;
        String discordId = storageUtil.getDiscordIdByUsername(playerName);
        if (discordId == null) return;

        String guildId = plugin.getConfig().getString("wingsync.discord.guild_id", "");
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.unban(UserSnowflake.fromId(discordId)).queue(
            s -> plugin.getLogger().info("WingSync: Unbanned " + playerName + " from Discord."),
            e -> plugin.getLogger().warning("WingSync: Failed to unban " + playerName + ": " + e.getMessage())
        );
    }

    // =========================================================================
    // Storage delegators (used by DiscordCommands / MinecraftCommands)
    // =========================================================================

    public StorageUtil getStorageUtil()                                              { return storageUtil; }
    public boolean isBotConnected()                                                  { return botConnected; }
    public JDA getJda()                                                              { return jda; }

    public void storePlayerData(String uuid, String username, String discordId, String discordUsername) {
        storageUtil.storePlayerData(uuid, username, discordId, discordUsername);
    }
    public void removePlayerData(String uuid)               { storageUtil.removePlayerData(uuid); }
    public void removePlayerDataByName(String username)     { storageUtil.removePlayerDataByName(username); }
    public String getDiscordIdByUuid(String uuid)           { return storageUtil.getDiscordIdByUuid(uuid); }
    public String getDiscordIdByUsername(String username)   { return storageUtil.getDiscordIdByUsername(username); }
    public List<String> getUsernamesByDiscordId(String id)  { return storageUtil.getUsernamesByDiscordId(id); }
    public String getDiscordUsernameByMinecraftUsername(String username) {
        return storageUtil.getDiscordUsernameByMinecraftUsername(username);
    }
}
