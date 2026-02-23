package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FreezingWindsEvent extends SmpEvent {

    private final Map<UUID, Integer> stillTicks = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Random random = new Random();

    public FreezingWindsEvent(EventSMP plugin) {
        super(plugin, "freezing_winds");
    }

    @Override
    public void onStart() {
        active = true;
        int threshold = plugin.getConfigManager().getEventInt(id, "still-threshold-ticks", 60);
        int slowAmp = plugin.getConfigManager().getEventInt(id, "slowness-amplifier", 1);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        UUID uuid = player.getUniqueId();
                        Location last = lastLocations.get(uuid);
                        Location current = player.getLocation();

                        boolean moved = last == null ||
                                Math.abs(current.getX() - last.getX()) > 0.1 ||
                                Math.abs(current.getZ() - last.getZ()) > 0.1 ||
                                player.isJumping();

                        if (moved) {
                            stillTicks.put(uuid, 0);
                            player.removePotionEffect(PotionEffectType.SLOWNESS);
                        } else {
                            int ticks = stillTicks.getOrDefault(uuid, 0) + 1;
                            stillTicks.put(uuid, ticks);
                            if (ticks >= threshold) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slowAmp, false, false));
                            }
                        }

                        lastLocations.put(uuid, current.clone());

                        // Snowflake particles blowing sideways
                        spawnWindParticles(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnWindParticles(Player player) {
        Location loc = player.getLocation().add(
                (random.nextDouble() - 0.5) * 3,
                random.nextDouble() * 2,
                (random.nextDouble() - 0.5) * 3);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        cancelTickTask();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
        stillTicks.clear();
        lastLocations.clear();
    }
}
