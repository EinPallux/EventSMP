package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SlimeInvasionEvent extends SmpEvent implements Listener {

    private final Random random = new Random();

    public SlimeInvasionEvent(EventSMP plugin) {
        super(plugin, "slime_invasion");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int spawnInterval = plugin.getConfigManager().getEventInt(id, "spawn-interval-ticks", 100);
        int spawnRadius   = plugin.getConfigManager().getEventInt(id, "spawn-radius", 20);
        int maxSlimes     = plugin.getConfigManager().getEventInt(id, "max-slimes-per-player", 3);

        // Periodically spawn slimes near players
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (org.bukkit.entity.Player player : world.getPlayers()) {
                        // Count nearby slimes to avoid overcrowding
                        long nearbySlimes = player.getNearbyEntities(spawnRadius, spawnRadius, spawnRadius)
                                .stream().filter(e -> e instanceof Slime).count();
                        if (nearbySlimes >= maxSlimes) continue;

                        // Spawn 1 slime at a valid surface location near the player
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist  = 8 + random.nextDouble() * (spawnRadius - 8);
                        Location spawnLoc = player.getLocation().clone().add(
                                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc));

                        int minSize = plugin.getConfigManager().getEventInt(id, "slime-size-min", 2);
                        int maxSize = plugin.getConfigManager().getEventInt(id, "slime-size-max", 4);
                        int size = minSize + random.nextInt(maxSize - minSize + 1);

                        world.spawn(spawnLoc, Slime.class, slime -> {
                            slime.setSize(size);
                            slime.getWorld().spawnParticle(Particle.ITEM_SLIME,
                                    slime.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.05);
                        });
                    }
                }
            }
        }.runTaskTimer(plugin, spawnInterval, spawnInterval);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof Slime)) return;
        if (isWorldDisabled(event.getEntity().getWorld())) return;

        // Boost naturally spawning slimes too
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            int minSize = plugin.getConfigManager().getEventInt(id, "slime-size-min", 2);
            int maxSize = plugin.getConfigManager().getEventInt(id, "slime-size-max", 4);
            Slime slime = (Slime) event.getEntity();
            // Schedule size boost next tick so spawn isn't cancelled
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active) return;
                    int size = minSize + random.nextInt(maxSize - minSize + 1);
                    slime.setSize(size);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSlimeSplit(SlimeSplitEvent event) {
        if (!active) return;
        if (isWorldDisabled(event.getEntity().getWorld())) return;
        // Allow splitting but limit split count to avoid lag
        if (event.getEntity().getSize() <= 1) {
            event.setCancelled(true);
        }
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