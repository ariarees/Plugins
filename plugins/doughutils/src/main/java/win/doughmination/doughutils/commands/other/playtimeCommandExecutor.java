/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.other;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class playtimeCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public playtimeCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        long totalPlaytime = plugin.getPlaytimeMap().getOrDefault(playerUUID, 0L);

        if (plugin.getLoginTimestamps().containsKey(playerUUID)) {
            totalPlaytime += System.currentTimeMillis() - plugin.getLoginTimestamps().get(playerUUID);
        }

        player.sendMessage(
            Component.text("Your total playtime: ", NamedTextColor.WHITE)
                .append(Component.text(formatTime(totalPlaytime), NamedTextColor.AQUA))
        );
        return true;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        return String.format("%02d hours, %02d minutes, %02d seconds", hours, minutes % 60, seconds % 60);
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
