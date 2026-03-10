/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.other;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import win.doughmination.doughcord.CordMain;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class TpsCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    public TpsCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        double[] tps = Bukkit.getServer().getTPS();

        sender.sendMessage(
            Component.text("Server TPS  ", NamedTextColor.WHITE)
                .append(Component.text("1m: ", NamedTextColor.GRAY))
                .append(coloredTps(tps[0]))
                .append(Component.text("  5m: ", NamedTextColor.GRAY))
                .append(coloredTps(tps[1]))
                .append(Component.text("  15m: ", NamedTextColor.GRAY))
                .append(coloredTps(tps[2]))
        );
        return true;
    }

    /** Colours the TPS value green (≥18), yellow (≥15), or red (<15). */
    private Component coloredTps(double tps) {
        double capped = Math.min(tps, 20.0);
        NamedTextColor color = capped >= 18.0 ? NamedTextColor.GREEN
                             : capped >= 15.0 ? NamedTextColor.YELLOW
                             : NamedTextColor.RED;
        return Component.text(String.format("%.1f", capped), color);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
