package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class XpRainEvent extends SmpEvent {

    private final Random random = new Random();

    public XpRainEvent(EventSMP plugin) {
        super(plugin, "xp_rain");
    }

    @Override
    public void onStart() {
        active = true;
        int intervalTicks = plugin.getConfigManager().getEventInt(id, "orb-interval-ticks", 40);
        int orbsPerPlayer = plugin.getConfigManager().getEventInt(id, "orbs-per-player", 2);
        int xpAmount = plugin.getConfigManager().getEventInt(id, "xp-amount", 3);
        int dropRadius = plugin.getConfigManager().getEventInt(id, "drop-radius", 8);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        for (int i = 0; i < orbsPerPlayer; i++) {
                            double ox = (random.nextDouble() - 0.5) * dropRadius * 2;
                            double oz = (random.nextDouble() - 0.5) * dropRadius * 2;
                            Location dropLoc = player.getLocation().add(ox, 8 + random.nextDouble() * 5, oz);

                            // Pre-spawn sparkle
                            world.spawnParticle(Particle.ENCHANTED_HIT, dropLoc, 6, 0.3, 0.3, 0.3, 0.05);

                            // Spawn XP orb
                            world.spawn(dropLoc, ExperienceOrb.class, orb -> orb.setExperience(xpAmount));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
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
