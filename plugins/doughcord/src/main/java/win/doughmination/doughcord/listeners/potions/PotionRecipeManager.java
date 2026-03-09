/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughcord.listeners.potions;

import win.doughmination.doughcord.CordMain;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class PotionRecipeManager {

    public static void registerRecipes(CordMain plugin) {

        // --- Growth Potion Recipe ---
        ItemStack growthPotion = new ItemStack(Material.POTION);
        ItemMeta growthMeta = growthPotion.getItemMeta();

        if (growthMeta != null) {

            growthMeta.displayName(
                    Component.text("Growth Potion", NamedTextColor.YELLOW)
            );

            growthMeta.lore(List.of(
                    Component.text("Drink to grow larger!", NamedTextColor.GRAY)
            ));

            growthMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "potionType"),
                    PersistentDataType.STRING,
                    "growth"
            );

            growthPotion.setItemMeta(growthMeta);
        }

        ShapelessRecipe growthRecipe = new ShapelessRecipe(
                new NamespacedKey(plugin, "growth_potion"),
                growthPotion
        );

        growthRecipe.addIngredient(Material.POTION);
        growthRecipe.addIngredient(Material.STICKY_PISTON);

        plugin.getServer().addRecipe(growthRecipe);


        // --- Shrink Potion Recipe ---
        ItemStack shrinkPotion = new ItemStack(Material.POTION);
        ItemMeta shrinkMeta = shrinkPotion.getItemMeta();

        if (shrinkMeta != null) {

            shrinkMeta.displayName(
                    Component.text("Shrink Potion", NamedTextColor.AQUA)
            );

            shrinkMeta.lore(List.of(
                    Component.text("Drink to shrink smaller!", NamedTextColor.GRAY)
            ));

            shrinkMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "potionType"),
                    PersistentDataType.STRING,
                    "shrink"
            );

            shrinkPotion.setItemMeta(shrinkMeta);
        }

        ShapelessRecipe shrinkRecipe = new ShapelessRecipe(
                new NamespacedKey(plugin, "shrink_potion"),
                shrinkPotion
        );

        shrinkRecipe.addIngredient(Material.POTION);
        shrinkRecipe.addIngredient(Material.PISTON);

        plugin.getServer().addRecipe(shrinkRecipe);
    }
}