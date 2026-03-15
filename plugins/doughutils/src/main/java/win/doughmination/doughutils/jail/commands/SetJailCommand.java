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

public class SetJailCommand implements CommandExecutor {

    private final Main plugin;

    public SetJailCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("jailplugin.setjail")) {
            sender.sendRichMessage("<red>You don't have permission to use this command!</red>");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("<red>This command can only be used by players!</red>");
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                plugin.setJailLocation(new Location(player.getWorld(), x, y, z));
            } catch (NumberFormatException e) {
                player.sendRichMessage("<red>Invalid coordinates! Use: /setjail <x> <y> <z></red>");
                return true;
            }
        } else if (args.length == 0) {
            plugin.setJailLocation(player.getLocation());
        } else {
            player.sendRichMessage("<red>Usage: /setjail or /setjail <x> <y> <z></red>");
            return true;
        }

        plugin.saveJailLocation();
        player.sendRichMessage("<green>Jail location has been set!</green>");
        return true;
    }
}
