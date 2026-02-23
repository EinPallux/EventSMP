package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HealingAuraEvent extends SmpEvent {

    public HealingAuraEvent(EventSMP plugin) {
        super(plugin, "healing_aura");
    }

    @Override
    public void onStart() {
        active = true;
        int intervalTicks = plugin.getConfigManager().getEventInt(id, "heal-interval-ticks", 300);
        double healAmount  = plugin.getConfigManager().getEventDouble(id, "heal-amount", 2.0);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        double newHp = Math.min(player.getHealth() + healAmount,
                                player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                        player.setHealth(newHp);

                        // Green heart particles
                        Location loc = player.getLocation().add(0, 1.5, 0);
                        world.spawnParticle(Particle.HEART, loc, 6, 0.4, 0.4, 0.4, 0.0);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, 0.4, 0.4, 0.4, 0.0);
                        player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
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