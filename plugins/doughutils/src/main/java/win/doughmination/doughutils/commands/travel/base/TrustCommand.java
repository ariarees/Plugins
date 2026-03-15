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

/**
 * Handles /base trust <player> — allows a player to bypass base protection.
 */
public class TrustCommand {

    private final Main plugin;
    private final BaseDataManager baseData;

    public TrustCommand(Main plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /base trust <player>", NamedTextColor.RED));
            return true;
        }

        UUID ownerUUID = player.getUniqueId();
        if (!plugin.getBases().containsKey(ownerUUID)) {
            player.sendMessage(Component.text("You don't have a base set! Use ", NamedTextColor.RED)
                .append(Component.text("/base set", NamedTextColor.YELLOW))
                .append(Component.text(" first.", NamedTextColor.RED)));
            return true;
        }

        String targetName = args[1];

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text("Player \"" + targetName + "\" not found.", NamedTextColor.RED));
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(ownerUUID)) {
            player.sendMessage(Component.text("You can't trust yourself — you already own the base!", NamedTextColor.RED));
            return true;
        }

        if (baseData.isTrusted(ownerUUID, targetUUID)) {
            player.sendMessage(Component.text(targetName + " is already trusted at your base.", NamedTextColor.YELLOW));
            return true;
        }

        baseData.trust(ownerUUID, targetUUID);
        player.sendMessage(Component.text(targetName + " can now bypass your base protection.", NamedTextColor.GREEN));

        // Notify the trusted player if they're online
        Player online = plugin.getServer().getPlayer(targetUUID);
        if (online != null) {
            online.sendMessage(Component.text(player.getName() + " has trusted you at their base.", NamedTextColor.GREEN));
        }

        return true;
    }
}
