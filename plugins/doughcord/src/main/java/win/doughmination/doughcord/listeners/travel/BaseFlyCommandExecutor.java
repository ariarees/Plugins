/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import win.doughmination.doughcord.CordMain;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BaseFlyCommandExecutor implements CommandExecutor {

    private final BaseFlightMain flightMain;
    private final CordMain doughPlugin;

    public BaseFlyCommandExecutor(BaseFlightMain flightMain, CordMain doughPlugin) {
        this.flightMain = flightMain;
        this.doughPlugin = doughPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (!doughPlugin.hasBase(playerUUID)) {
            player.sendMessage(ChatColor.RED + "You have not set a base!");
            return true;
        }

        Location baseLocation = doughPlugin.getBaseLocation(playerUUID);
        if (baseLocation == null || !baseLocation.getWorld().equals(player.getWorld())) {
            player.sendMessage(ChatColor.RED + "You are not in the same world as your base!");
            return true;
        }

        double distance = baseLocation.distance(player.getLocation());
        boolean withinRadius = distance <= 100;

        if (!withinRadius) {
            player.sendMessage(ChatColor.RED + "You must be within your base radius to toggle flight!");
            return true;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
            player.sendMessage(ChatColor.RED + "Usage: /basefly <on|off>");
            return true;
        }

        boolean enableFlight = args[0].equalsIgnoreCase("on");
        flightMain.getFlightToggles().put(playerUUID, enableFlight);
        doughPlugin.getPlayerDataManager().saveFlightToggle(playerUUID, enableFlight);

        if (enableFlight) {
            player.setAllowFlight(true);
            player.sendMessage(ChatColor.GREEN + "Flight enabled while you are within your base.");
        } else {
            player.setAllowFlight(false);
            player.sendMessage(ChatColor.YELLOW + "Flight disabled.");
        }
        return true;
    }
}
