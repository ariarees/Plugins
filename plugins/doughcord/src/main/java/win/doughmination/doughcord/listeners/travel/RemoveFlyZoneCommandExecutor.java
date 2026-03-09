/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveFlyZoneCommandExecutor implements CommandExecutor {

    private final BaseFlightMain flightMain;

    public RemoveFlyZoneCommandExecutor(BaseFlightMain flightMain) {
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

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /rmflyzone <n>", NamedTextColor.RED));
            return true;
        }

        String zoneName = args[0];
        if (flightMain.getCommunalFlyZones().remove(zoneName) != null) {
            sender.sendMessage(Component.text("Fly zone '" + zoneName + "' has been removed.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Fly zone '" + zoneName + "' does not exist.", NamedTextColor.RED));
        }

        return true;
    }
}
