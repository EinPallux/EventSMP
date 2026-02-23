package com.pallux.eventsmp.config;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final EventSMP plugin;

    private FileConfiguration config;
    private FileConfiguration eventsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;

    private File configFile;
    private File eventsFile;
    private File messagesFile;
    private File guiFile;

    public ConfigManager(EventSMP plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        loadConfig();
        loadEventsConfig();
        loadMessagesConfig();
        loadGuiConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
        applyDefaults(config, "config.yml");
    }

    private void loadEventsConfig() {
        eventsFile = new File(plugin.getDataFolder(), "events.yml");
        if (!eventsFile.exists()) plugin.saveResource("events.yml", false);
        eventsConfig = YamlConfiguration.loadConfiguration(eventsFile);
        applyDefaults(eventsConfig, "events.yml");
    }

    private void loadMessagesConfig() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        applyDefaults(messagesConfig, "messages.yml");
    }

    private void loadGuiConfig() {
        guiFile = new File(plugin.getDataFolder(), "event-gui.yml");
        if (!guiFile.exists()) plugin.saveResource("event-gui.yml", false);
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        applyDefaults(guiConfig, "event-gui.yml");
    }

    private void applyDefaults(FileConfiguration config, String resourcePath) {
        InputStream defStream = plugin.getResource(resourcePath);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    public FileConfiguration getConfig()        { return config; }
    public FileConfiguration getEventsConfig()  { return eventsConfig; }
    public FileConfiguration getMessagesConfig(){ return messagesConfig; }
    public FileConfiguration getGuiConfig()     { return guiConfig; }

    /** Returns a single-line message string. */
    public String getMessage(String key) {
        return messagesConfig.getString("messages." + key, "&cMissing message: " + key);
    }

    /**
     * Returns a message that may be stored as either a YAML list (multi-line)
     * or a plain string. Always returns a List of lines.
     */
    public List<String> getMessageLines(String key) {
        String path = "messages." + key;
        if (messagesConfig.isList(path)) {
            return messagesConfig.getStringList(path);
        }
        // Fallback: single string — split on literal \n
        String single = messagesConfig.getString(path, "");
        if (single.contains("\\n")) {
            return List.of(single.split("\\\\n"));
        }
        return List.of(single);
    }

    public String getEventString(String eventId, String key) {
        return eventsConfig.getString("events." + eventId + "." + key, "");
    }

    public int getEventInt(String eventId, String key, int def) {
        return eventsConfig.getInt("events." + eventId + "." + key, def);
    }

    public double getEventDouble(String eventId, String key, double def) {
        return eventsConfig.getDouble("events." + eventId + "." + key, def);
    }

    public boolean getEventBoolean(String eventId, String key, boolean def) {
        return eventsConfig.getBoolean("events." + eventId + "." + key, def);
    }
}