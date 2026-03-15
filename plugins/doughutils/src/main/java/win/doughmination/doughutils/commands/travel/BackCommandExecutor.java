/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;


import java.util.Collections;
import java.util.List;

public class BackCommandExecutor implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public BackCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getBackLocationManager().has(player.getUniqueId())) {
            player.sendMessage(Component.text("No previous location to return to.", NamedTextColor.RED));
            return true;
        }

        Location back = plugin.getBackLocationManager().get(player.getUniqueId());

        // Save current position before teleporting so /back can chain
        plugin.getBackLocationManager().set(player.getUniqueId(), player.getLocation());

        player.teleport(back);
        player.sendMessage(Component.text("Returning to your previous location.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
