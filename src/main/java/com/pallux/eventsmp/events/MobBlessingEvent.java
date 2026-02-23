package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

public class MobBlessingEvent extends SmpEvent {

    private static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
            EntityType.RABBIT, EntityType.CAT, EntityType.WOLF, EntityType.FOX,
            EntityType.MOOSHROOM, EntityType.GOAT, EntityType.AXOLOTL, EntityType.FROG,
            EntityType.TURTLE, EntityType.SNIFFER, EntityType.CAMEL, EntityType.ALLAY
    );

    public MobBlessingEvent(EventSMP plugin) {
        super(plugin, "mob_blessing");
    }

    @Override
    public void onStart() {
        active = true;
        int healInterval = plugin.getConfigManager().getEventInt(id, "heal-interval-ticks", 100);
        double healAmount = plugin.getConfigManager().getEventDouble(id, "heal-amount", 1.0);
        double healRadius = plugin.getConfigManager().getEventDouble(id, "heal-radius", 8.0);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Entity entity : world.getEntities()) {
                        if (!PASSIVE_MOBS.contains(entity.getType())) continue;
                        LivingEntity mob = (LivingEntity) entity;

                        // Green sparkle around the mob
                        Location mobLoc = mob.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, mobLoc, 4,
                                0.4, 0.4, 0.4, 0.0);
                        world.spawnParticle(Particle.DUST, mobLoc, 3, 0.3, 0.5, 0.3, 0.0,
                                new Particle.DustOptions(Color.fromRGB(80, 220, 80), 1.0f));

                        // Heal nearby players
                        for (Player player : world.getNearbyPlayers(mob.getLocation(), healRadius)) {
                            var maxHpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            if (maxHpAttr == null) continue;
                            double maxHp = maxHpAttr.getValue();
                            if (player.getHealth() < maxHp) {
                                double newHp = Math.min(player.getHealth() + healAmount, maxHp);
                                player.setHealth(newHp);
                                // Small heart particle on the player
                                player.getWorld().spawnParticle(Particle.HEART,
                                        player.getLocation().add(0, 2, 0),
                                        1, 0.2, 0.2, 0.2, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, healInterval, healInterval);
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