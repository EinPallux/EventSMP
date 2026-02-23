package com.pallux.eventsmp.events;

import com.pallux.eventsmp.EventSMP;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmeraldFeverEvent extends SmpEvent implements Listener {

    /**
     * We store modified villager UUIDs so we can restore on end.
     * Map: villager UUID -> original recipes
     */
    private final Map<java.util.UUID, List<MerchantRecipe>> originalRecipes = new HashMap<>();

    public EmeraldFeverEvent(EventSMP plugin) {
        super(plugin, "emerald_fever");
    }

    @Override
    public void onStart() {
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Particle tick around villagers
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) { cancel(); return; }
                for (World world : Bukkit.getWorlds()) {
                    if (isWorldDisabled(world)) continue;
                    for (Entity entity : world.getEntities()) {
                        if (!(entity instanceof Villager)) continue;
                        Location loc = entity.getLocation().add(0, 1.2, 0);
                        world.spawnParticle(Particle.DUST, loc, 4, 0.3, 0.4, 0.3, 0.0,
                                new Particle.DustOptions(Color.fromRGB(80, 230, 80), 1.2f));
                        world.spawnParticle(Particle.CRIT, loc, 2, 0.2, 0.3, 0.2, 0.05);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!active) return;
        if (!(event.getInventory() instanceof MerchantInventory merchantInv)) return;
        if (!(merchantInv.getMerchant() instanceof Villager villager)) return;
        if (isWorldDisabled(villager.getWorld())) return;

        java.util.UUID uuid = villager.getUniqueId();

        // Store originals only once
        if (!originalRecipes.containsKey(uuid)) {
            List<MerchantRecipe> originals = new ArrayList<>(villager.getRecipes());
            originalRecipes.put(uuid, originals);
        }

        double discount = plugin.getConfigManager().getEventDouble(id, "price-discount", 0.5);

        List<MerchantRecipe> discounted = new ArrayList<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            MerchantRecipe newRecipe = new MerchantRecipe(
                    recipe.getResult(),
                    recipe.getUses(),
                    recipe.getMaxUses(),
                    recipe.hasExperienceReward(),
                    recipe.getVillagerExperience(),
                    recipe.getPriceMultiplier()
            );
            // Copy ingredients with reduced cost
            List<org.bukkit.inventory.ItemStack> ingredients = recipe.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                org.bukkit.inventory.ItemStack ing = ingredients.get(i).clone();
                int reduced = Math.max(1, (int) Math.ceil(ing.getAmount() * discount));
                ing.setAmount(reduced);
                if (i == 0) newRecipe.addIngredient(ing);
                else newRecipe.addIngredient(ing);
            }
            discounted.add(newRecipe);
        }
        villager.setRecipes(discounted);
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

        // Restore original recipes
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Villager villager)) continue;
                List<MerchantRecipe> orig = originalRecipes.get(villager.getUniqueId());
                if (orig != null) {
                    villager.setRecipes(orig);
                }
            }
        }
        originalRecipes.clear();
    }
}
