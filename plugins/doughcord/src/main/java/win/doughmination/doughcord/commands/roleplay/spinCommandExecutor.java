package win.doughmination.doughcord.commands.roleplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class spinCommandExecutor implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final Set<UUID> spinningPlayers = new HashSet<>();

    public spinCommandExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can spin!");
            return true;
        }

        if (spinningPlayers.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("You're already spinning!", NamedTextColor.RED));
            return true;
        }

        spinningPlayers.add(player.getUniqueId());

        player.sendMessage(
                Component.text("You spin me right round, baby, right round!", NamedTextColor.LIGHT_PURPLE)
        );

        new BukkitRunnable() {

            int ticks = 0;
            float yaw = player.getLocation().getYaw();

            @Override
            public void run() {

                if (!player.isOnline()) {
                    spinningPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                yaw += 7.2f;
                player.setRotation(yaw, player.getLocation().getPitch());

                player.getWorld().playSound(
                        player.getLocation(),
                        Sound.ENTITY_PHANTOM_FLAP,
                        0.3f,
                        1.6f
                );

                ticks++;

                if (ticks >= 100) {
                    spinningPlayers.remove(player.getUniqueId());

                    player.sendMessage(
                            Component.text("Like a record, baby.", NamedTextColor.GRAY)
                    );

                    cancel();
                }
            }

        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

}