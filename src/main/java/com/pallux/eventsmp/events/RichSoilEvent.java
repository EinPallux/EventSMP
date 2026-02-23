package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RichSoilEvent extends SmpEvent implements Listener {

    private static final java.util.Set<Material> CROP_MATERIALS = java.util.EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.MELON_STEM, Material.PUMPKIN_STEM, Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP
    );

    private final Random random = new Random();

    public RichSoilEvent(EventSMP plugin) {
        super(plugin, "rich_soil");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int particleInterval = plugin.getConfigManager().getEventInt(id, "particle-interval-ticks", 60);

        // Bone meal particles on random farmland/crops
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    // Collect nearby crop blocks around players and sprinkle particles
                    for (org.bukkit.entity.Player player : world.getPlayers()) {
                        Location loc = player.getLocation();
                        List<Block> cropBlocks = new ArrayList<>();
                        for (int dx = -6; dx <= 6; dx++) {
                            for (int dz = -6; dz <= 6; dz++) {
                                Block b = world.getBlockAt(
                                        loc.getBlockX() + dx,
                                        loc.getBlockY(),
                                        loc.getBlockZ() + dz);
                                if (CROP_MATERIALS.contains(b.getType()) ||
                                        b.getType() == Material.FARMLAND) {
                                    cropBlocks.add(b);
                                }
                            }
                        }
                        // Sprinkle on a random few
                        int count = Math.min(cropBlocks.size(), 4);
                        for (int i = 0; i < count; i++) {
                            Block pick = cropBlocks.get(random.nextInt(cropBlocks.size()));
                            Location pLoc = pick.getLocation().add(0.5, 1.0, 0.5);
                            world.spawnParticle(Particle.HAPPY_VILLAGER, pLoc, 5,
                                    0.3, 0.3, 0.3, 0.0);
                            world.spawnParticle(Particle.COMPOSTER, pLoc, 8,
                                    0.3, 0.3, 0.3, 0.0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, particleInterval, particleInterval);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (!active) return;
        Block block = event.getBlock();
        if (!CROP_MATERIALS.contains(block.getType())) return;
        if (isWorldDisabled(block.getWorld())) return;

        double extraGrowChance = plugin.getConfigManager().getEventDouble(id, "extra-grow-chance", 1.0);
        if (random.nextDouble() < extraGrowChance) {
            // The event already handles the first grow tick. We schedule an additional
            // bone-meal-like grow one tick later for the 2x effect.
            Block b = block;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active) return;
                    if (b.getBlockData() instanceof Ageable ageable) {
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(ageable.getAge() + 1);
                            b.setBlockData(ageable);
                            b.getWorld().spawnParticle(Particle.COMPOSTER,
                                    b.getLocation().add(0.5, 0.5, 0.5),
                                    6, 0.3, 0.3, 0.3, 0.0);
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
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
