package com.pallux.eventsmp;

import com.pallux.eventsmp.commands.EsmpCommand;
import com.pallux.eventsmp.commands.EventsCommand;
import com.pallux.eventsmp.config.ConfigManager;
import com.pallux.eventsmp.listeners.PlayerJoinListener;
import com.pallux.eventsmp.managers.EventManager;
import com.pallux.eventsmp.managers.DisplayManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EventSMP extends JavaPlugin {

    private static EventSMP instance;
    private ConfigManager configManager;
    private EventManager eventManager;
    private DisplayManager displayManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config manager first
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Initialize managers
        displayManager = new DisplayManager(this);
        eventManager = new EventManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register commands
        EsmpCommand esmpCommand = new EsmpCommand(this);
        var esmpCmd = getCommand("esmp");
        if (esmpCmd != null) {
            esmpCmd.setExecutor(esmpCommand);
            esmpCmd.setTabCompleter(esmpCommand);
        }

        var eventsCmd = getCommand("events");
        if (eventsCmd != null) {
            EventsCommand eventsCommand = new EventsCommand(this);
            eventsCmd.setExecutor(eventsCommand);
        }

        getLogger().info("EventSMP has been enabled successfully!");
        getLogger().info("This Plugin is in BETA state, please Report Bugs on Discord:");
        getLogger().info("https://discord.com/invite/FwRtcAdsTC");
        getLogger().info("============================================================");
        getLogger().info("PLEASE NOTE: This plugin was made for Paper/Spigot 1.21+ it");
        getLogger().info("may not work on other Platforms / Server Cores.");
        getLogger().info("============================================================");

    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.stopCurrentEvent(true);
            eventManager.shutdown();
        }
        if (displayManager != null) {
            displayManager.cleanup();
        }
        getLogger().info("EventSMP has been disabled.");
    }

    public void reload() {
        if (eventManager != null) {
            eventManager.stopCurrentEvent(false);
            eventManager.shutdown();
        }
        if (displayManager != null) {
            displayManager.cleanup();
        }
        configManager.loadAll();
        displayManager = new DisplayManager(this);
        eventManager = new EventManager(this);
    }

    public static EventSMP getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }
}
