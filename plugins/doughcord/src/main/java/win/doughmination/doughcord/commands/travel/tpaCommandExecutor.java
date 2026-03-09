/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import win.doughmination.doughcord.CordMain;
import win.doughmination.api.LibMain;

import java.util.ArrayList;
import java.util.List;

public class tpaCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public tpaCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player requester)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(requester, "tpask")) {
            requester.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        if (args.length != 1) {
            requester.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage(ChatColor.RED + "You cannot teleport to yourself!");
            return true;
        }

        if (plugin.getTeleportRequestManager().hasRequest(target.getUniqueId())) {
            requester.sendMessage(ChatColor.YELLOW + "This player already has a pending teleport request.");
            return true;
        }

        plugin.getTeleportRequestManager().addRequest(target.getUniqueId(), requester.getUniqueId());

        target.sendMessage(ChatColor.AQUA + requester.getName() + ChatColor.YELLOW + " wants to teleport to you!");
        target.sendMessage(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/tpaccept" + ChatColor.GREEN + " to accept or " +
                ChatColor.AQUA + "/tpdeny" + ChatColor.GREEN + " to deny.");
        requester.sendMessage(ChatColor.GREEN + "Teleport request sent to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + ".");
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
