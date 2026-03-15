/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class setspawnCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public setspawnCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("dough.setspawn")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        Location location = player.getLocation();
        World world = location.getWorld();

        if (world == null) {
            player.sendMessage(Component.text("Failed to set spawn. Invalid world.", NamedTextColor.RED));
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

        player.sendMessage(
            Component.text("Server spawn has been set to your current location: ", NamedTextColor.GREEN)
                .append(Component.text(formatLocation(location), NamedTextColor.AQUA))
        );
        Bukkit.getLogger().info("Server spawn has been updated by " + player.getName() + " at " + formatLocation(location));
        return true;
    }

    private String formatLocation(Location location) {
        return String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f, Yaw: %.2f, Pitch: %.2f",
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return java.util.Collections.emptyList();
    }
}
