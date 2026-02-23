package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

public class MeteorShowerEvent extends SmpEvent {

    private final Random random = new Random();

    public MeteorShowerEvent(EventSMP plugin) {
        super(plugin, "meteor_shower");
    }

    @Override
    public void onStart() {
        active = true;
        int intervalTicks = plugin.getConfigManager().getEventInt(id, "meteor-interval-ticks", 40);
        int meteorsPerWave = plugin.getConfigManager().getEventInt(id, "meteors-per-wave", 3);
        double damage = plugin.getConfigManager().getEventDouble(id, "meteor-damage", 4.0);
        int fireTicks = plugin.getConfigManager().getEventInt(id, "meteor-fire-ticks", 40);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    Collection<? extends Player> players = world.getPlayers();
                    if (players.isEmpty()) continue;
                    for (int i = 0; i < meteorsPerWave; i++) {
                        // Pick a random online player as center
                        Player[] arr = players.toArray(new Player[0]);
                        Player target = arr[random.nextInt(arr.length)];
                        spawnMeteor(target, damage, fireTicks);
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void spawnMeteor(Player nearPlayer, double damage, int fireTicks) {
        Location center = nearPlayer.getLocation();
        // Random offset from player, -20 to 20 blocks
        double offsetX = (random.nextDouble() - 0.5) * 40;
        double offsetZ = (random.nextDouble() - 0.5) * 40;

        int highY = center.getWorld().getMaxHeight() - 10;
        Location spawnLoc = new Location(center.getWorld(),
                center.getX() + offsetX, highY, center.getZ() + offsetZ);

        // Meteor direction: straight down with slight randomness
        Vector velocity = new Vector((random.nextDouble() - 0.5) * 0.2, -3.5, (random.nextDouble() - 0.5) * 0.2);

        // Spawn a fireball entity that looks like a meteor
        org.bukkit.entity.Fireball fireball = center.getWorld().spawn(spawnLoc, org.bukkit.entity.Fireball.class, fb -> {
            fb.setVelocity(velocity);
            fb.setDirection(velocity);
            fb.setYield(0f);          // no block damage
            fb.setIsIncendiary(false);
        });

        // Schedule impact check: track the fireball and apply damage if near a player
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (!active || fireball.isDead() || ticks > 200) {
                    if (!fireball.isDead()) fireball.remove();
                    cancel();
                    return;
                }
                Location fbLoc = fireball.getLocation();
                // Check block below
                if (!fbLoc.getWorld().getBlockAt(fbLoc.add(0, -1, 0)).getType().isAir()
                        || fbLoc.getY() <= fbLoc.getWorld().getMinHeight()) {
                    impactEffect(fireball.getLocation());
                    fireball.remove();
                    cancel();
                    return;
                }
                // Particle trail
                fbLoc.getWorld().spawnParticle(Particle.FLAME, fbLoc, 5, 0.1, 0.1, 0.1, 0.02);
                fbLoc.getWorld().spawnParticle(Particle.SMOKE, fbLoc, 3, 0.1, 0.1, 0.1, 0.01);

                // Damage players in range
                for (Player p : fbLoc.getWorld().getNearbyPlayers(fbLoc, 1.5)) {
                    p.damage(damage);
                    p.setFireTicks(fireTicks);
                    impactEffect(fbLoc);
                    fireball.remove();
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void impactEffect(Location loc) {
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 60, 0.5, 0.5, 0.5, 0.2);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.2, 0.2, 0.2, 0.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
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
