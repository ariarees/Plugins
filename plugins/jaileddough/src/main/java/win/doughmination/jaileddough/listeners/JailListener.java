/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.jaileddough.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import win.doughmination.api.LibMain;

public class JailListener implements Listener {

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        LibMain doughApi = LibMain.getInstance();
        if (doughApi == null) return;

        if (doughApi.isPlayerJailed(attacker.getUniqueId())) {
            attacker.sendMessage(ChatColor.RED + "You cannot attack other players while jailed!");
            event.setCancelled(true);
        }
    }
}