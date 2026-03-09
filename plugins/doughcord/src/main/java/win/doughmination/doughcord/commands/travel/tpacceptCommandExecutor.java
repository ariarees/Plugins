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

public class tpacceptCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public tpacceptCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(target, "tpaccept")) {
            target.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        UUID targetUUID = target.getUniqueId();

        if (!manager.hasRequest(targetUUID)) {
            target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }

        TeleportRequestManager.TeleportRequest request = manager.getRequest(targetUUID);
        Player requester = plugin.getServer().getPlayer(request.getRequesterUUID());

        if (requester == null || !requester.isOnline()) {
            target.sendMessage(ChatColor.RED + "The requester is no longer online.");
            manager.removeRequest(targetUUID);
            return true;
        }

        requester.teleport(target.getLocation());
        manager.removeRequest(targetUUID);

        requester.sendMessage(ChatColor.GREEN + "Teleport request accepted by " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + "!");
        target.sendMessage(ChatColor.GREEN + "You have accepted the teleport request from " + ChatColor.AQUA + requester.getName() + ChatColor.GREEN + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
