package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class LuckyDayEvent extends SmpEvent implements Listener {

    private final Random random = new Random();

    // Positive effects to randomly grant
    private static final List<PotionEffectType> LUCKY_EFFECTS = List.of(
            PotionEffectType.STRENGTH,
            PotionEffectType.REGENERATION,
            PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.HASTE,
            PotionEffectType.LUCK,
            PotionEffectType.ABSORPTION
    );

    public LuckyDayEvent(EventSMP plugin) {
        super(plugin, "lucky_day");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Happy villager particle tick around players
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Player player : world.getPlayers()) {
                        Location loc = player.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.5, 0.5, 0.5, 0.0);
                    }
                }
            }
        }.runTaskTimer(plugin, 15L, 15L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!active) return;
        if (isWorldDisabled(event.getBlock().getWorld())) return;
        double chance = plugin.getConfigManager().getEventDouble(id, "mine-bonus-chance", 0.25);
        if (random.nextDouble() >= chance) return;

        // Drop a random extra item from the block
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        var drops = event.getBlock().getDrops(tool, event.getPlayer());
        if (drops.isEmpty()) return;
        ItemStack bonus = drops.iterator().next().clone();
        bonus.setAmount(1);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), bonus);

        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.3, 0.3, 0.3, 0.0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!active) return;
        if (event.getEntity() instanceof Player) return;
        if (event.getEntity().getKiller() == null) return;
        if (isWorldDisabled(event.getEntity().getWorld())) return;

        double chance = plugin.getConfigManager().getEventDouble(id, "mob-loot-chance", 0.30);
        if (random.nextDouble() >= chance) return;

        // Duplicate one random existing drop
        if (!event.getDrops().isEmpty()) {
            ItemStack bonus = event.getDrops().get(random.nextInt(event.getDrops().size())).clone();
            bonus.setAmount(1);
            event.getDrops().add(bonus);
        }

        Location loc = event.getEntity().getLocation().add(0, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.3, 0.5, 0.3, 0.0);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!active) return;
        if (isWorldDisabled(event.getEnchanter().getWorld())) return;

        double chance = plugin.getConfigManager().getEventDouble(id, "enchant-effect-chance", 0.40);
        if (random.nextDouble() >= chance) return;

        int minTicks = plugin.getConfigManager().getEventInt(id, "enchant-effect-min-ticks", 600);
        int maxTicks = plugin.getConfigManager().getEventInt(id, "enchant-effect-max-ticks", 1200);
        int duration = minTicks + random.nextInt(maxTicks - minTicks + 1);

        PotionEffectType effectType = LUCKY_EFFECTS.get(random.nextInt(LUCKY_EFFECTS.size()));
        event.getEnchanter().addPotionEffect(new PotionEffect(effectType, duration, 0, false, true));
        event.getEnchanter().sendActionBar(
                net.kyori.adventure.text.Component.text("§e⭐ Lucky enchant! You feel " + effectType.getKey().getKey() + "!"));
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
