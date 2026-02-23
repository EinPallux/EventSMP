package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OreFrenzyEvent extends SmpEvent implements Listener {

    private static final java.util.Set<Material> ORE_MATERIALS = java.util.EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    public OreFrenzyEvent(EventSMP plugin) {
        super(plugin, "ore_frenzy");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!active) return;
        Block block = event.getBlock();
        if (!ORE_MATERIALS.contains(block.getType())) return;

        Player player = event.getPlayer();
        World world = block.getWorld();

        if (plugin.getConfigManager().getConfig()
                .getStringList("disabled-worlds").contains(world.getName())) return;

        int multiplier = plugin.getConfigManager().getEventInt(id, "drop-multiplier", 2);

        // Particles at mining site
        world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
        world.spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.1);

        // Get normal drops
        ItemStack tool = player.getInventory().getItemInMainHand();
        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        Collection<ItemStack> drops = block.getDrops(tool, player);

        // Cancel normal drop, handle manually
        event.setDropItems(false);

        for (ItemStack drop : drops) {
            int baseAmount = drop.getAmount();
            // Apply multiplier first
            int newAmount = baseAmount * multiplier;
            // Apply Fortune bonus on top (simplified: fortune adds extra chance)
            if (fortune > 0) {
                newAmount += (int)(newAmount * (fortune * 0.33));
            }
            drop.setAmount(newAmount);
            world.dropItemNaturally(block.getLocation(), drop);
        }

        // Drop XP naturally
        if (event.getExpToDrop() > 0) {
            block.getWorld().spawn(block.getLocation(), org.bukkit.entity.ExperienceOrb.class,
                    orb -> orb.setExperience(event.getExpToDrop()));
            event.setExpToDrop(0);
        }
    }

    @Override
    public void onEnd() {
        active = false;
        HandlerList.unregisterAll(this);
        cancelTickTask();
    }
}
