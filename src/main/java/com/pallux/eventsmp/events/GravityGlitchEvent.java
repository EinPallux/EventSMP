package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GravityGlitchEvent extends SmpEvent {

    private final Random random = new Random();
    private final Map<UUID, Long> nextChange = new HashMap<>();

    public GravityGlitchEvent(EventSMP plugin) {
        super(plugin, "gravity_glitch");
    }

    @Override
    public void onStart() {
        active = true;
        int minInterval = plugin.getConfigManager().getEventInt(id, "change-interval-min", 400);
        int maxInterval = plugin.getConfigManager().getEventInt(id, "change-interval-max", 800);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                long now = System.currentTimeMillis();
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        long next = nextChange.getOrDefault(player.getUniqueId(), 0L);
                        if (now >= next) {
                            applyGravityChange(player);
                            int delay = minInterval + random.nextInt(maxInterval - minInterval);
                            nextChange.put(player.getUniqueId(), now + delay * 50L);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void applyGravityChange(Player player) {
        boolean lowGravity = random.nextBoolean();
        Location loc = player.getLocation();

        // Remove existing gravity effects
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.SLOWNESS);

        if (lowGravity) {
            // Low gravity: slow falling + jump boost
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 2, false, false));
            player.sendActionBar(net.kyori.adventure.text.Component.text("§d⬆ Low Gravity!"));
        } else {
            // Heavy gravity: slowness
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 1, false, false));
            player.sendActionBar(net.kyori.adventure.text.Component.text("§5⬇ Heavy Gravity!"));
        }

        // Particles
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, lowGravity ? 1.5f : 0.5f);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        // Remove effects from all players
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
        nextChange.clear();
        cancelTickTask();
    }
}
