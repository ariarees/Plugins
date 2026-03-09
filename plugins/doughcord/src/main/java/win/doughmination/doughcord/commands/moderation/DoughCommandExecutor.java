/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.moderation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import win.doughmination.doughcord.CordMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class DoughCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;

    private static final List<String> CATEGORIES = Arrays.asList("travel", "moderation", "roleplay", "chests", "other");
    private final Map<String, List<Component>> categoryHelp = new LinkedHashMap<>();

    public DoughCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        buildHelp();
    }

    private void buildHelp() {
        categoryHelp.put("travel", List.of(
            header("travel"),
            entry("/base [tp]",                    "Teleport to your saved base."),
            entry("/base set",                     "Set your base to your current location."),
            entry("/base name <n>",                "Name your base (30min cooldown)."),
            entry("/base trust <player>",          "Allow a player to interact at your base."),
            entry("/base untrust <player>",        "Revoke a player's access to your base."),
            entry("/base info",                    "View your base details and trusted players."),
            entry("/base visit <player>",          "[Op] Teleport to any player's base."),
            entry("/spawn",                        "Teleport to the server spawn."),
            entry("/setspawn",                     "Set the server spawn location."),
            entry("/rtp",                          "Teleport to a random location."),
            entry("/tpa <player>",                 "Request to teleport to another player."),
            entry("/tpaccept",                     "Accept a pending teleport request."),
            entry("/tpdeny",                       "Deny a pending teleport request."),
            entry("/basefly <on|off>",             "Toggle flight within your base radius."),
            entry("/flyzone <x1 y1 z1 x2 y2 z2 n>", "Create a communal fly zone."),
            entry("/rmflyzone <n>",                "Remove a communal fly zone.")
        ));

        categoryHelp.put("moderation", List.of(
            header("moderation"),
            entry("/doughban <player> [reason]",   "Ban a player from the server."),
            entry("/unban <player>",               "Unban a player."),
            entry("/banlist",                      "View all banned players."),
            entry("/doughreload",                  "Reload the plugin configuration."),
            entry("/version",                      "Show the current plugin version.")
        ));

        categoryHelp.put("roleplay", List.of(
            header("roleplay"),
            entry("/meow",          "Send a cute cat message."),
            entry("/bark",          "Send a cute dog message."),
            entry("/kiss <player>", "Send a kiss to another player.")
        ));

        categoryHelp.put("chests", List.of(
            header("chests"),
            entry("/echest",                             "Remotely open your ender chest."),
            entry("/vchest",                             "Remotely open your VIP chest."),
            entry("/chest <echest|vchest|inv> <player>", "Inspect a player's chest or inventory.")
        ));

        categoryHelp.put("other", List.of(
            header("other"),
            entry("/playtime",              "Check your total playtime."),
            entry("/veinminer <ores|trees>","Toggle vein mining for ores or trees."),
            entry("/recipes",               "Get the URL to view all spawn egg recipes."),
            entry("/growthpotion",          "Gives you a growth potion."),
            entry("/shrinkpotion",          "Gives you a shrink potion.")
        ));
    }

    private Component header(String category) {
        return Component.text("════ ", NamedTextColor.GOLD)
            .append(Component.text("Doughminationcord", NamedTextColor.AQUA))
            .append(Component.text(" — ", NamedTextColor.GOLD))
            .append(Component.text(category, NamedTextColor.YELLOW))
            .append(Component.text(" ════", NamedTextColor.GOLD));
    }

    private Component entry(String cmd, String desc) {
        return Component.text("  " + cmd, NamedTextColor.AQUA)
            .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
            .append(Component.text(desc, NamedTextColor.WHITE));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("════ ", NamedTextColor.GOLD)
                .append(Component.text("Doughminationcord", NamedTextColor.AQUA))
                .append(Component.text(" Help ════", NamedTextColor.GOLD)));
            sender.sendMessage(Component.text("Use ", NamedTextColor.WHITE)
                .append(Component.text("/dough <category>", NamedTextColor.AQUA))
                .append(Component.text(" to view commands.", NamedTextColor.WHITE)));
            for (String cat : CATEGORIES) {
                sender.sendMessage(Component.text("  • ", NamedTextColor.YELLOW)
                    .append(Component.text(cat, NamedTextColor.AQUA)));
            }
            return true;
        }

        String cat = args[0].toLowerCase();
        List<Component> lines = categoryHelp.get(cat);
        if (lines == null) {
            sender.sendMessage(Component.text("Unknown category '" + args[0] + "'. Available: " + String.join(", ", CATEGORIES), NamedTextColor.RED));
            return true;
        }

        lines.forEach(sender::sendMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> results = new java.util.ArrayList<>();
            for (String cat : CATEGORIES) {
                if (cat.startsWith(partial)) results.add(cat);
            }
            return results;
        }
        return Collections.emptyList();
    }
}
