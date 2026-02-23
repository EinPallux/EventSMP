package com.pallux.eventsmp.gui;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.events.SmpEvent;
import com.pallux.eventsmp.managers.EventManager;
import com.pallux.eventsmp.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EventsGui implements Listener {

    private final EventSMP plugin;
    private final Inventory inventory;
    private final Player viewer;

    private static final Map<String, Material> EVENT_ICONS = new HashMap<>();
    static {
        EVENT_ICONS.put("meteor_shower",   Material.FIRE_CHARGE);
        EVENT_ICONS.put("ore_frenzy",      Material.DIAMOND);
        EVENT_ICONS.put("tornado_trouble", Material.FEATHER);
        EVENT_ICONS.put("blood_moon",      Material.REDSTONE);
        EVENT_ICONS.put("gravity_glitch",  Material.END_CRYSTAL);
        EVENT_ICONS.put("lightning_storm", Material.LIGHTNING_ROD);
        EVENT_ICONS.put("speed_surge",     Material.SUGAR);
        EVENT_ICONS.put("freezing_winds",  Material.SNOWBALL);
        EVENT_ICONS.put("creeper_panic",   Material.CREEPER_HEAD);
        EVENT_ICONS.put("xp_rain",         Material.EXPERIENCE_BOTTLE);
        EVENT_ICONS.put("golden_touch",    Material.GOLD_INGOT);
        EVENT_ICONS.put("healing_aura",    Material.GLISTERING_MELON_SLICE);
        EVENT_ICONS.put("toxic_wasteland", Material.SLIME_BALL);
        EVENT_ICONS.put("lucky_day",       Material.EMERALD);
        EVENT_ICONS.put("earthquake",      Material.STONE);
    }

    // 5 rows of usable slots (avoiding border), enough for 15 events
    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public EventsGui(EventSMP plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        String title = plugin.getConfigManager().getMessage("gui-title");
        this.inventory = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));
        buildGui();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void buildGui() {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE,
                plugin.getConfigManager().getMessage("gui-filler-name"), null);
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        Collection<SmpEvent> events = plugin.getEventManager().getRegisteredEvents().values();
        SmpEvent current = plugin.getEventManager().getCurrentEvent();
        int slotIndex = 0;
        for (SmpEvent event : events) {
            if (slotIndex >= SLOTS.length) break;
            inventory.setItem(SLOTS[slotIndex++], buildEventItem(event, current));
        }

        // Close button
        ItemStack closeBtn = createItem(Material.BARRIER,
                plugin.getConfigManager().getMessage("gui-close-button-name"),
                List.of(plugin.getConfigManager().getMessage("gui-close-button-lore")));
        inventory.setItem(49, closeBtn);
    }

    private ItemStack buildEventItem(SmpEvent event, SmpEvent current) {
        Material icon = EVENT_ICONS.getOrDefault(event.getId(), Material.PAPER);
        boolean isActive = current != null && current.getId().equals(event.getId());

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().getMessage("gui-event-lore-separator"));

        // Description bullet points from events.yml
        List<String> descLines = event.getDescriptionLines();
        if (!descLines.isEmpty()) {
            for (String line : descLines) {
                lore.add(line);
            }
            lore.add(plugin.getConfigManager().getMessage("gui-event-lore-separator"));
        }

        // Duration (formatted)
        String durLabel = ColorUtil.replace(
                plugin.getConfigManager().getMessage("gui-event-duration-label"),
                "{duration}", EventManager.formatDuration(event.getDuration()));
        lore.add(durLabel);

        // Active badge
        if (isActive) {
            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("gui-current-event-lore"));
        }

        lore.add(plugin.getConfigManager().getMessage("gui-event-lore-separator"));

        // Wrap in glowing frame if active
        ItemStack item = createItem(icon, event.getDisplayName(), lore);
        if (isActive) {
            item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.colorize(name));
        if (lore != null) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) components.add(ColorUtil.colorize(line));
            meta.lore(components);
        }
        item.setItemMeta(meta);
        return item;
    }

    public void open() { viewer.openInventory(inventory); }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
        if (event.getSlot() == 49) viewer.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) HandlerList.unregisterAll(this);
    }
}
