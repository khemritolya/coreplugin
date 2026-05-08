package org.coreplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class Coreplugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
    }

}
