package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CreeperPanicEvent extends SmpEvent implements Listener {

    public CreeperPanicEvent(EventSMP plugin) {
        super(plugin, "creeper_panic");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int speedAmp = plugin.getConfigManager().getEventInt(id, "speed-amplifier", 2);
        int fuseTicks = plugin.getConfigManager().getEventInt(id, "fuse-ticks", 20);

        // Apply effects to existing creepers
        for (World world : Bukkit.getWorlds()) {
            if (isWorldDisabled(world)) continue;
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Creeper creeper) {
                    applyToCreeper(creeper, speedAmp, fuseTicks);
                }
            }
        }

        // Particle tick for creepers
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Creeper creeper) {
                            Location loc = creeper.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 3,
                                    0.2, 0.3, 0.2, 0.02,
                                    null);
                            // Green sparks using dust
                            world.spawnParticle(Particle.DUST, loc, 4, 0.2, 0.3, 0.2, 0.0,
                                    new Particle.DustOptions(Color.fromRGB(0, 200, 0), 1.0f));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (isWorldDisabled(creeper.getWorld())) return;

        int speedAmp = plugin.getConfigManager().getEventInt(id, "speed-amplifier", 2);
        int fuseTicks = plugin.getConfigManager().getEventInt(id, "fuse-ticks", 20);
        applyToCreeper(creeper, speedAmp, fuseTicks);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof Creeper)) return;
        if (isWorldDisabled(event.getEntity().getWorld())) return;

        float power = (float) plugin.getConfigManager().getEventDouble(id, "explosion-power", 1.5);
        event.setRadius(power);
    }

    private void applyToCreeper(Creeper creeper, int speedAmp, int fuseTicks) {
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmp, false, false));
        creeper.setMaxFuseTicks(fuseTicks);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        HandlerList.unregisterAll(this);
        cancelTickTask();
        // Remove speed from all creepers
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Creeper creeper) {
                    creeper.removePotionEffect(PotionEffectType.SPEED);
                    creeper.setMaxFuseTicks(30); // reset to default
                }
            }
        }
    }
}
