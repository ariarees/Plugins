/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import win.doughmination.doughcord.CordMain;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles /base visit <player> and the standalone /visitbase <player>.
 * Op-only. Works with both online and offline players (as long as they have a saved base).
 * Bypasses base protection — ops can always visit.
 */
public class VisitCommand implements CommandExecutor, TabCompleter {

    private final CordMain plugin;
    private final BaseDataManager baseData;

    public VisitCommand(CordMain plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    /**
     * Called from BaseCommandExecutor: args[0] is "visit", args[1] is the target name.
     */
    public boolean execute(Player visitor, String[] args) {
        if (!visitor.hasPermission("dough.visitbase")) {
            visitor.sendMessage(Component.text("You do not have permission to visit other bases!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            visitor.sendMessage(Component.text("Usage: /base visit <player>", NamedTextColor.RED));
            return true;
        }

        return doVisit(visitor, args[1]);
    }

    /**
     * Called when used as standalone /visitbase <player>.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player visitor)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(visitor, "visitbase")) {
            visitor.sendMessage(Component.text("You cannot use this command while jailed!", NamedTextColor.RED));
            return true;
        }

        if (!visitor.hasPermission("dough.visitbase")) {
            visitor.sendMessage(Component.text("You do not have permission to visit other bases!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            visitor.sendMessage(Component.text("Usage: /visitbase <player>", NamedTextColor.RED));
            return true;
        }

        return doVisit(visitor, args[0]);
    }

    private boolean doVisit(Player visitor, String targetName) {
        // Try online first
        Player onlineTarget = plugin.getServer().getPlayer(targetName);
        UUID targetUUID;
        String displayName;

        if (onlineTarget != null) {
            targetUUID = onlineTarget.getUniqueId();
            displayName = onlineTarget.getName();
        } else {
            // Fall back to offline player lookup
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = plugin.getServer().getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore()) {
                visitor.sendMessage(Component.text("Player \"" + targetName + "\" not found.", NamedTextColor.RED));
                return true;
            }
            targetUUID = offline.getUniqueId();
            displayName = offline.getName() != null ? offline.getName() : targetName;
        }

        if (!plugin.getBases().containsKey(targetUUID)) {
            visitor.sendMessage(Component.text(displayName + " has no base set.", NamedTextColor.RED));
            return true;
        }

        Location base = plugin.getBases().get(targetUUID);
        String baseName = baseData.getBaseName(targetUUID)
            .map(n -> " (\"" + n + "\")")
            .orElse("");

        if (visitor.teleport(base)) {
            String soundName = plugin.getConfig().getString("sounds.base", "ENTITY_ENDERMAN_TELEPORT");
            Sound sound = org.bukkit.Registry.SOUNDS.get(
                org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase())
            );
            if (sound != null) visitor.playSound(base, sound, 1.0f, 1.0f);

            visitor.sendMessage(
                Component.text("Teleported to ", NamedTextColor.GREEN)
                    .append(Component.text(displayName, NamedTextColor.YELLOW))
                    .append(Component.text("'s base" + baseName + "!", NamedTextColor.GREEN))
            );
        } else {
            visitor.sendMessage(
                Component.text("Failed to teleport to " + displayName + "'s base.", NamedTextColor.RED)
            );
        }

        return true;
    }

    /** Tab complete for /visitbase — only show players who have a base set. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("dough.visitbase")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> plugin.getBases().containsKey(p.getUniqueId()))
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
