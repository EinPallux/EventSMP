package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class EnderDisturbanceEvent extends SmpEvent {

    private final Random random = new Random();

    public EnderDisturbanceEvent(EventSMP plugin) {
        super(plugin, "ender_disturbance");
    }

    @Override
    public void onStart() {
        active = true;
        int soundMin = plugin.getConfigManager().getEventInt(id, "sound-interval-min", 100);
        int soundMax = plugin.getConfigManager().getEventInt(id, "sound-interval-max", 600);

        tickTask = new BukkitRunnable() {
            private int nextSoundIn = nextInterval(soundMin, soundMax);
            private int elapsed = 0;

            @Override
            public void run() {
                if (!active) { cancel(); return; }
                elapsed++;

                // Force nearby Endermen to teleport occasionally
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Enderman enderman) {
                            // ~5% chance each tick to randomly teleport an enderman
                            if (random.nextDouble() < 0.05) {
                                Location base = enderman.getLocation();
                                double ox = (random.nextDouble() - 0.5) * 32;
                                double oz = (random.nextDouble() - 0.5) * 32;
                                Location dest = base.clone().add(ox, 0, oz);
                                dest.setY(world.getHighestBlockYAt(dest));
                                enderman.teleport(dest);
                                world.spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0),
                                        20, 0.3, 0.8, 0.3, 0.1);
                            }
                        }
                    }
                }

                // Random teleport sound for players
                if (elapsed >= nextSoundIn) {
                    elapsed = 0;
                    nextSoundIn = nextInterval(soundMin, soundMax);
                    for (World world : Bukkit.getWorlds()) {
                        if (isWorldDisabled(world)) continue;
                        List<Player> players = world.getPlayers();
                        if (players.isEmpty()) continue;
                        // Play to a random subset of players
                        for (Player player : players) {
                            if (random.nextDouble() < 0.6) {
                                player.playSound(player.getLocation(),
                                        Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f,
                                        0.7f + (float)(random.nextDouble() * 0.6));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private int nextInterval(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        cancelTickTask();
    }
}
