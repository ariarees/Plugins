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
import win.doughmination.jaileddough.JailMain;

public class SetJailCommand implements CommandExecutor {

    private final JailMain plugin;

    public SetJailCommand(JailMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                plugin.setJailLocation(new Location(player.getWorld(), x, y, z));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid coordinates! Use: /setjail <x> <y> <z>");
                return true;
            }
        } else if (args.length == 0) {
            plugin.setJailLocation(player.getLocation());
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /setjail or /setjail <x> <y> <z>");
            return true;
        }

        plugin.saveJailLocation();
        player.sendMessage(ChatColor.GREEN + "Jail location has been set!");
        return true;
    }
}