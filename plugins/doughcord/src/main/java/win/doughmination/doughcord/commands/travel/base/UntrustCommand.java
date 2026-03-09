/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel.base;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import win.doughmination.doughcord.CordMain;

import java.util.UUID;

/**
 * Handles /base untrust <player> — revokes a player's bypass access to your base.
 */
public class UntrustCommand {

    private final CordMain plugin;
    private final BaseDataManager baseData;

    public UntrustCommand(CordMain plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /base untrust <player>", NamedTextColor.RED));
            return true;
        }

        UUID ownerUUID = player.getUniqueId();

        String targetName = args[1];

        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text("Player \"" + targetName + "\" not found.", NamedTextColor.RED));
            return true;
        }

        UUID targetUUID = target.getUniqueId();

        if (!baseData.isTrusted(ownerUUID, targetUUID)) {
            player.sendMessage(Component.text(targetName + " is not trusted at your base.", NamedTextColor.YELLOW));
            return true;
        }

        baseData.untrust(ownerUUID, targetUUID);
        player.sendMessage(Component.text(targetName + " has been removed from your base trust list.", NamedTextColor.GREEN));

        // Notify if online
        Player online = plugin.getServer().getPlayer(targetUUID);
        if (online != null) {
            online.sendMessage(Component.text(player.getName() + " has removed you from their base trust list.", NamedTextColor.YELLOW));
        }

        return true;
    }
}
