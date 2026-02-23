package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class GoldenTouchEvent extends SmpEvent implements Listener {

    private final Random random = new Random();

    public GoldenTouchEvent(EventSMP plugin) {
        super(plugin, "golden_touch");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Particle tick for all living entities
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (entity instanceof Player) continue;
                        Location loc = entity.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.DUST, loc, 4, 0.3, 0.4, 0.3, 0.0,
                                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));
                    }
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!active) return;
        if (event.getEntity() instanceof Player) return;
        if (isWorldDisabled(event.getEntity().getWorld())) return;
        if (event.getEntity().getKiller() == null) return;

        double nuggetChance = plugin.getConfigManager().getEventDouble(id, "gold-nugget-chance", 0.5);
        double ingotChance  = plugin.getConfigManager().getEventDouble(id, "gold-ingot-chance", 0.15);
        int nuggetMin = plugin.getConfigManager().getEventInt(id, "nugget-min", 1);
        int nuggetMax = plugin.getConfigManager().getEventInt(id, "nugget-max", 4);
        int ingotMin  = plugin.getConfigManager().getEventInt(id, "ingot-min", 1);
        int ingotMax  = plugin.getConfigManager().getEventInt(id, "ingot-max", 2);

        Location loc = event.getEntity().getLocation();

        if (random.nextDouble() < nuggetChance) {
            int amount = nuggetMin + random.nextInt(nuggetMax - nuggetMin + 1);
            event.getDrops().add(new ItemStack(Material.GOLD_NUGGET, amount));
            loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.0,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        }
        if (random.nextDouble() < ingotChance) {
            int amount = ingotMin + random.nextInt(ingotMax - ingotMin + 1);
            event.getDrops().add(new ItemStack(Material.GOLD_INGOT, amount));
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
