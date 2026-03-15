/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles /base info — displays base name, location, protection radius, and trusted players.
 */
public class InfoCommand {

    private final Main plugin;
    private final BaseDataManager baseData;

    public InfoCommand(Main plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    public boolean execute(Player player) {
        UUID ownerUUID = player.getUniqueId();
        Location base = plugin.getBases().get(ownerUUID);

        if (base == null) {
            player.sendMessage(Component.text("You don't have a base set! Use ", NamedTextColor.RED)
                .append(Component.text("/base set", NamedTextColor.YELLOW))
                .append(Component.text(" to set one.", NamedTextColor.RED)));
            return true;
        }

        double radius = plugin.getConfig().getDouble("flight.base-radius", 100);

        // Header
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Base Info", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        // Name
        String displayName = baseData.getBaseName(ownerUUID).orElse("Unnamed");
        player.sendMessage(
            Component.text("  Name: ", NamedTextColor.YELLOW)
                .append(Component.text(displayName, NamedTextColor.WHITE))
        );

        // Location
        player.sendMessage(
            Component.text("  World: ", NamedTextColor.YELLOW)
                .append(Component.text(base.getWorld() != null ? base.getWorld().getName() : "?", NamedTextColor.WHITE))
        );
        player.sendMessage(
            Component.text("  Location: ", NamedTextColor.YELLOW)
                .append(Component.text(
                    String.format("%.1f, %.1f, %.1f", base.getX(), base.getY(), base.getZ()),
                    NamedTextColor.WHITE
                ))
        );

        // Protection radius
        player.sendMessage(
            Component.text("  Protection Radius: ", NamedTextColor.YELLOW)
                .append(Component.text((int) radius + " blocks", NamedTextColor.WHITE))
        );

        // Trusted players
        Set<UUID> trusted = baseData.getTrusted(ownerUUID);
        if (trusted.isEmpty()) {
            player.sendMessage(
                Component.text("  Trusted Players: ", NamedTextColor.YELLOW)
                    .append(Component.text("None", NamedTextColor.GRAY))
            );
        } else {
            String names = trusted.stream()
                .map(u -> {
                    org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(u);
                    return op.getName() != null ? op.getName() : u.toString();
                })
                .collect(Collectors.joining(", "));
            player.sendMessage(
                Component.text("  Trusted Players: ", NamedTextColor.YELLOW)
                    .append(Component.text(names, NamedTextColor.WHITE))
            );
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        return true;
    }
}
