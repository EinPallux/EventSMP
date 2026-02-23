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

public class EchoingDarknessEvent extends SmpEvent {

    private final Random random = new Random();
    private final Map<UUID, Long> nextDarkness = new HashMap<>();
    private final Map<UUID, Long> nextSound    = new HashMap<>();

    public EchoingDarknessEvent(EventSMP plugin) {
        super(plugin, "echoing_darkness");
    }

    @Override
    public void onStart() {
        active = true;

        int darkMin  = plugin.getConfigManager().getEventInt(id, "darkness-interval-min", 200);
        int darkMax  = plugin.getConfigManager().getEventInt(id, "darkness-interval-max", 900);
        int darkDur  = plugin.getConfigManager().getEventInt(id, "darkness-duration-ticks", 60);
        int soundMin = plugin.getConfigManager().getEventInt(id, "sound-interval-min", 200);
        int soundMax = plugin.getConfigManager().getEventInt(id, "sound-interval-max", 900);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                long now = System.currentTimeMillis();
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        UUID uuid = player.getUniqueId();

                        // Darkness pulse
                        long nextD = nextDarkness.getOrDefault(uuid, 0L);
                        if (now >= nextD) {
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.DARKNESS, darkDur, 0, false, false));
                            player.playSound(player.getLocation(),
                                    Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
                            long delay = (darkMin + random.nextInt(darkMax - darkMin)) * 50L;
                            nextDarkness.put(uuid, now + delay);
                        }

                        // Deep echoing sound
                        long nextS = nextSound.getOrDefault(uuid, 0L);
                        if (now >= nextS) {
                            playEchoSound(player);
                            long delay = (soundMin + random.nextInt(soundMax - soundMin)) * 50L;
                            nextSound.put(uuid, now + delay);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void playEchoSound(Player player) {
        Sound[] sounds = {
                Sound.ENTITY_WARDEN_AMBIENT,
                Sound.ENTITY_WARDEN_LISTENING,
                Sound.ENTITY_WARDEN_SONIC_BOOM,
                Sound.BLOCK_SCULK_SENSOR_CLICKING,
                Sound.BLOCK_SCULK_SHRIEKER_SHRIEK
        };
        Sound chosen = sounds[random.nextInt(sounds.length)];
        float pitch = 0.5f + (float)(random.nextDouble() * 0.4);
        player.playSound(player.getLocation(), chosen, 0.7f, pitch);

        // A few dark particles
        Location loc = player.getLocation().add(
                (random.nextDouble() - 0.5) * 4,
                random.nextDouble() * 2,
                (random.nextDouble() - 0.5) * 4);
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 3, 0.2, 0.2, 0.2, 0.01);
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        cancelTickTask();
        nextDarkness.clear();
        nextSound.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                player.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }
    }
}
