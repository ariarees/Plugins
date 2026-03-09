/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import win.doughmination.jaileddough.JailMain;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {

    private final JailMain plugin;

    public UnjailCommand(JailMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unjail <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        UUID id = target.getUniqueId();
        Location returnLoc = plugin.getLocationStorage().get(id);

        if (returnLoc == null) {
            sender.sendMessage(ChatColor.RED + target.getName() + " has no saved pre-jail location!");
            return true;
        }

        target.teleport(returnLoc);
        plugin.getLocationStorage().remove(id);
        plugin.getTimerStorage().remove(id);

        target.sendMessage(ChatColor.GREEN + "You have been released from jail!");
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been released from jail!");

        LibMain doughApi = LibMain.getInstance();
        if (doughApi != null) {
            doughApi.clearPlayerJailData(id);
        } else {
            plugin.getLogger().severe("DoughAPI instance is null while unjailing a player!");
        }

        return true;
    }
}