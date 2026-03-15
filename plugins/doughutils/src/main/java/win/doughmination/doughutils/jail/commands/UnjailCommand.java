/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.jail.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.doughutils.Main;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {

    private final Main plugin;

    public UnjailCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.unjail")) {
            sender.sendRichMessage("<red>You don't have permission to use this command!</red>");
            return true;
        }

        if (args.length != 1) {
            sender.sendRichMessage("<red>Usage: /unjail <player></red>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendRichMessage("<red>Player not found!</red>");
            return true;
        }

        UUID id = target.getUniqueId();
        Location returnLoc = plugin.getLocationStorage().get(id);
        if (returnLoc == null) {
            sender.sendRichMessage("<red>" + target.getName() + " has no saved pre-jail location!</red>");
            return true;
        }

        target.teleport(returnLoc);
        plugin.getLocationStorage().remove(id);
        plugin.getTimerStorage().remove(id);
        plugin.unjailPlayer(id);

        target.sendRichMessage("<green>You have been released from jail!</green>");
        sender.sendRichMessage("<green>Player " + target.getName() + " has been released from jail!</green>");
        return true;
    }
}
