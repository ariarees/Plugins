/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.moderation;

import win.doughmination.doughcord.CordMain;
import org.bukkit.ChatColor;
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

    private static final String PLUGIN_NAME = ChatColor.AQUA + "Doughminationcord";
    private static final List<String> CATEGORIES = Arrays.asList("travel", "moderation", "roleplay", "chests", "other");

    // category name -> formatted help text
    private final Map<String, String> categoryHelp = new LinkedHashMap<>();

    public DoughCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        buildHelp();
    }

    private void buildHelp() {
        categoryHelp.put("travel",
                header("travel") +
                entry("/spawn",                    "Teleport to the server spawn.") +
                entry("/setspawn",                 "Set the server spawn location.") +
                entry("/base",                     "Teleport to your saved base location.") +
                entry("/setbase",                  "Set your personal base location.") +
                entry("/visitbase <player>",       "Visit another player's base.") +
                entry("/rtp",                      "Teleport to a random location.") +
                entry("/tpa <player>",             "Request to teleport to another player.") +
                entry("/tpaccept",                 "Accept a pending teleport request.") +
                entry("/tpdeny",                   "Deny a pending teleport request.") +
                entry("/basefly <on|off>",         "Toggle flight within your base radius.") +
                entry("/flyzone <x1 y1 z1 x2 y2 z2 n>", "Create a communal fly zone.") +
                entry("/rmflyzone <n>",            "Remove a communal fly zone.")
        );

        categoryHelp.put("moderation",
                header("moderation") +
                entry("/doughban <player> [reason]", "Ban a player from the server.") +
                entry("/unban <player>",             "Unban a player.") +
                entry("/banlist",                    "View all banned players.") +
                entry("/doughreload",                "Reload the plugin configuration.") +
                entry("/version",                    "Show the current plugin version.")
        );

        categoryHelp.put("roleplay",
                header("roleplay") +
                entry("/meow",          "Send a cute cat message.") +
                entry("/bark",          "Send a cute dog message.") +
                entry("/kiss <player>", "Send a kiss to another player.")
        );

        categoryHelp.put("chests",
                header("chests") +
                entry("/echest",                          "Remotely open your ender chest.") +
                entry("/vchest",                          "Remotely open your VIP chest.") +
                entry("/chest <echest|vchest|inv> <player>", "Inspect a player's chest or inventory.")
        );

        categoryHelp.put("other",
                header("other") +
                entry("/playtime",             "Check your total playtime.") +
                entry("/veinminer <ores|trees>","Toggle vein mining for ores or trees.") +
                entry("/recipes",              "Get the URL to view all spawn egg recipes.") +
                entry("/growthpotion",         "Gives you a growth potion.") +
                entry("/shrinkpotion",         "Gives you a shrink potion.")
        );
    }

    private String header(String category) {
        return ChatColor.GOLD + "════ " + PLUGIN_NAME + ChatColor.GOLD + " — " +
                ChatColor.YELLOW + category + ChatColor.GOLD + " ════\n";
    }

    private String entry(String cmd, String desc) {
        return ChatColor.AQUA + cmd + ChatColor.DARK_GRAY + " — " + ChatColor.WHITE + desc + "\n";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show category list
            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.GOLD + "════ " + PLUGIN_NAME + ChatColor.GOLD + " Help ════\n");
            sb.append(ChatColor.WHITE + "Use " + ChatColor.AQUA + "/dough <category>" + ChatColor.WHITE + " to view commands.\n");
            for (String cat : CATEGORIES) {
                sb.append(ChatColor.YELLOW + "  • " + ChatColor.AQUA + cat + "\n");
            }
            sender.sendMessage(sb.toString().stripTrailing());
            return true;
        }

        String cat = args[0].toLowerCase();
        String help = categoryHelp.get(cat);
        if (help == null) {
            sender.sendMessage(ChatColor.RED + "Unknown category '" + args[0] + "'. " +
                    ChatColor.WHITE + "Available: " + ChatColor.AQUA + String.join(", ", CATEGORIES));
            return true;
        }

        sender.sendMessage(help.stripTrailing());
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
