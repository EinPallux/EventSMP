package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TornadoTroubleEvent extends SmpEvent {

    private final Random random = new Random();
    private final Map<UUID, Long> nextPushTime = new HashMap<>();

    public TornadoTroubleEvent(EventSMP plugin) {
        super(plugin, "tornado_trouble");
    }

    @Override
    public void onStart() {
        active = true;
        int pushMin = plugin.getConfigManager().getEventInt(id, "push-interval-min", 40);
        int pushMax = plugin.getConfigManager().getEventInt(id, "push-interval-max", 100);
        double pushMin2 = plugin.getConfigManager().getEventDouble(id, "push-strength-min", 1.0);
        double pushMax2 = plugin.getConfigManager().getEventDouble(id, "push-strength-max", 3.0);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                long now = System.currentTimeMillis();
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        long nextTime = nextPushTime.getOrDefault(player.getUniqueId(), 0L);
                        if (now >= nextTime) {
                            pushPlayer(player, pushMin2, pushMax2);
                            long delay = (pushMin + random.nextInt(pushMax - pushMin)) * 50L;
                            nextPushTime.put(player.getUniqueId(), now + delay);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void pushPlayer(Player player, double min, double max) {
        Location loc = player.getLocation();
        double strength = min + random.nextDouble() * (max - min);
        double angle = random.nextDouble() * Math.PI * 2;
        double vx = Math.cos(angle) * strength * 0.4;
        double vz = Math.sin(angle) * strength * 0.4;
        double vy = 0.15;

        player.setVelocity(new Vector(vx, vy, vz));

        // Windy particles
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, 20, 0.8, 0.8, 0.8, 0.1);
        loc.getWorld().playSound(loc, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.2f);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        nextPushTime.clear();
        cancelTickTask();
    }
}
