package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScorchingSunEvent extends SmpEvent {

    public ScorchingSunEvent(EventSMP plugin) {
        super(plugin, "scorching_sun");
    }

    @Override
    public void onStart() {
        active = true;
        int checkInterval = plugin.getConfigManager().getEventInt(id, "check-interval-ticks", 20);
        int fireTicks     = plugin.getConfigManager().getEventInt(id, "fire-ticks-per-check", 40);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;

                    // Only active during daytime (time 0-12300 is day)
                    long time = world.getTime();
                    if (time > 12300 && time < 23850) continue; // night/dawn/dusk

                    for (Player player : world.getPlayers()) {
                        Location loc = player.getLocation();

                        // Check if player is in direct sunlight: highest block above them is sky
                        int highestY = world.getHighestBlockYAt(loc);
                        boolean inSunlight = loc.getBlockY() >= highestY;

                        if (inSunlight) {
                            player.setFireTicks(Math.max(player.getFireTicks(), fireTicks));

                            // Heat shimmer particles
                            world.spawnParticle(Particle.FLAME,
                                    loc.clone().add(
                                            (Math.random() - 0.5) * 0.6,
                                            1.8,
                                            (Math.random() - 0.5) * 0.6),
                                    1, 0.0, 0.0, 0.0, 0.01);
                            world.spawnParticle(Particle.SMALL_FLAME,
                                    loc.clone().add(0, 2.2, 0), 2, 0.3, 0.1, 0.3, 0.0);
                        } else {
                            // In shade: extinguish if fire ticks > 0
                            if (player.getFireTicks() > 0) {
                                player.setFireTicks(0);
                                // Cool-down puff
                                world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0),
                                        8, 0.3, 0.3, 0.3, 0.02);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, checkInterval, checkInterval);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        cancelTickTask();
        // Extinguish all players
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (player.getFireTicks() > 0) player.setFireTicks(0);
            }
        }
    }
}
