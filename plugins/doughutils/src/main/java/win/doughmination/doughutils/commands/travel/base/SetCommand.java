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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;



import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles /base set — sets the player's base to their current location.
 * Carries a 30-minute cooldown (ops are exempt).
 */
public class SetCommand {

    private static final long COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final Main plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public SetCommand(Main plugin) {
        this.plugin = plugin;
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(e -> now - e.getValue() >= COOLDOWN_MILLIS);
            }
        }.runTaskTimer(plugin, 20L, 20L * 10);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    public boolean execute(Player player) {
        if (!plugin.canUseCommand(player)) {
            player.sendMessage(Component.text("You cannot use this command while jailed!", NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (!player.isOp()) {
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(uuid);
            if (last != null && now - last < COOLDOWN_MILLIS) {
                long remaining = COOLDOWN_MILLIS - (now - last);
                long mins = TimeUnit.MILLISECONDS.toMinutes(remaining);
                long secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                player.sendMessage(
                    Component.text("You must wait ", NamedTextColor.RED)
                        .append(Component.text(mins + "m " + secs + "s", NamedTextColor.YELLOW))
                        .append(Component.text(" before setting your base again.", NamedTextColor.RED))
                );
                return true;
            }
            cooldowns.put(uuid, now);
        }

        Location location = player.getLocation();
        plugin.getBases().put(uuid, location);
        plugin.getPlayerDataManager().saveBase(uuid, location);

        player.sendMessage(Component.text("Your base location has been set!", NamedTextColor.GREEN));
        return true;
    }
}
