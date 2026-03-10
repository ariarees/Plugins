/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import win.doughmination.doughcord.CordMain;

import java.util.UUID;

/**
 * Handles /base fly <on|off> — toggles base flight for the player.
 * Migrated from the old standalone /basefly command.
 */
public class FlyCommand {

    private final CordMain plugin;

    public FlyCommand(CordMain plugin) {
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (!plugin.hasBase(uuid)) {
            player.sendMessage(Component.text("You have not set a base!", NamedTextColor.RED));
            return true;
        }

        Location baseLocation = plugin.getBaseLocation(uuid);
        if (baseLocation == null || !baseLocation.getWorld().equals(player.getWorld())) {
            player.sendMessage(Component.text("You are not in the same world as your base!", NamedTextColor.RED));
            return true;
        }

        if (baseLocation.distance(player.getLocation()) > 100) {
            player.sendMessage(Component.text("You must be within your base radius to toggle flight!", NamedTextColor.RED));
            return true;
        }

        // args[0] is "fly", so the on/off toggle is args[1]
        if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            player.sendMessage(Component.text("Usage: /base fly <on|off>", NamedTextColor.RED));
            return true;
        }

        boolean enableFlight = args[1].equalsIgnoreCase("on");
        plugin.getBaseFlightMain().getFlightToggles().put(uuid, enableFlight);
        plugin.getPlayerDataManager().saveFlightToggle(uuid, enableFlight);

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
