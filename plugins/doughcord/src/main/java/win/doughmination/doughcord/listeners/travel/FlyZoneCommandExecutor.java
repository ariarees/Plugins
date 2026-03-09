/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyZoneCommandExecutor implements CommandExecutor {

    private final BaseFlightMain flightMain;

    public FlyZoneCommandExecutor(BaseFlightMain flightMain) {
        this.flightMain = flightMain;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("dough.flyzone")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 7) {
            sender.sendMessage(Component.text("Usage: /flyzone <x1> <y1> <z1> <x2> <y2> <z2> <n>", NamedTextColor.RED));
            return true;
        }

        try {
            double x1 = Double.parseDouble(args[0]);
            double y1 = Double.parseDouble(args[1]);
            double z1 = Double.parseDouble(args[2]);
            double x2 = Double.parseDouble(args[3]);
            double y2 = Double.parseDouble(args[4]);
            double z2 = Double.parseDouble(args[5]);
            String zoneName = args[6];

            Location loc1 = new Location(player.getWorld(), x1, y1, z1);
            Location loc2 = new Location(player.getWorld(), x2, y2, z2);

            flightMain.getCommunalFlyZones().put(zoneName, new BaseFlightMain.FlyZone(zoneName, loc1, loc2));
            sender.sendMessage(Component.text("Fly zone '" + zoneName + "' has been created.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Coordinates must be valid numbers.", NamedTextColor.RED));
        }

        return true;
    }
}
