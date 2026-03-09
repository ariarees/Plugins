/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import win.doughmination.doughcord.CordMain;

import org.bukkit.command.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class setspawnCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public setspawnCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("dough.setspawn")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Location location = player.getLocation();
        World world = location.getWorld();

        if (world == null) {
            player.sendMessage(ChatColor.RED + "Failed to set spawn. Invalid world.");
            return true;
        }

        plugin.getConfig().set("spawn.world", world.getName());
        plugin.getConfig().set("spawn.x", location.getX());
        plugin.getConfig().set("spawn.y", location.getY());
        plugin.getConfig().set("spawn.z", location.getZ());
        plugin.getConfig().set("spawn.yaw", location.getYaw());
        plugin.getConfig().set("spawn.pitch", location.getPitch());
        plugin.saveConfig();

        world.setSpawnLocation(location);

        player.sendMessage(ChatColor.GREEN + "Server spawn has been set to your current location: " +
                ChatColor.AQUA + formatLocation(location));
        Bukkit.getLogger().info("Server spawn has been updated by " + player.getName() + " at " + formatLocation(location));
        return true;
    }

    private String formatLocation(Location location) {
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f, Yaw: %.2f, Pitch: %.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender s, org.bukkit.command.Command c, String a, String[] args) {
        return java.util.Collections.emptyList();
    }
}
