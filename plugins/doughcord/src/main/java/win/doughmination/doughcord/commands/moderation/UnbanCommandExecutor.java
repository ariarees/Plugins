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
import win.doughmination.api.BanData;
import win.doughmination.doughcord.CordMain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnbanCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public UnbanCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.unban")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /unban <player>", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        LibMain doughminationAPI = LibMain.getInstance();

        UUID targetUUID = null;
        for (Map.Entry<UUID, BanData> entry : doughminationAPI.getAllBans().entrySet()) {
            if (entry.getValue().getPlayerName().equalsIgnoreCase(targetName)) {
                targetUUID = entry.getKey();
                break;
            }
        }

        if (targetUUID == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (doughminationAPI.isPlayerBanned(offlinePlayer.getUniqueId())) {
                targetUUID = offlinePlayer.getUniqueId();
            }
        }

        if (targetUUID == null || !doughminationAPI.isPlayerBanned(targetUUID)) {
            sender.sendMessage(Component.text(targetName + " is not banned!", NamedTextColor.RED));
            return true;
        }

        String unbannedBy = (sender instanceof Player player) ? player.getName() : "Console";
        doughminationAPI.unbanPlayer(targetUUID, unbannedBy);

        Bukkit.broadcast(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(" has been unbanned by ", NamedTextColor.GREEN))
                .append(Component.text(unbannedBy, NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.GREEN))
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            for (BanData banData : LibMain.getInstance().getAllBans().values()) {
                if (banData.getPlayerName().toLowerCase().startsWith(partialName)) {
                    completions.add(banData.getPlayerName());
                }
            }
        }
        return completions;
    }
}
