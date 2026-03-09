/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.moderation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import win.doughmination.doughcord.CordMain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public BanCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.ban")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /doughban <player> [reason]", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1
            ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
            : "No reason specified";

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = targetPlayer.getUniqueId();

        LibMain cloveLib = LibMain.getInstance();
        if (cloveLib.isPlayerBanned(targetUUID)) {
            sender.sendMessage(Component.text(targetName + " is already banned!", NamedTextColor.RED));
            return true;
        }

        String bannedBy = (sender instanceof Player player) ? player.getName() : "Console";
        UUID bannedByUUID = (sender instanceof Player player) ? player.getUniqueId() : null;

        cloveLib.banPlayer(targetUUID, targetName, reason, bannedBy, bannedByUUID);

        Bukkit.broadcast(
            Component.text("⛔ ", NamedTextColor.RED)
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(" has been banned by ", NamedTextColor.RED))
                .append(Component.text(bannedBy, NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.RED))
        );
        Bukkit.broadcast(Component.text("Reason: " + reason, NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) completions.add(player.getName());
            }
        }
        return completions;
    }
}
