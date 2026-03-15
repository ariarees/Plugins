/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.moderation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import win.doughmination.doughutils.Main;
import win.doughmination.doughutils.data.BanData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class BanlistCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final Main plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BanlistCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dough.banlist")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        Map<UUID, BanData> bans = plugin.getAllBans();

        if (bans.isEmpty()) {
            sender.sendMessage(Component.text("There are no banned players.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("════ ", NamedTextColor.GOLD)
            .append(Component.text("Banned Players", NamedTextColor.RED))
            .append(Component.text(" (" + bans.size() + ") ════", NamedTextColor.GOLD)));

        for (BanData banData : bans.values()) {
            String banDate = dateFormat.format(new Date(banData.getBannedAt()));
            sender.sendMessage(Component.text("• ", NamedTextColor.RED)
                .append(Component.text(banData.getPlayerName(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("  Reason: ", NamedTextColor.GRAY)
                .append(Component.text(banData.getReason(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("  Banned by: ", NamedTextColor.GRAY)
                .append(Component.text(banData.getBannedBy() + " on " + banDate, NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
