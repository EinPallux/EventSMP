package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpeedSurgeEvent extends SmpEvent {

    public SpeedSurgeEvent(EventSMP plugin) {
        super(plugin, "speed_surge");
    }

    @Override
    public void onStart() {
        active = true;
        int amplifier = plugin.getConfigManager().getEventInt(id, "speed-amplifier", 1);

        // Apply speed to all current players
        for (World world : Bukkit.getWorlds()) {
            if (isWorldDisabled(world)) continue;
            for (Player player : world.getPlayers()) {
                applySpeed(player, amplifier);
            }
        }

        // Tick: maintain speed, particles for sprinting players
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        // Keep speed refreshed
                        if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
                            applySpeed(player, amplifier);
                        }
                        // Sprint trail
                        if (player.isSprinting()) {
                            Location loc = player.getLocation();
                            world.spawnParticle(Particle.ELECTRIC_SPARK,
                                    loc.add(0, 0.1, 0), 4, 0.2, 0.0, 0.2, 0.05);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void applySpeed(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, amplifier, false, false));
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
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }
}
