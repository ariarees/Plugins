/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.moderation;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


public class VersionCommandExecutor implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final Main plugin;

    public VersionCommandExecutor(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        var meta = plugin.getPluginMeta();

        String version = meta.getVersion();
        String authors = String.join(", ", meta.getAuthors());
        String website = meta.getWebsite();

        sender.sendMessage(Component.text("════ ", NamedTextColor.GOLD)
                .append(Component.text("DoughminationCord Plugin Info", NamedTextColor.GREEN))
                .append(Component.text(" ════", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("Version: ", NamedTextColor.YELLOW)
                .append(Component.text(version, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Authors: ", NamedTextColor.YELLOW)
                .append(Component.text(authors, NamedTextColor.WHITE)));

        if (website != null && !website.isEmpty()) {
            sender.sendMessage(Component.text("Website: ", NamedTextColor.YELLOW)
                    .append(Component.text(website, NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}