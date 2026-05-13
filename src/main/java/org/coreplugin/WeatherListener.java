package org.coreplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherListener implements Listener {

    private final SandstormManager sandstorm;

    public WeatherListener(SandstormManager sandstorm) {
        this.sandstorm = sandstorm;
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
            if (!sandstorm.isActive()) sandstorm.startStorm();
        }
    }
}