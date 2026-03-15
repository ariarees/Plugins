/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles /base name <name> — names the player's base with a 30-minute cooldown.
 */
public class NameCommand {

    private final Main plugin;
    private final BaseDataManager baseData;

    public NameCommand(Main plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /base name <name>", NamedTextColor.RED));
            return true;
        }

        UUID ownerUUID = player.getUniqueId();
        if (!plugin.getBases().containsKey(ownerUUID)) {
            player.sendMessage(Component.text("You don't have a base set! Use ", NamedTextColor.RED)
                .append(Component.text("/base set", NamedTextColor.YELLOW))
                .append(Component.text(" first.", NamedTextColor.RED)));
            return true;
        }

        // Join args in case name has spaces, limit to 32 chars
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (name.length() > 32) {
            player.sendMessage(Component.text("Base name must be 32 characters or fewer.", NamedTextColor.RED));
            return true;
        }

        // Cooldown check (ops bypass)
        if (!player.isOp()) {
            long remaining = baseData.getNameCooldownRemaining(ownerUUID);
            if (remaining > 0) {
                long mins = TimeUnit.MILLISECONDS.toMinutes(remaining);
                long secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                player.sendMessage(
                    Component.text("You must wait ", NamedTextColor.RED)
                        .append(Component.text(mins + "m " + secs + "s", NamedTextColor.YELLOW))
                        .append(Component.text(" before renaming your base again.", NamedTextColor.RED))
                );
                return true;
            }
        }

        boolean set = baseData.setBaseName(ownerUUID, name, player.isOp());
        if (set) {
            player.sendMessage(Component.text("Your base is now named ", NamedTextColor.GREEN)
                .append(Component.text("\"" + name + "\"", NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.GREEN)));
        } else {
            // Shouldn't happen given the pre-check above, but just in case
            player.sendMessage(Component.text("Failed to set base name — you may still be on cooldown.", NamedTextColor.RED));
        }

        return true;
    }
}
