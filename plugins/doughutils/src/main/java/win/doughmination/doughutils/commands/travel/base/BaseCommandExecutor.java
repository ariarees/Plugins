/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main executor for the /base command.
 *
 * Subcommands:
 *   /base           → TpCommand  (alias for /base tp)
 *   /base tp        → TpCommand
 *   /base set       → SetCommand
 *   /base trust     → TrustCommand
 *   /base untrust   → UntrustCommand
 *   /base name      → NameCommand
 *   /base info      → InfoCommand
 */
public class BaseCommandExecutor implements CommandExecutor, TabCompleter {

    private final TpCommand      tpCommand;
    private final SetCommand     setCommand;
    private final TrustCommand   trustCommand;
    private final UntrustCommand untrustCommand;
    private final NameCommand    nameCommand;
    private final InfoCommand    infoCommand;
    private final VisitCommand   visitCommand;
    private final FlyCommand     flyCommand;

    // "visit" omitted from base list — only shown/suggested to ops
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "tp", "set", "trust", "untrust", "name", "info", "fly"
    );
    private static final List<String> OP_SUBCOMMANDS = Arrays.asList(
        "tp", "set", "trust", "untrust", "name", "info", "fly", "visit"
    );

    public BaseCommandExecutor(Main plugin, BaseDataManager baseData) {
        this.tpCommand      = new TpCommand(plugin);
        this.setCommand     = new SetCommand(plugin);
        this.trustCommand   = new TrustCommand(plugin, baseData);
        this.untrustCommand = new UntrustCommand(plugin, baseData);
        this.nameCommand    = new NameCommand(plugin, baseData);
        this.infoCommand    = new InfoCommand(plugin, baseData);
        this.visitCommand   = new VisitCommand(plugin, baseData);
        this.flyCommand     = new FlyCommand(plugin);
    }

    /** Call on plugin disable to clean up SetCommand's cooldown task. */
    public void shutdown() {
        setCommand.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        // /base or /base tp → teleport
        if (args.length == 0 || args[0].equalsIgnoreCase("tp")) {
            return tpCommand.execute(player);
        }

        switch (args[0].toLowerCase()) {
            case "set"     -> { return setCommand.execute(player); }
            case "trust"   -> { return trustCommand.execute(player, args); }
            case "untrust" -> { return untrustCommand.execute(player, args); }
            case "name"    -> { return nameCommand.execute(player, args); }
            case "info"    -> { return infoCommand.execute(player); }
            case "visit"   -> { return visitCommand.execute(player, args); }
            case "fly"     -> { return flyCommand.execute(player, args); }
            default        -> { sendHelp(player); return true; }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  /base Commands", NamedTextColor.GOLD));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(help("/base [tp]",              "Teleport to your base"));
        player.sendMessage(help("/base set",               "Set your base to your current location"));
        player.sendMessage(help("/base name <n>",          "Name your base (30min cooldown)"));
        player.sendMessage(help("/base trust <player>",    "Allow a player to interact at your base"));
        player.sendMessage(help("/base untrust <player>",  "Remove a player's base access"));
        player.sendMessage(help("/base info",              "View your base details and trusted players"));
        player.sendMessage(help("/base fly <on|off>",      "Toggle flight within your base radius"));
        if (player.hasPermission("dough.visitbase")) {
            player.sendMessage(help("/base visit <player>", "§7[Op] Teleport to any player's base"));
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    private Component help(String cmd, String desc) {
        return Component.text("  " + cmd, NamedTextColor.YELLOW)
            .append(Component.text(" - " + desc, NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subs = sender.hasPermission("dough.visitbase") ? OP_SUBCOMMANDS : SUBCOMMANDS;
            return subs.stream()
                .filter(s -> s.startsWith(partial))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            if (sub.equals("trust") || sub.equals("untrust")) {
                return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("fly")) {
                return Arrays.asList("on", "off").stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
            }
            if (sub.equals("visit") && sender.hasPermission("dough.visitbase")) {
                return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
