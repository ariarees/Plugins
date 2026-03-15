/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.commands.travel.base;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Listens for player actions within a base protection radius.
 * Untrusted players receive a warning and have their action cancelled.
 * They can still physically walk around — only interactions are blocked.
 */
public class BaseProtectionListener implements Listener {

    private final Main plugin;
    private final BaseDataManager baseData;

    // Tracks which players have already been warned (per base owner) to avoid spam
    // Key: visitor UUID — Value: set of owner UUIDs whose base they've been warned about
    private final java.util.Map<UUID, Set<UUID>> warnedPlayers = new java.util.concurrent.ConcurrentHashMap<>();

    public BaseProtectionListener(Main plugin, BaseDataManager baseData) {
        this.plugin = plugin;
        this.baseData = baseData;
    }

    // -----------------------------------------------------------------------
    // Core check
    // -----------------------------------------------------------------------

    /**
     * Returns the UUID of the base owner if the location is inside a protected base
     * that the visitor is NOT trusted in. Returns null if no conflict.
     */
    private UUID getViolatedOwner(Player visitor, Location loc) {
        double radius = plugin.getConfig().getDouble("flight.base-radius", 100);

        for (Map.Entry<UUID, Location> entry : plugin.getBases().entrySet()) {
            UUID ownerUUID = entry.getKey();
            Location baseCenter = entry.getValue();

            // Don't protect against yourself
            if (ownerUUID.equals(visitor.getUniqueId())) continue;
            // Ops bypass protection
            if (visitor.isOp()) continue;
            // Must be same world
            if (baseCenter.getWorld() == null || !baseCenter.getWorld().equals(loc.getWorld())) continue;
            // Check radius
            if (baseCenter.distance(loc) > radius) continue;
            // Check trust
            if (baseData.isTrusted(ownerUUID, visitor.getUniqueId())) continue;

            return ownerUUID;
        }
        return null;
    }

    private void warnIfNew(Player visitor, UUID ownerUUID) {
        Set<UUID> warned = warnedPlayers.computeIfAbsent(visitor.getUniqueId(), k -> new HashSet<>());
        if (warned.add(ownerUUID)) {
            // First offence — send warning
            String ownerName = plugin.getServer().getOfflinePlayer(ownerUUID).getName();
            String baseName = baseData.getBaseName(ownerUUID)
                    .map(n -> " (\"" + n + "\")")
                    .orElse("");
            visitor.sendMessage(
                Component.text("⚠ You are in ", NamedTextColor.YELLOW)
                    .append(Component.text(ownerName != null ? ownerName : "someone", NamedTextColor.GOLD))
                    .append(Component.text("'s base" + baseName + ". You cannot interact here.", NamedTextColor.YELLOW))
            );
        }
    }

    // Clear warn cache when a player leaves a base zone (checked on move)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only process when crossing a block boundary for performance
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Set<UUID> warned = warnedPlayers.get(player.getUniqueId());
        if (warned == null || warned.isEmpty()) return;

        double radius = plugin.getConfig().getDouble("flight.base-radius", 100);

        // Remove any owners whose base the player has now left
        warned.removeIf(ownerUUID -> {
            Location base = plugin.getBases().get(ownerUUID);
            if (base == null) return true;
            if (!base.getWorld().equals(event.getTo().getWorld())) return true;
            return base.distance(event.getTo()) > radius;
        });
    }

    // -----------------------------------------------------------------------
    // Protected events
    // -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        UUID owner = getViolatedOwner(event.getPlayer(), event.getBlock().getLocation());
        if (owner == null) return;
        event.setCancelled(true);
        warnIfNew(event.getPlayer(), owner);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        UUID owner = getViolatedOwner(event.getPlayer(), event.getBlock().getLocation());
        if (owner == null) return;
        event.setCancelled(true);
        warnIfNew(event.getPlayer(), owner);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        UUID owner = getViolatedOwner(event.getPlayer(), event.getClickedBlock().getLocation());
        if (owner == null) return;
        event.setCancelled(true);
        warnIfNew(event.getPlayer(), owner);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        UUID owner = getViolatedOwner(attacker, event.getEntity().getLocation());
        if (owner == null) return;
        event.setCancelled(true);
        warnIfNew(attacker, owner);
    }
}
