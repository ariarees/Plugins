/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.travel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import win.doughmination.doughcord.CordMain;
import win.doughmination.api.LibMain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class rtpCommandExecutor implements CommandExecutor, TabCompleter {

    private final CordMain plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    private static final int MIN_RADIUS = 1000;
    private static final int MAX_RADIUS = 10000;
    private static final long COOLDOWN_MS = 3 * 60 * 60 * 1000L;

    private final Random random = new Random();

    public rtpCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= COOLDOWN_MS);
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(player, "rtp")) {
            player.sendMessage(Component.text("You cannot use this command while jailed!", NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (cooldowns.containsKey(uuid)) {
            long elapsed = System.currentTimeMillis() - cooldowns.get(uuid);
            long remaining = COOLDOWN_MS - elapsed;
            if (remaining > 0) {
                long hours   = remaining / 3_600_000;
                long minutes = (remaining % 3_600_000) / 60_000;
                long seconds = (remaining % 60_000) / 1_000;
                player.sendMessage(
                    Component.text("You must wait ", NamedTextColor.RED)
                        .append(Component.text(String.format("%02d:%02d:%02d", hours, minutes, seconds), NamedTextColor.AQUA))
                        .append(Component.text(" before using /rtp again.", NamedTextColor.RED))
                );
                return true;
            }
        }

        player.sendMessage(Component.text("Finding a safe location, please wait...", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            @Override
            public void run() {
                World world = player.getWorld();
                Location loc = findSafeLocation(world);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (loc == null) {
                            player.sendMessage(Component.text("Could not find a safe location. Try again!", NamedTextColor.RED));
                            return;
                        }
                        player.teleport(loc);
                        cooldowns.put(uuid, System.currentTimeMillis());
                        player.sendMessage(
                            Component.text("Teleported to ", NamedTextColor.GREEN)
                                .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.AQUA))
                                .append(Component.text("!", NamedTextColor.GREEN))
                        );
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    private Location findSafeLocation(World world) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int range = MAX_RADIUS - MIN_RADIUS;
            int x = (random.nextInt(range) + MIN_RADIUS) * (random.nextBoolean() ? 1 : -1);
            int z = (random.nextInt(range) + MIN_RADIUS) * (random.nextBoolean() ? 1 : -1);
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (ground == Material.WATER || ground == Material.LAVA || ground == Material.AIR) continue;
            return loc;
        }
        return null;
    }

    public void shutdown() {
        if (cleanupTask != null) { cleanupTask.cancel(); cleanupTask = null; }
        cooldowns.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
