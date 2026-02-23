package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Base class for all SMP Events. Each event implementation extends this class.
 */
public abstract class SmpEvent {

    protected final EventSMP plugin;
    protected final String id;
    protected BukkitTask tickTask;
    protected boolean active = false;

    protected SmpEvent(EventSMP plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public abstract void onStart();
    public abstract void onEnd();

    public String getDisplayName() {
        return plugin.getConfigManager().getEventString(id, "display-name");
    }

    /** One-line description shown in chat broadcast. */
    public String getShortDescription() {
        return plugin.getConfigManager().getEventString(id, "short-description");
    }

    /** Bullet-point lines shown in the GUI lore. */
    public List<String> getDescriptionLines() {
        return plugin.getConfigManager().getEventsConfig()
                .getStringList("events." + id + ".description");
    }

    public int getDuration() {
        return plugin.getConfigManager().getEventInt(id, "duration", 300);
    }

    public int getWeight() {
        return plugin.getConfigManager().getEventInt(id, "weight", 10);
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getEventBoolean(id, "enabled", true);
    }

    public String getId() { return id; }
    public boolean isActive() { return active; }

    protected void cancelTickTask() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
