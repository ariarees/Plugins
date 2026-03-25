/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.roleplay;
import win.doughmination.doughutils.Main;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class bapCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public bapCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            senderPlayer.sendMessage(Component.text("Usage: /bap <player>", NamedTextColor.AQUA));
            return true;
        }

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            senderPlayer.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        if (senderPlayer.equals(targetPlayer)) {
            senderPlayer.sendMessage(Component.text("You can't bap yourself!", NamedTextColor.RED));
            return true;
        }

        plugin.getServer().broadcast(
                Component.text(senderPlayer.getName(), NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(" baps ", NamedTextColor.WHITE))
                        .append(Component.text(targetPlayer.getName(), NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("!", NamedTextColor.WHITE))
        );

        senderPlayer.playSound(senderPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
