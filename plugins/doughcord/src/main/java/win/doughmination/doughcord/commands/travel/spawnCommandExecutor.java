/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import win.doughmination.doughcord.CordMain;
import win.doughmination.api.LibMain;

public class spawnCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public spawnCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(player, "spawn")) {
            player.sendMessage(Component.text("You cannot use this command while jailed!", NamedTextColor.RED));
            return true;
        }

        Location spawnLocation = getSpawnLocation();
        if (spawnLocation == null) {
            player.sendMessage(Component.text("The server spawn location is not set!", NamedTextColor.RED));
            return true;
        }

        plugin.getBackLocationManager().set(player.getUniqueId(), player.getLocation());
        player.teleport(spawnLocation);
        player.sendMessage(Component.text("You have been teleported to the server spawn!", NamedTextColor.GREEN));
        return true;
    }

    private Location getSpawnLocation() {
        if (!plugin.getConfig().contains("spawn.world")) {
            Bukkit.getLogger().warning("Spawn location not found in config!");
            return null;
        }
        try {
            String worldName = plugin.getConfig().getString("spawn.world");
            double x = plugin.getConfig().getDouble("spawn.x");
            double y = plugin.getConfig().getDouble("spawn.y");
            double z = plugin.getConfig().getDouble("spawn.z");
            float yaw   = (float) plugin.getConfig().getDouble("spawn.yaw");
            float pitch = (float) plugin.getConfig().getDouble("spawn.pitch");
            World world = Bukkit.getWorld(worldName);
            if (world == null) { Bukkit.getLogger().severe("Invalid spawn world: " + worldName); return null; }
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to load spawn location from config: " + e.getMessage());
            return null;
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
