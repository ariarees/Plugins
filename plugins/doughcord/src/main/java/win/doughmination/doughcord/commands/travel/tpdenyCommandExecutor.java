/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.listeners.travel.TeleportRequestManager;
import win.doughmination.api.LibMain;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class tpdenyCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public tpdenyCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(target, "tpdeny")) {
            target.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        UUID targetUUID = target.getUniqueId();

        if (!manager.hasRequest(targetUUID)) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests to deny.");
            return true;
        }

        TeleportRequestManager.TeleportRequest request = manager.getRequest(targetUUID);
        Player requester = plugin.getServer().getPlayer(request.getRequesterUUID());

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(ChatColor.RED + "Your teleport request to " + ChatColor.AQUA + target.getName() + ChatColor.RED + " was denied.");
        }

        target.sendMessage(ChatColor.YELLOW + "You have denied the teleport request.");
        manager.removeRequest(targetUUID);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
