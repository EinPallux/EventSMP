package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

public class BloodMoonEvent extends SmpEvent implements Listener {

    // Hostile mob types
    private static final Set<EntityType> HOSTILE = EnumSet.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER,
            EntityType.CREEPER, EntityType.WITCH, EntityType.ENDERMAN, EntityType.SLIME,
            EntityType.PHANTOM, EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY,
            EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
            EntityType.VEX, EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.GHAST,
            EntityType.MAGMA_CUBE, EntityType.ZOMBIE_VILLAGER, EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN, EntityType.SHULKER, EntityType.SILVERFISH,
            EntityType.ENDERMITE, EntityType.WARDEN, EntityType.BREEZE
    );

    public BloodMoonEvent(EventSMP plugin) {
        super(plugin, "blood_moon");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Set time to night
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL && !isWorldDisabled(world)) {
                if (world.getTime() < 12542 || world.getTime() > 23459) {
                    world.setTime(13000); // nighttime
                }
            }
        }

        // Particle task
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Entity entity : world.getEntities()) {
                        if (HOSTILE.contains(entity.getType())) {
                            Location loc = entity.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.DUST,
                                    loc, 6,
                                    0.3, 0.5, 0.3, 0.0,
                                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!active) return;
        Entity damager = event.getDamager();
        if (!HOSTILE.contains(damager.getType())) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (isWorldDisabled(damager.getWorld())) return;

        double multiplier = plugin.getConfigManager().getEventDouble(id, "damage-multiplier", 1.5);
        event.setDamage(event.getDamage() * multiplier);
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
    }
}
