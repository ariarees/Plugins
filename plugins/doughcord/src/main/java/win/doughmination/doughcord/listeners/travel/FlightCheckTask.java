/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import win.doughmination.doughcord.CordMain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class FlightCheckTask extends BukkitRunnable {

    private final BaseFlightMain flightMain;
    private final CordMain doughPlugin;

    public FlightCheckTask(BaseFlightMain flightMain, CordMain doughPlugin) {
        this.flightMain = flightMain;
        this.doughPlugin = doughPlugin;
    }

    @Override
    public void run() {
        boolean allFlightEnabled = doughPlugin.getConfig().getBoolean("AllFlight");

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();

            if (allFlightEnabled) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    player.sendMessage(ChatColor.GREEN + "Flight enabled globally.");
                }
                continue;
            }

            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
                continue;
            }

            Location playerLocation = player.getLocation();
            boolean inCommunalZone = false;

            for (BaseFlightMain.FlyZone zone : flightMain.getCommunalFlyZones().values()) {
                if (zone.isWithinZone(playerLocation)) {
                    inCommunalZone = true;
                    break;
                }
            }

            if (inCommunalZone) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    player.sendMessage(ChatColor.GREEN + "Flight enabled within communal fly zone.");
                }
                continue;
            }

            Location baseLocation = doughPlugin.getBaseLocation(playerUUID);
            if (baseLocation != null && baseLocation.getWorld().equals(playerLocation.getWorld())) {
                double distance = baseLocation.distance(playerLocation);
                boolean withinRadius = distance <= 100;

                if (withinRadius && flightMain.getFlightToggles().getOrDefault(playerUUID, false)) {
                    if (!player.getAllowFlight()) {
                        player.setAllowFlight(true);
                        player.sendMessage(ChatColor.GREEN + "Flight enabled within base radius.");
                    }
                } else if (!withinRadius && player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    player.sendMessage(ChatColor.RED + "Flight disabled. You left the base radius.");
                }
            } else if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.sendMessage(ChatColor.RED + "Flight disabled. You do not have a valid base.");
            }
        }
    }
}
