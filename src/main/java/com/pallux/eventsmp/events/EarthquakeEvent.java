package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class EarthquakeEvent extends SmpEvent {

    private final Random random = new Random();

    public EarthquakeEvent(EventSMP plugin) {
        super(plugin, "earthquake");
    }

    @Override
    public void onStart() {
        active = true;
        int intervalTicks   = plugin.getConfigManager().getEventInt(id, "quake-interval-ticks", 400);
        int nauseaTicks     = plugin.getConfigManager().getEventInt(id, "nausea-duration-ticks", 60);
        double knockback    = plugin.getConfigManager().getEventDouble(id, "knockback-strength", 0.4);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                triggerQuake(nauseaTicks, knockback);
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void triggerQuake(int nauseaTicks, double knockback) {
        for (World world : Bukkit.getWorlds()) {
            if (isWorldDisabled(world)) continue;
            for (Player player : world.getPlayers()) {
                // Nausea
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaTicks, 0, false, false));

                // Random slight knockback
                double angle = random.nextDouble() * 2 * Math.PI;
                double vx = Math.cos(angle) * knockback;
                double vz = Math.sin(angle) * knockback;
                player.setVelocity(new Vector(vx, 0.15, vz));

                // Block crack particles around feet
                Location loc = player.getLocation();
                Material ground = loc.clone().subtract(0, 1, 0).getBlock().getType();
                if (ground.isAir()) ground = Material.STONE;
                world.spawnParticle(Particle.BLOCK, loc, 20, 0.5, 0.1, 0.5, 0.3, ground.createBlockData());

                // Rumble sound
                player.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.6f);
                player.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
            }
        }
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
                player.removePotionEffect(PotionEffectType.NAUSEA);
            }
        }
    }
}
