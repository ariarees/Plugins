/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;



import java.util.UUID;

/**
 * Handles /base (no args) and /base tp — teleports the player to their saved base.
 */
public class TpCommand {

    private final Main plugin;

    public TpCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean execute(Player player) {
        if (!plugin.canUseCommand(player)) {
            player.sendMessage(Component.text("You cannot teleport while jailed!", NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (!plugin.getBases().containsKey(uuid)) {
            player.sendMessage(Component.text("You have not set a base! Use ", NamedTextColor.RED)
                .append(Component.text("/base set", NamedTextColor.YELLOW))
                .append(Component.text(" to set one.", NamedTextColor.RED)));
            return true;
        }

        Location baseLocation = plugin.getBases().get(uuid);
        plugin.getBackLocationManager().set(uuid, player.getLocation());
        if (player.teleport(baseLocation)) {
            String soundName = plugin.getConfig().getString("sounds.base", "ENTITY_ENDERMAN_TELEPORT");
            Sound sound = org.bukkit.Registry.SOUNDS.get(
                org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase())
            );
            if (sound != null) player.playSound(baseLocation, sound, 1.0f, 1.0f);
            player.sendMessage(Component.text("Teleporting to your base!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Teleport failed!", NamedTextColor.GOLD));
        }

        return true;
    }
}
