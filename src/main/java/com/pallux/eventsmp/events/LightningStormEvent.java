package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class LightningStormEvent extends SmpEvent {

    private final Random random = new Random();

    public LightningStormEvent(EventSMP plugin) {
        super(plugin, "lightning_storm");
    }

    @Override
    public void onStart() {
        active = true;
        int intervalTicks = plugin.getConfigManager().getEventInt(id, "strike-interval-ticks", 60);
        int strikesPerWave = plugin.getConfigManager().getEventInt(id, "strikes-per-wave", 2);
        double proximityChance = plugin.getConfigManager().getEventDouble(id, "player-proximity-chance", 0.3);
        int proximityRange = plugin.getConfigManager().getEventInt(id, "player-proximity-range", 15);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    List<Player> players = List.copyOf(world.getPlayers());
                    if (players.isEmpty()) continue;

                    for (int i = 0; i < strikesPerWave; i++) {
                        Player anchor = players.get(random.nextInt(players.size()));
                        Location strikeLoc;

                        if (random.nextDouble() < proximityChance) {
                            // Strike near a player
                            double angle = random.nextDouble() * 2 * Math.PI;
                            double dist = 3 + random.nextDouble() * (proximityRange - 3);
                            strikeLoc = anchor.getLocation().add(
                                    Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        } else {
                            // Random strike within 60 blocks of a player
                            double ox = (random.nextDouble() - 0.5) * 120;
                            double oz = (random.nextDouble() - 0.5) * 120;
                            strikeLoc = anchor.getLocation().add(ox, 0, oz);
                        }

                        // Get highest block at location
                        strikeLoc.setY(world.getHighestBlockYAt(strikeLoc));

                        spawnSafeLightning(world, strikeLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void spawnSafeLightning(World world, Location loc) {
        // Spawn lightning that doesn't start fires
        LightningStrike strike = world.strikeLightning(loc);
        // Electric particle burst at impact
        world.spawnParticle(Particle.END_ROD, loc, 30, 0.3, 1.0, 0.3, 0.1);
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, 0.5, 0.5, 0.5, 0.15);
        // Remove fires caused by the strike in a small radius
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int dy = -1; dy <= 3; dy++) {
                            Location check = loc.clone().add(dx, dy, dz);
                            if (check.getBlock().getType() == Material.FIRE) {
                                check.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 2L);
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
