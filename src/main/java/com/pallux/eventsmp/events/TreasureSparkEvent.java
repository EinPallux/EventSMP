package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TreasureSparkEvent extends SmpEvent implements Listener {

    private final Random random = new Random();
    /** Set of block locations currently "sparkling" with treasure. */
    private final Set<Location> sparkleBlocks = Collections.synchronizedSet(new HashSet<>());

    // Solid, common surface blocks eligible to become treasure blocks
    private static final Set<Material> ELIGIBLE = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DIRT, Material.GRASS_BLOCK,
            Material.SAND, Material.GRAVEL, Material.ANDESITE, Material.DIORITE,
            Material.GRANITE, Material.DEEPSLATE, Material.TUFF, Material.NETHERRACK,
            Material.END_STONE
    );

    // Possible loot
    private static final Material[] LOOT_TABLE = {
            Material.COAL,       Material.COAL,       Material.COAL,
            Material.IRON_INGOT, Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.EMERALD,
            Material.DIAMOND
    };

    public TreasureSparkEvent(EventSMP plugin) {
        super(plugin, "treasure_spark");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int spawnInterval  = plugin.getConfigManager().getEventInt(id, "sparkle-interval-ticks", 40);
        int maxSparkBlocks = plugin.getConfigManager().getEventInt(id, "max-sparkle-blocks", 5);
        int sparkleRadius  = plugin.getConfigManager().getEventInt(id, "sparkle-radius", 12);
        int sparkleLifetime = plugin.getConfigManager().getEventInt(id, "sparkle-lifetime-ticks", 200);

        tickTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                tick++;

                // Particle effect on existing sparkle blocks
                synchronized (sparkleBlocks) {
                    for (Location loc : sparkleBlocks) {
                        loc.getWorld().spawnParticle(Particle.END_ROD,
                                loc.clone().add(0.5, 0.5, 0.5), 3,
                                0.3, 0.3, 0.3, 0.02);
                        loc.getWorld().spawnParticle(Particle.CRIT,
                                loc.clone().add(0.5, 1.0, 0.5), 2,
                                0.2, 0.2, 0.2, 0.05);
                    }
                }

                // Occasionally register new sparkle blocks
                if (tick % spawnInterval == 0) {
                    for (World world : Bukkit.getWorlds()) {
                        if (isWorldDisabled(world)) continue;
                        for (Player player : world.getPlayers()) {
                            synchronized (sparkleBlocks) {
                                if (sparkleBlocks.size() >= maxSparkBlocks) break;
                            }
                            // Find a random eligible block near the player
                            for (int attempt = 0; attempt < 10; attempt++) {
                                int dx = (random.nextInt(sparkleRadius * 2 + 1)) - sparkleRadius;
                                int dz = (random.nextInt(sparkleRadius * 2 + 1)) - sparkleRadius;
                                int dy = random.nextInt(5) - 2;
                                Block block = world.getBlockAt(
                                        player.getLocation().getBlockX() + dx,
                                        player.getLocation().getBlockY() + dy,
                                        player.getLocation().getBlockZ() + dz);
                                if (!ELIGIBLE.contains(block.getType())) continue;
                                Location key = block.getLocation();
                                synchronized (sparkleBlocks) {
                                    if (!sparkleBlocks.contains(key)) {
                                        sparkleBlocks.add(key);
                                        // Auto-remove after lifetime
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                sparkleBlocks.remove(key);
                                            }
                                        }.runTaskLater(plugin, sparkleLifetime);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!active) return;
        if (isWorldDisabled(event.getBlock().getWorld())) return;

        Location loc = event.getBlock().getLocation();
        boolean wasSparkling;
        synchronized (sparkleBlocks) {
            wasSparkling = sparkleBlocks.remove(loc);
        }
        if (!wasSparkling) return;

        // Drop random loot
        Material loot = LOOT_TABLE[random.nextInt(LOOT_TABLE.length)];
        loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), new ItemStack(loot, 1));

        // Fancy reward effect
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 0.5, 0.5),
                25, 0.4, 0.4, 0.4, 0.08);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0.5, 0.5, 0.5),
                15, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
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
        synchronized (sparkleBlocks) {
            sparkleBlocks.clear();
        }
    }
}
