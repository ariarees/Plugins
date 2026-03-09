/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import win.doughmination.doughcord.CordMain;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class FlightListener implements Listener {

    private final BaseFlightMain flightMain;
    private final CordMain doughPlugin;

    public FlightListener(BaseFlightMain flightMain, CordMain doughPlugin) {
        this.flightMain = flightMain;
        this.doughPlugin = doughPlugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip head rotations — only process actual position changes
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (doughPlugin.getConfig().getBoolean("AllFlight")) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                player.sendMessage(ChatColor.GREEN + "Flight enabled globally.");
            }
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            return;
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
            return;
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