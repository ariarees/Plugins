/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.doughutils.listeners.spawneggs;

import org.bukkit.plugin.java.JavaPlugin;

public class SpawnMain extends JavaPlugin {

    @Override
    public void onEnable() {
        RecipeManager.registerRecipes(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
