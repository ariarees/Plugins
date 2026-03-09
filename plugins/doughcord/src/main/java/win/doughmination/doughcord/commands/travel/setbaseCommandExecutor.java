package win.doughmination.doughcord.commands.travel;

import win.doughmination.doughcord.CordMain;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import win.doughmination.api.LibMain;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.TabCompleter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class setbaseCommandExecutor implements CommandExecutor, TabCompleter {

    private static final long COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final CordMain plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private org.bukkit.scheduler.BukkitTask cleanupTask;

    public setbaseCommandExecutor(CordMain plugin) {
        this.plugin = plugin;

        // Scheduled cleanup: remove expired cooldowns every 10 seconds
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= COOLDOWN_MILLIS);
            }
        }.runTaskTimer(plugin, 20L, 20L * 10);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!LibMain.getInstance().canUseCommand(player, "setbase")) {
            player.sendMessage(ChatColor.RED + "You cannot use this command while jailed!");
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        // Cooldown check (bypass for ops)
        if (!player.isOp()) {
            long now = System.currentTimeMillis();
            Long lastUsed = cooldowns.get(playerUUID);

            if (lastUsed != null && now - lastUsed < COOLDOWN_MILLIS) {
                long remaining = COOLDOWN_MILLIS - (now - lastUsed);
                long mins = TimeUnit.MILLISECONDS.toMinutes(remaining);
                long secs = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
                player.sendMessage(ChatColor.RED + "You must wait " +
                        ChatColor.YELLOW + mins + "m " + secs + "s" +
                        ChatColor.RED + " before setting your base again.");
                return true;
            }

            cooldowns.put(playerUUID, now);
        }

        Location location = player.getLocation();
        plugin.getBases().put(playerUUID, location);
        plugin.getPlayerDataManager().saveBase(playerUUID, location);

        player.sendMessage(ChatColor.GREEN + "Your base location has been set!");
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}