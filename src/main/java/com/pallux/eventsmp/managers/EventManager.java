package com.pallux.eventsmp.managers;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.events.*;
import com.pallux.eventsmp.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private final EventSMP plugin;
    private final Map<String, SmpEvent> registeredEvents = new LinkedHashMap<>();
    private SmpEvent currentEvent = null;
    private int remainingSeconds = 0;
    private BukkitTask countdownTask;
    private BukkitTask autoTriggerTask;

    public EventManager(EventSMP plugin) {
        this.plugin = plugin;
        registerEvents();
        startAutoTrigger();
    }

    private void registerEvents() {
        register(new MeteorShowerEvent(plugin));
        register(new OreFrenzyEvent(plugin));
        register(new TornadoTroubleEvent(plugin));
        register(new BloodMoonEvent(plugin));
        register(new GravityGlitchEvent(plugin));
        register(new LightningStormEvent(plugin));
        register(new SpeedSurgeEvent(plugin));
        register(new FreezingWindsEvent(plugin));
        register(new CreeperPanicEvent(plugin));
        register(new XpRainEvent(plugin));
        register(new GoldenTouchEvent(plugin));
        register(new HealingAuraEvent(plugin));
        register(new ToxicWastelandEvent(plugin));
        register(new LuckyDayEvent(plugin));
        register(new EarthquakeEvent(plugin));
    }

    private void register(SmpEvent event) {
        registeredEvents.put(event.getId(), event);
    }

    private void startAutoTrigger() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("auto-events.enabled", true)) return;
        int base = config.getInt("auto-events.interval-between-events", 600);
        int variation = config.getInt("auto-events.interval-variation", 120);
        scheduleNextAutoEvent(base, variation);
    }

    private void scheduleNextAutoEvent(int base, int variation) {
        int delay = base + new Random().nextInt(variation * 2 + 1) - variation;
        delay = Math.max(60, delay);
        autoTriggerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentEvent == null) {
                    SmpEvent random = pickRandomEvent();
                    if (random != null) startEvent(random.getId());
                }
                scheduleNextAutoEvent(base, variation);
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private SmpEvent pickRandomEvent() {
        List<SmpEvent> available = new ArrayList<>();
        int totalWeight = 0;
        for (SmpEvent event : registeredEvents.values()) {
            if (event.isEnabled()) {
                available.add(event);
                totalWeight += event.getWeight();
            }
        }
        if (available.isEmpty()) return null;
        Random rand = new Random();
        int roll = rand.nextInt(totalWeight);
        int cumulative = 0;
        for (SmpEvent event : available) {
            cumulative += event.getWeight();
            if (roll < cumulative) return event;
        }
        return available.get(available.size() - 1);
    }

    public boolean startEvent(String eventId) {
        SmpEvent event = registeredEvents.get(eventId);
        if (event == null) return false;
        if (currentEvent != null) return false;

        currentEvent = event;
        remainingSeconds = event.getDuration();
        event.onStart();

        broadcastStart(event);
        playEventSound(event, "start-sound");
        plugin.getDisplayManager().updateDisplay(event.getDisplayName(), remainingSeconds, event.getDuration());
        startCountdown(event);

        plugin.getLogger().info("Started event: " + eventId);
        return true;
    }

    private void startCountdown(SmpEvent event) {
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentEvent == null || !currentEvent.isActive()) { cancel(); return; }
                remainingSeconds--;
                plugin.getDisplayManager().updateDisplay(
                        currentEvent.getDisplayName(), remainingSeconds, event.getDuration());
                if (remainingSeconds <= 0) {
                    stopCurrentEvent(true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopCurrentEvent(boolean announce) {
        if (currentEvent == null) return;
        SmpEvent event = currentEvent;
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
        event.onEnd();
        if (announce) {
            broadcastEnd(event);
            playEventSound(event, "end-sound");
        }
        currentEvent = null;
        remainingSeconds = 0;
        plugin.getDisplayManager().clearDisplay();
        plugin.getLogger().info("Stopped event: " + event.getId());
    }

    /** Formats seconds as "Xm Ys" or "Xs" */
    public static String formatDuration(int seconds) {
        if (seconds >= 60) {
            int m = seconds / 60;
            int s = seconds % 60;
            return s > 0 ? m + "m " + s + "s" : m + "m";
        }
        return seconds + "s";
    }

    private void broadcastStart(SmpEvent event) {
        String desc = event.getShortDescription();
        if (desc == null || desc.isEmpty()) desc = "";
        String duration = formatDuration(event.getDuration());
        List<String> lines = plugin.getConfigManager().getMessageLines("event-start-broadcast");
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                String raw = ColorUtil.replace(line,
                        "{event_name}", event.getDisplayName(),
                        "{event}", event.getId(),
                        "{description}", desc,
                        "{duration}", duration);
                player.sendMessage(ColorUtil.colorize(raw));
            }
        }
    }

    private void broadcastEnd(SmpEvent event) {
        List<String> lines = plugin.getConfigManager().getMessageLines("event-end-broadcast");
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                String raw = ColorUtil.replace(line,
                        "{event_name}", event.getDisplayName(),
                        "{event}", event.getId());
                player.sendMessage(ColorUtil.colorize(raw));
            }
        }
    }

    private void playEventSound(SmpEvent event, String soundKey) {
        String soundName = plugin.getConfigManager().getEventString(event.getId(), soundKey);
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : Bukkit.getOnlinePlayers())
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    public void shutdown() {
        if (autoTriggerTask != null && !autoTriggerTask.isCancelled()) autoTriggerTask.cancel();
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
    }

    public SmpEvent getCurrentEvent() { return currentEvent; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public Map<String, SmpEvent> getRegisteredEvents() { return Collections.unmodifiableMap(registeredEvents); }
    public SmpEvent getEvent(String id) { return registeredEvents.get(id); }
}
