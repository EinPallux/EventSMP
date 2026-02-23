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

public class ToxicWastelandEvent extends SmpEvent {

    private final Random random = new Random();
    private final Map<UUID, Long> nextPoison = new HashMap<>();

    public ToxicWastelandEvent(EventSMP plugin) {
        super(plugin, "toxic_wasteland");
    }

    @Override
    public void onStart() {
        active = true;
        int poisonMin    = plugin.getConfigManager().getEventInt(id, "poison-interval-min", 100);
        int poisonMax    = plugin.getConfigManager().getEventInt(id, "poison-interval-max", 200);
        int poisonTicks  = plugin.getConfigManager().getEventInt(id, "poison-duration-ticks", 40);

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                long now = System.currentTimeMillis();
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        UUID uuid = player.getUniqueId();

                        // Poison proc
                        long nextTime = nextPoison.getOrDefault(uuid, 0L);
                        if (now >= nextTime) {
                            player.addPotionEffect(new PotionEffect(
                                    PotionEffectType.POISON, poisonTicks, 0, false, true));
                            int delay = poisonMin + random.nextInt(poisonMax - poisonMin);
                            nextPoison.put(uuid, now + delay * 50L);
                        }

                        // Ambient toxic particles floating around player
                        Location base = player.getLocation();
                        for (int i = 0; i < 3; i++) {
                            Location pLoc = base.clone().add(
                                    (random.nextDouble() - 0.5) * 3,
                                    random.nextDouble() * 2.5,
                                    (random.nextDouble() - 0.5) * 3);
                            world.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(50, 180, 50), 1.2f));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private boolean isWorldDisabled(World world) {
        return plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName());
    }

    @Override
    public void onEnd() {
        active = false;
        cancelTickTask();
        nextPoison.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                player.removePotionEffect(PotionEffectType.POISON);
            }
        }
    }
}
