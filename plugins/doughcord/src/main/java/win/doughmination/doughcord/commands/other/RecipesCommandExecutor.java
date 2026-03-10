/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.commands.other;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import win.doughmination.doughcord.CordMain;

public class RecipesCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String RECIPES_URL = "https://modding.doughmination.co.uk/dougminationcord-recipes";
    private final CordMain plugin;

    public RecipesCommandExecutor(CordMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            player.sendMessage(
                Component.text("Click here to view all spawn egg recipes: ", NamedTextColor.LIGHT_PURPLE)
                    .append(
                        Component.text(RECIPES_URL, NamedTextColor.AQUA)
                            .decorate(TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(RECIPES_URL))
                    )
            );
        } else {
            sender.sendMessage(Component.text("View all spawn egg recipes at: ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(RECIPES_URL, NamedTextColor.AQUA)));
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
