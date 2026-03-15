/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 License
 */

package win.doughmination.doughutils.endstats;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {

    private final EventManager eventManager;

    public EventListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        if (!eventManager.isEventActive()) return;
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            eventManager.addEggHolder(player.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!eventManager.isEventActive()) return;
        if (event.getEntity() instanceof EnderDragon && event.getDamager() instanceof Player player) {
            eventManager.addDragonDamage(player.getUniqueId(), event.getFinalDamage());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!eventManager.isEventActive()) return;
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;

        Player killer = dragon.getKiller();
        if (killer == null) return;

        if (eventManager.getDaggerHolder() == null) {
            eventManager.setDaggerHolder(killer.getUniqueId());
            eventManager.calculateMuscleToneHolder();
        } else {
            eventManager.addCrossedSwordsHolder(killer.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!eventManager.isEventActive()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        if (item.getType() == Material.ELYTRA && eventManager.getWingHolder() == null) {
            eventManager.setWingHolder(player.getUniqueId());
        } else if (item.getType() == Material.SHULKER_BOX && eventManager.getPackageHolder() == null) {
            eventManager.setPackageHolder(player.getUniqueId());
        }
    }
}
