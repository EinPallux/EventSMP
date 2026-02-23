package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BunnyHopEvent extends SmpEvent implements Listener {

    /** Tracks whether a player was in the air last tick (for landing detection). */
    private final Map<UUID, Boolean> wasInAir = new HashMap<>();

    public BunnyHopEvent(EventSMP plugin) {
        super(plugin, "bunny_hop");
    }

    @Override
    public void onStart() {
        active = true;
        int amplifier = plugin.getConfigManager().getEventInt(id, "jump-amplifier", 1);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Apply Jump Boost to all current players and maintain it
        for (World world : Bukkit.getWorlds()) {
            if (isWorldDisabled(world)) continue;
            for (Player player : world.getPlayers()) {
                applyJump(player, amplifier);
            }
        }

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        if (!player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                            applyJump(player, amplifier);
                        }

                        // Detect landing: was in air, now on ground
                        boolean inAir = !player.isOnGround();
                        boolean prev = wasInAir.getOrDefault(player.getUniqueId(), false);
                        if (prev && !inAir) {
                            // Player just landed
                            Location loc = player.getLocation();
                            world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.1, 0),
                                    20, 0.5, 0.1, 0.5, 0.04);
                            world.spawnParticle(Particle.WHITE_ASH, loc.clone().add(0, 0.2, 0),
                                    12, 0.4, 0.2, 0.4, 0.05);
                            world.playSound(loc, Sound.BLOCK_WOOL_STEP, 0.6f, 1.3f);
                        }
                        wasInAir.put(player.getUniqueId(), inAir);
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void applyJump(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, amplifier, false, false));
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
        wasInAir.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            }
        }
    }
}
