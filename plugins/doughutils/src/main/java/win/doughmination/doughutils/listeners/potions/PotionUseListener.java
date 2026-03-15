/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.potions;
import win.doughmination.doughutils.Main;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class PotionUseListener implements Listener {

    private final NamespacedKey potionTypeKey;
    private final Main plugin;

    public PotionUseListener(Main plugin) {
        this.plugin = plugin;
        potionTypeKey = new NamespacedKey(plugin, "potionType");
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(potionTypeKey, PersistentDataType.STRING)) return;
        String type = meta.getPersistentDataContainer().get(potionTypeKey, PersistentDataType.STRING);
        Player player = event.getPlayer();
        if ("growth".equals(type)) {
            player.sendMessage(Component.text("You feel yourself growing larger!", NamedTextColor.GOLD));
            graduallyChangeScale(player, 1.0, 1.6, 20L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                graduallyChangeScale(player, 1.6, 1.0, 20L);
                player.sendMessage(Component.text("Your size has been returned to normal.", NamedTextColor.YELLOW));
            }, 600L);
        } else if ("shrink".equals(type)) {
            player.sendMessage(Component.text("You feel yourself shrinking smaller!", NamedTextColor.AQUA));
            graduallyChangeScale(player, 1.0, 0.4, 20L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                graduallyChangeScale(player, 0.4, 1.0, 20L);
                player.sendMessage(Component.text("Your size has been returned to normal.", NamedTextColor.YELLOW));
            }, 600L);
        }
    }

    private void graduallyChangeScale(Player player, double start, double end, long durationTicks) {
        double difference = end - start;
        int steps = (int) durationTicks;
        double stepChange = difference / steps;
        new BukkitRunnable() {
            int currentStep = 0;
            double currentScale = start;

            @Override
            public void run() {
                if (currentStep >= steps) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "attribute " + player.getName() + " minecraft:scale base set " + end);
                    cancel();
                    return;
                }
                currentScale += stepChange;
                String scaleFormatted = String.format("%.2f", currentScale);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "attribute " + player.getName() + " minecraft:scale base set " + scaleFormatted);
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
