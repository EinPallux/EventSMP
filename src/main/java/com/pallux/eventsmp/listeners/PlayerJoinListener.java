package com.pallux.eventsmp.listeners;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EventSMP plugin;

    public PlayerJoinListener(EventSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDisplayManager().handlePlayerJoin(event.getPlayer());
    }
}
