/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.jail.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import win.doughmination.doughutils.Main;

public class JailListener implements Listener {

    private final Main plugin;

    public JailListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        if (plugin.isPlayerJailed(attacker.getUniqueId())) {
            attacker.sendRichMessage("<red>You cannot attack other players while jailed!</red>");
            event.setCancelled(true);
        }
    }
}
