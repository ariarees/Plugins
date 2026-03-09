/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands;


import win.doughmination.doughcord.CordMain;

import org.bukkit.command.TabCompleter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class playtimeCommandExecutor implements CommandExecutor, TabCompleter {
    private final CordMain plugin;

    public playtimeCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        long totalPlaytime = plugin.getPlaytimeMap().getOrDefault(playerUUID, 0L);

        // Add current session time if player is online
        if (plugin.getLoginTimestamps().containsKey(playerUUID)) {
            totalPlaytime += System.currentTimeMillis() - plugin.getLoginTimestamps().get(playerUUID);
        }

        String formattedTime = formatTime(totalPlaytime);
        player.sendMessage(ChatColor.WHITE + "Your total playtime " + formattedTime);
        return true;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02d hours, %02d minutes, %02d seconds",
                hours,
                minutes % 60,
                seconds % 60
        );
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
