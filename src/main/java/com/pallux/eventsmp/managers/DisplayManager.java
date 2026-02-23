package com.pallux.eventsmp.managers;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.utils.ColorUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager {

    private final EventSMP plugin;
    private BossBar bossBar;
    private BukkitTask actionBarTask;
    private String currentActionBarText;

    /** Players who have opted out of seeing the event bar. */
    private final Set<UUID> hiddenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public DisplayManager(EventSMP plugin) {
        this.plugin = plugin;
        initBossBar();
        startNoneDisplay();
    }

    private void initBossBar() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        BossBar.Color color   = parseBossBarColor(config.getString("bossbar.color", "RED"));
        BossBar.Overlay style = parseBossBarOverlay(config.getString("bossbar.style", "SOLID"));
        float progress        = (float) config.getDouble("bossbar.progress", 1.0);

        String noneText = plugin.getConfigManager().getMessage("bossbar-none");
        bossBar = BossBar.bossBar(ColorUtil.colorize(noneText), progress, color, style);
    }

    private void startNoneDisplay() {
        String mode = getMode();
        if (mode.equals("BOSSBAR")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!hiddenPlayers.contains(player.getUniqueId())) {
                    player.showBossBar(bossBar);
                }
            }
        } else {
            currentActionBarText = plugin.getConfigManager().getMessage("actionbar-none");
            startActionBarTask();
        }
    }

    public void updateDisplay(String eventName, int remainingSeconds, int totalDuration) {
        String mode        = getMode();
        boolean showProg   = plugin.getConfigManager().getConfig().getBoolean("bossbar.show-progress", true);

        if (mode.equals("BOSSBAR")) {
            String text = ColorUtil.replace(
                    plugin.getConfigManager().getMessage("bossbar-active"), "{event_name}", eventName);
            bossBar.name(ColorUtil.colorize(text));

            if (showProg && totalDuration > 0) {
                float progress = Math.max(0f, Math.min(1f, (float) remainingSeconds / totalDuration));
                bossBar.progress(progress);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!hiddenPlayers.contains(player.getUniqueId())) {
                    player.showBossBar(bossBar);
                } else {
                    player.hideBossBar(bossBar);
                }
            }
        } else {
            currentActionBarText = ColorUtil.replace(
                    plugin.getConfigManager().getMessage("actionbar-active"), "{event_name}", eventName);
            if (actionBarTask == null || actionBarTask.isCancelled()) startActionBarTask();
        }
    }

    public void clearDisplay() {
        String mode = getMode();
        if (mode.equals("BOSSBAR")) {
            String noneText = plugin.getConfigManager().getMessage("bossbar-none");
            bossBar.name(ColorUtil.colorize(noneText));
            bossBar.progress(1.0f);
        } else {
            currentActionBarText = plugin.getConfigManager().getMessage("actionbar-none");
        }
    }

    private void startActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                Component msg = ColorUtil.colorize(currentActionBarText != null ? currentActionBarText : "");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!hiddenPlayers.contains(player.getUniqueId())) {
                        player.sendActionBar(msg);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /** Called when a player joins — show or hide bossbar based on their preference. */
    public void handlePlayerJoin(Player player) {
        if (getMode().equals("BOSSBAR")) {
            if (!hiddenPlayers.contains(player.getUniqueId())) {
                player.showBossBar(bossBar);
            }
        }
    }

    /**
     * Toggles the event bar visibility for a player.
     * @return true if the bar is now hidden, false if now shown.
     */
    public boolean toggleBar(Player player) {
        UUID uuid = player.getUniqueId();
        if (hiddenPlayers.contains(uuid)) {
            hiddenPlayers.remove(uuid);
            // Immediately show bossbar if in that mode
            if (getMode().equals("BOSSBAR")) player.showBossBar(bossBar);
            return false; // now shown
        } else {
            hiddenPlayers.add(uuid);
            if (getMode().equals("BOSSBAR")) player.hideBossBar(bossBar);
            return true; // now hidden
        }
    }

    public boolean isHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    public void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) player.hideBossBar(bossBar);
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
    }

    private String getMode() {
        return plugin.getConfigManager().getConfig()
                .getString("display-mode", "BOSSBAR").toUpperCase();
    }

    private BossBar.Color parseBossBarColor(String name) {
        return switch (name.toUpperCase()) {
            case "PINK"   -> BossBar.Color.PINK;
            case "BLUE"   -> BossBar.Color.BLUE;
            case "GREEN"  -> BossBar.Color.GREEN;
            case "YELLOW" -> BossBar.Color.YELLOW;
            case "PURPLE" -> BossBar.Color.PURPLE;
            case "WHITE"  -> BossBar.Color.WHITE;
            default       -> BossBar.Color.RED;
        };
    }

    private BossBar.Overlay parseBossBarOverlay(String name) {
        return switch (name.toUpperCase()) {
            case "SEGMENTED_6"  -> BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10" -> BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12" -> BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20" -> BossBar.Overlay.NOTCHED_20;
            default             -> BossBar.Overlay.PROGRESS;
        };
    }
}
