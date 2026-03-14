/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.JailData;
import win.doughmination.api.LibMain;
import win.doughmination.jaileddough.JailMain;

public class JailCommand implements CommandExecutor {

    private final JailMain plugin;

    public JailCommand(JailMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (plugin.getJailLocation() == null) {
            sender.sendMessage(Component.text("Jail location has not been set! Use /setjail first.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(Component.text("Usage: /jail <player> [time]", NamedTextColor.RED));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        long jailDurationMs = 0;
        if (args.length == 2) {
            try {
                jailDurationMs = Long.parseLong(args[1]) * 1000L;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid time! Use a whole number of seconds.", NamedTextColor.RED));
                return true;
            }
        }

        // Persist where the player was BEFORE teleporting them.
        plugin.getLocationStorage().store(target.getUniqueId(), target.getLocation());

        // Persist the release timestamp so it survives restarts.
        if (jailDurationMs > 0) {
            plugin.getTimerStorage().store(target.getUniqueId(), System.currentTimeMillis() + jailDurationMs);
        }

        target.teleport(plugin.getJailLocation());
        target.sendMessage(Component.text("You have been jailed!", NamedTextColor.RED));
        sender.sendMessage(Component.text("Player " + target.getName() + " has been jailed!", NamedTextColor.GREEN));

        LibMain doughApi = LibMain.getInstance();
        if (doughApi != null) {
            doughApi.setPlayerJailData(
                    target.getUniqueId(),
                    new JailData(true, System.currentTimeMillis() + jailDurationMs, plugin.getJailLocation(), null)
            );
        } else {
            plugin.getLogger().severe("DoughAPI instance is null while jailing a player!");
        }

        return true;
    }
}