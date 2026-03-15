/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;



import java.util.ArrayList;
import java.util.List;

public class tpaCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public tpaCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.canUseCommand(requester)) {
            return true;
        }

        if (args.length != 1) {
            requester.sendMessage(Component.text("Usage: /tpa <player>", NamedTextColor.RED));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(Component.text("Player not found or not online!", NamedTextColor.RED));
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(Component.text("You cannot teleport to yourself!", NamedTextColor.RED));
            return true;
        }

        if (plugin.getTeleportRequestManager().hasRequest(target.getUniqueId())) {
            requester.sendMessage(Component.text("This player already has a pending teleport request.", NamedTextColor.YELLOW));
            return true;
        }

        plugin.getTeleportRequestManager().addRequest(target.getUniqueId(), requester.getUniqueId());

        target.sendMessage(
            Component.text(requester.getName(), NamedTextColor.AQUA)
                .append(Component.text(" wants to teleport to you!", NamedTextColor.YELLOW))
        );
        target.sendMessage(
            Component.text("Type ", NamedTextColor.GREEN)
                .append(Component.text("/tpaccept", NamedTextColor.AQUA))
                .append(Component.text(" to accept or ", NamedTextColor.GREEN))
                .append(Component.text("/tpdeny", NamedTextColor.AQUA))
                .append(Component.text(" to deny.", NamedTextColor.GREEN))
        );
        requester.sendMessage(
            Component.text("Teleport request sent to ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(".", NamedTextColor.GREEN))
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
        }
        return completions;
    }
}
