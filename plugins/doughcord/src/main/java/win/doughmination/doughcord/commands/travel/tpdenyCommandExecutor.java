/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import win.doughmination.doughcord.CordMain;
import win.doughmination.doughcord.listeners.travel.TeleportRequestManager;
import win.doughmination.api.LibMain;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class tpdenyCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public tpdenyCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(target, "tpdeny")) {
            target.sendMessage(Component.text("You cannot use this command while jailed!", NamedTextColor.RED));
            return true;
        }

        TeleportRequestManager manager = plugin.getTeleportRequestManager();
        UUID targetUUID = target.getUniqueId();

        if (!manager.hasRequest(targetUUID)) {
            target.sendMessage(Component.text("You have no pending teleport requests to deny.", NamedTextColor.RED));
            return true;
        }

        TeleportRequestManager.TeleportRequest request = manager.getRequest(targetUUID);
        Player requester = plugin.getServer().getPlayer(request.getRequesterUUID());

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(
                Component.text("Your teleport request to ", NamedTextColor.RED)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" was denied.", NamedTextColor.RED))
            );
        }

        target.sendMessage(Component.text("You have denied the teleport request.", NamedTextColor.YELLOW));
        manager.removeRequest(targetUUID);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
