/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.wingsync.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.wingsync.listeners.StorageUtil;

import java.util.List;
import java.util.UUID;

public class DiscordCommands extends ListenerAdapter {

    private final Main plugin;

    public DiscordCommands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().updateCommands().addCommands(
                Commands.slash("register", "WingSync: Add a player to the Minecraft whitelist")
                        .addOption(OptionType.STRING, "player", "Minecraft username to whitelist", true),
                Commands.slash("remove", "WingSync: Remove a player from the Minecraft whitelist")
                        .addOption(OptionType.STRING, "player", "Minecraft username to remove", true),
                Commands.slash("listwhitelist", "WingSync: Display all whitelisted players"),
                Commands.slash("whois", "WingSync: Find Minecraft accounts linked to a Discord user")
                        .addOption(OptionType.USER, "user", "Discord user to check", true),
                Commands.slash("whomc", "WingSync: Find the Discord user linked to a Minecraft username")
                        .addOption(OptionType.STRING, "username", "Minecraft username to check", true),
                Commands.slash("storage", "WingSync: Show the current storage backend"),
                Commands.slash("pardon", "WingSync: Pardon a banned player and restore Discord access")
                        .addOption(OptionType.STRING, "player", "Minecraft username to pardon", true)
        ).queue(
                s -> plugin.getLogger().info("WingSync: Slash commands registered."),
                e -> plugin.getLogger().severe("WingSync: Failed to register slash commands: " + e.getMessage())
        );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "whois"         -> handleWhoisCommand(event);
            case "whomc"         -> handleWhomcCommand(event);
            case "register"      -> handleWhitelistCommand(event);
            case "remove"        -> handleUnwhitelistCommand(event);
            case "listwhitelist" -> handleListWhitelistCommand(event);
            case "storage"       -> handleStorageCommand(event);
            case "pardon"        -> handlePardonCommand(event);
        }
    }

    private void handleStorageCommand(SlashCommandInteractionEvent event) {
        StorageUtil storage = plugin.getWingSyncManager().getStorageUtil();
        StorageUtil.StorageType type = storage.getStorageType();
        String typeName = switch (type) {
            case MYSQL      -> "MySQL";
            case POSTGRESQL -> "PostgreSQL";
            default         -> "JSON (file)";
        };
        String details = type == StorageUtil.StorageType.FILE
                ? "Records: " + storage.getPlayerDataMap().size()
                : plugin.getConfig().getString("wingsync.storage.host") + ":" + plugin.getConfig().getInt("wingsync.storage.port");
        event.reply("**WingSync Storage**\nType: " + typeName + "\nDetails: " + details).queue();
    }

    private void handleWhoisCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String discordId = event.getOption("user").getAsUser().getId();
        List<String> usernames = plugin.getWingSyncManager().getUsernamesByDiscordId(discordId);
        String response = "Minecraft accounts linked to <@" + discordId + ">: "
                + (usernames.isEmpty() ? "None" : String.join(", ", usernames));
        event.getHook().sendMessage(response).queue();
    }

    private void handleWhomcCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String username = event.getOption("username").getAsString();
        String discordUsername = plugin.getWingSyncManager().getDiscordUsernameByMinecraftUsername(username);
        if (discordUsername != null)
            event.getHook().sendMessage("**" + discordUsername + "** is linked to **" + username + "**").queue();
        else
            event.getHook().sendMessage("[ERROR] No Discord user linked to **" + username + "**").queue();
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String playerName = event.getOption("player").getAsString();
        String discordId = event.getUser().getId();
        String discordUsername = event.getUser().getName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                @SuppressWarnings("deprecation")
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + playerName);
                plugin.getWingSyncManager().storePlayerData(uuid.toString(), playerName, discordId, discordUsername);
                event.getHook().sendMessage("[OK] **" + playerName + "** added to the whitelist!").queue();
            } catch (Exception e) {
                event.getHook().sendMessage("[ERROR] Failed to add player to whitelist.").queue();
            }
        });
    }

    private void handleUnwhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String playerName = event.getOption("player").getAsString();
        String discordId = event.getUser().getId();
        String adminId = plugin.getConfig().getString("wingsync.discord.admin_id", "");

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                @SuppressWarnings("deprecation")
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                String playerDiscordId = plugin.getWingSyncManager().getDiscordIdByUuid(player.getUniqueId().toString());
                if (playerDiscordId != null && (playerDiscordId.equals(discordId) || discordId.equals(adminId))) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);
                    plugin.getWingSyncManager().removePlayerData(player.getUniqueId().toString());
                    event.getHook().sendMessage("[OK] **" + playerName + "** removed from the whitelist.").queue();
                } else {
                    event.getHook().sendMessage("[ERROR] You don't have permission to unwhitelist this player.").queue();
                }
            } catch (Exception e) {
                event.getHook().sendMessage("[ERROR] Failed to remove player.").queue();
            }
        });
    }

    private void handleListWhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Bukkit.getScheduler().runTask(plugin, () -> {
            StringBuilder sb = new StringBuilder("**Whitelisted Players:**\n```\n");
            for (OfflinePlayer p : Bukkit.getWhitelistedPlayers()) sb.append("- ").append(p.getName()).append("\n");
            if (sb.toString().equals("**Whitelisted Players:**\n```\n")) sb.append("No players whitelisted.");
            sb.append("```");
            event.getHook().sendMessage(sb.toString()).queue();
        });
    }

    private void handlePardonCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String playerName = event.getOption("player").getAsString();
        String discordId = event.getUser().getId();
        String adminId = plugin.getConfig().getString("wingsync.discord.admin_id", "");

        if (!discordId.equals(adminId)) {
            event.getHook().sendMessage("[ERROR] Only the admin can pardon players.").queue();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + playerName);
                plugin.getWingSyncManager().unbanUserFromDiscord(playerName);
                event.getHook().sendMessage("[OK] **" + playerName + "** has been pardoned!").queue();
            } catch (Exception e) {
                event.getHook().sendMessage("[ERROR] Failed to pardon player.").queue();
            }
        });
    }
}
