/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.other;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import win.doughmination.doughcord.CordMain;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SeenCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm:ss z")
                    .withZone(ZoneId.systemDefault());

    public SeenCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /seen <player>", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];

        // Check if the player is currently online
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            sender.sendMessage(
                Component.text(online.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" is currently ", NamedTextColor.WHITE))
                    .append(Component.text("online", NamedTextColor.GREEN))
                    .append(Component.text(".", NamedTextColor.WHITE))
            );
            return true;
        }

        // Try to find them by name in data files (UUID lookup via Bukkit's offline player cache)
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);

        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(
                Component.text("No data found for player ", NamedTextColor.RED)
                    .append(Component.text(targetName, NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.RED))
            );
            return true;
        }

        UUID uuid = offlinePlayer.getUniqueId();
        long lastSeen = plugin.getPlayerDataManager().loadLastSeen(uuid);

        if (lastSeen <= 0) {
            // Fall back to Bukkit's own last-played timestamp if we have no record yet
            lastSeen = offlinePlayer.getLastPlayed();
        }

        if (lastSeen <= 0) {
            sender.sendMessage(
                Component.text("No last seen data available for ", NamedTextColor.RED)
                    .append(Component.text(targetName, NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.RED))
            );
            return true;
        }

        String formatted = FORMATTER.format(Instant.ofEpochMilli(lastSeen));
        sender.sendMessage(
            Component.text(offlinePlayer.getName() != null ? offlinePlayer.getName() : targetName, NamedTextColor.AQUA)
                .append(Component.text(" was last seen ", NamedTextColor.WHITE))
                .append(Component.text(formatted, NamedTextColor.YELLOW))
                .append(Component.text(".", NamedTextColor.WHITE))
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();

        String partial = args[0].toLowerCase();

        // Suggest online players first
        List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());

        // Also suggest any player we have a data file for
        File settingsDir = new File(plugin.getDataFolder(), "data/settings");
        if (settingsDir.exists()) {
            File[] files = settingsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    try {
                        UUID uuid = UUID.fromString(f.getName().replace(".json", ""));
                        @SuppressWarnings("deprecation")
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        String name = op.getName();
                        if (name != null && name.toLowerCase().startsWith(partial) && !suggestions.contains(name)) {
                            suggestions.add(name);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        Collections.sort(suggestions);
        return suggestions;
    }
}
