/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 * WingSync
 */

package win.doughmination.wingsync.commands;

// Internal
import win.doughmination.wingsync.Main;

// Bukkit
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

// dv8
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

// Java
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
        ).queue(
                success -> plugin.getLogger().info("Successfully registered WingSync slash commands!"),
                error -> plugin.getLogger().severe("Failed to register WingSync slash commands: " + error.getMessage())
        );
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
        win.doughmination.wingsync.listeners.StorageUtil storage = plugin.getStorageUtil();
        win.doughmination.wingsync.listeners.StorageUtil.StorageType type = storage.getStorageType();

        String typeName;
        String details;

        switch (type) {
            case MYSQL:
                typeName = "MySQL Database";
                details = "Connected to: " + plugin.getConfig().getString("storage.host")
                        + ":" + plugin.getConfig().getInt("storage.port");
                break;
            case POSTGRESQL:
                typeName = "PostgreSQL Database";
                details = "Connected to: " + plugin.getConfig().getString("storage.host")
                        + ":" + plugin.getConfig().getInt("storage.port");
                break;
            default:
                typeName = "File-based Storage";
                details = "Data file: " + storage.getDataFile().getName()
                        + " (" + storage.getPlayerDataMap().size() + " records)";
                break;
        }

        event.reply("**Storage Information**\n" +
                "Type: " + typeName + "\n" +
                "Details: " + details).queue();
    }

    private void handleWhoisCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String discordId = event.getOption("user").getAsUser().getId();

        try {
            List<String> usernames = plugin.getUsernamesByDiscordId(discordId);
            StringBuilder response = new StringBuilder("Minecraft accounts linked to <@" + discordId + ">: ");

            if (!usernames.isEmpty()) {
                response.append(String.join(", ", usernames));
            } else {
                response.append("None");
            }

            event.getHook().sendMessage(response.toString()).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("[ERROR] Failed to fetch data. Please try again later.").queue();
            plugin.getLogger().severe("Error in whois command: " + e.getMessage());
        }
    }

    private void handleWhomcCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String username = event.getOption("username").getAsString();

        try {
            String discordUsername = plugin.getDiscordUsernameByMinecraftUsername(username);

            if (discordUsername != null) {
                event.getHook().sendMessage("**" + discordUsername + "** is linked to Minecraft username **" + username + "**").queue();
            } else {
                event.getHook().sendMessage("[ERROR] No Discord user is linked to Minecraft username **" + username + "**").queue();
            }
        } catch (Exception e) {
            event.getHook().sendMessage("[ERROR] Failed to fetch data. Please try again later.").queue();
            plugin.getLogger().severe("Error in whomc command: " + e.getMessage());
        }
    }

    private void handleWhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String playerName = event.getOption("player").getAsString();
        String discordId = event.getUser().getId();
        String discordUsername = event.getUser().getName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + playerName);
                plugin.storePlayerData(uuid.toString(), playerName, discordId, discordUsername);

                event.getHook().sendMessage("[OK] Player **" + playerName + "** has been added to the whitelist!").queue();
            } catch (Exception e) {
                plugin.getLogger().warning("Error adding player: " + e.getMessage());
                event.getHook().sendMessage("[ERROR] Failed to add player to whitelist.").queue();
            }
        });
    }

    private void handleUnwhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String playerName = event.getOption("player").getAsString();
        String discordId = event.getUser().getId();
        String adminDiscordId = plugin.getConfig().getString("discord.admin_id");

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();

                String playerDiscordId = plugin.getDiscordIdByUuid(uuid.toString());

                if (playerDiscordId != null && (playerDiscordId.equals(discordId) || discordId.equals(adminDiscordId))) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + playerName);
                    plugin.removePlayerData(uuid.toString());
                    event.getHook().sendMessage("[OK] Player **" + playerName + "** has been removed from the whitelist.").queue();
                } else {
                    event.getHook().sendMessage("[ERROR] You do not have permission to unwhitelist this player.").queue();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing player: " + e.getMessage());
                event.getHook().sendMessage("[ERROR] Failed to remove player from whitelist.").queue();
            }
        });
    }

    private void handleListWhitelistCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        Bukkit.getScheduler().runTask(plugin, () -> {
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
        String adminDiscordId = plugin.getConfig().getString("discord.admin_id");

        if (!discordId.equals(adminDiscordId)) {
            event.getHook().sendMessage("[ERROR] You do not have permission to use this command. Only the admin can pardon players.").queue();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + playerName);

                try {
                    plugin.unbanUserFromDiscord(playerName);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to unban " + playerName + " from Discord: " + e.getMessage());
                }

                event.getHook().sendMessage("[OK] Player **" + playerName + "** has been pardoned!\n" +
                        "- Minecraft ban removed\n" +
                        "- Discord ban removed (if they were linked)").queue();

                plugin.getLogger().info(event.getUser().getName() + " pardoned player: " + playerName);

            } catch (Exception e) {
                plugin.getLogger().warning("Error pardoning player: " + e.getMessage());
                event.getHook().sendMessage("[ERROR] Failed to pardon player. Please check the logs.").queue();
            }
        });
    }
}