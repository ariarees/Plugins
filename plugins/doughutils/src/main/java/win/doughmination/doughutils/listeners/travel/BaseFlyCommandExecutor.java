/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.travel;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BaseFlyCommandExecutor implements CommandExecutor {

    private final BaseFlightMain flightMain;
    private final Main doughPlugin;

    public BaseFlyCommandExecutor(BaseFlightMain flightMain, Main doughPlugin) {
        this.flightMain = flightMain;
        this.doughPlugin = doughPlugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        if (!doughPlugin.hasBase(playerUUID)) {
            player.sendMessage(Component.text("You have not set a base!", NamedTextColor.RED));
            return true;
        }

        Location baseLocation = doughPlugin.getBaseLocation(playerUUID);
        if (baseLocation == null || !baseLocation.getWorld().equals(player.getWorld())) {
            player.sendMessage(Component.text("You are not in the same world as your base!", NamedTextColor.RED));
            return true;
        }

        double distance = baseLocation.distance(player.getLocation());
        boolean withinRadius = distance <= 100;

        if (!withinRadius) {
            player.sendMessage(Component.text("You must be within your base radius to toggle flight!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
            player.sendMessage(Component.text("Usage: /basefly <on|off>", NamedTextColor.RED));
            return true;
        }

        boolean enableFlight = args[0].equalsIgnoreCase("on");
        flightMain.getFlightToggles().put(playerUUID, enableFlight);
        doughPlugin.getPlayerDataManager().saveFlightToggle(playerUUID, enableFlight);

        if (enableFlight) {
            player.setAllowFlight(true);
            player.sendMessage(Component.text("Flight enabled while you are within your base.", NamedTextColor.GREEN));
        } else {
            player.setAllowFlight(false);
            player.sendMessage(Component.text("Flight disabled.", NamedTextColor.YELLOW));
        }
        return true;
    }
}
