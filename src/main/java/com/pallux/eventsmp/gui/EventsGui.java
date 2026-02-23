package com.pallux.eventsmp.gui;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.events.SmpEvent;
import com.pallux.eventsmp.managers.EventManager;
import com.pallux.eventsmp.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EventsGui implements Listener {

    private final EventSMP plugin;
    private final Inventory inventory;
    private final Player viewer;

    /** Slot of the close button, detected from static-slots at build time. */
    private int closeButtonSlot = -1;

    public EventsGui(EventSMP plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;

        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("title", "&8Events");
        this.inventory = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));

        buildGui();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // -----------------------------------------------------------------------
    //  BUILD
    // -----------------------------------------------------------------------

    private void buildGui() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        // 1. Apply filler-items (lowest priority)
        applyFillerItems(gui);

        // 2. Place static-slots over fillers
        applyStaticSlots(gui);

        // 3. Place event items (highest priority, overwrites everything)
        applyEventItems(gui);
    }

    // -----------------------------------------------------------------------
    //  FILLER ITEMS
    // -----------------------------------------------------------------------

    /**
     * Reads the filler-items list. Each entry has material/name/lore and a
     * "slots" list that accepts individual numbers ("5") and ranges ("0-8").
     * Entries are applied in order, so later entries overwrite earlier ones
     * for overlapping slots.
     */
    private void applyFillerItems(FileConfiguration gui) {
        List<Map<?, ?>> fillerList = gui.getMapList("filler-items");
        if (fillerList.isEmpty()) {
            // Fallback: fill everything with gray glass
            ItemStack fallback = buildSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
            for (int i = 0; i < 54; i++) inventory.setItem(i, fallback);
            return;
        }

        for (Map<?, ?> entry : fillerList) {
            String matName = getString(entry, "material", "GRAY_STAINED_GLASS_PANE").toUpperCase();
            String name    = getString(entry, "name", " ");

            @SuppressWarnings("unchecked")
            List<String> lore = entry.containsKey("lore") ? (List<String>) entry.get("lore") : null;

            Material material = parseMaterial(matName);
            ItemStack item = buildSimpleItem(material, name, lore);

            // Parse the slots list
            Object rawSlots = entry.get("slots");
            Set<Integer> slots = parseSlotsList(rawSlots);

            for (int slot : slots) {
                if (slot >= 0 && slot < 54) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    /**
     * Parses a YAML value that is a list of slot descriptors.
     * Each element may be:
     *   - An integer:          5
     *   - A string integer:   "5"
     *   - A range string:     "0-8"
     */
    private Set<Integer> parseSlotsList(Object rawSlots) {
        Set<Integer> result = new LinkedHashSet<>();
        if (!(rawSlots instanceof List<?> list)) return result;

        for (Object element : list) {
            String token = element.toString().trim();
            if (token.contains("-")) {
                // Range: "start-end"
                String[] parts = token.split("-", 2);
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end   = Integer.parseInt(parts[1].trim());
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        result.add(i);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[EventsGui] Invalid slot range '" + token + "' in filler-items — skipping.");
                }
            } else {
                // Single slot
                try {
                    result.add(Integer.parseInt(token));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[EventsGui] Invalid slot value '" + token + "' in filler-items — skipping.");
                }
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    //  STATIC SLOTS
    // -----------------------------------------------------------------------

    private void applyStaticSlots(FileConfiguration gui) {
        ConfigurationSection staticSection = gui.getConfigurationSection("static-slots");
        if (staticSection == null) return;

        for (String key : staticSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                if (slot < 0 || slot >= 54) continue;
                ConfigurationSection itemSection = staticSection.getConfigurationSection(key);
                if (itemSection == null) continue;
                ItemStack item = buildItemFromSection(itemSection);
                inventory.setItem(slot, item);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[EventsGui] Invalid static-slot key '" + key + "' (must be 0–53) — skipping.");
            }
        }

        // Detect close button: first BARRIER in static-slots, fallback slot 49
        closeButtonSlot = detectCloseButtonSlot(staticSection);
    }

    private int detectCloseButtonSlot(ConfigurationSection staticSection) {
        for (String key : staticSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection sec = staticSection.getConfigurationSection(key);
                if (sec == null) continue;
                if ("BARRIER".equalsIgnoreCase(sec.getString("material", ""))) return slot;
            } catch (NumberFormatException ignored) {}
        }
        return 49;
    }

    // -----------------------------------------------------------------------
    //  EVENT ITEMS
    // -----------------------------------------------------------------------

    private void applyEventItems(FileConfiguration gui) {
        List<Integer> eventSlots = gui.getIntegerList("event-slots");
        if (eventSlots.isEmpty()) {
            eventSlots = List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34);
        }

        Collection<SmpEvent> events = plugin.getEventManager().getRegisteredEvents().values();
        SmpEvent currentEvent = plugin.getEventManager().getCurrentEvent();

        int slotIndex = 0;
        for (SmpEvent event : events) {
            if (slotIndex >= eventSlots.size()) break;
            int slot = eventSlots.get(slotIndex++);
            if (slot < 0 || slot >= 54) continue;
            inventory.setItem(slot, buildEventItem(gui, event, currentEvent));
        }
    }

    // -----------------------------------------------------------------------
    //  ITEM BUILDERS
    // -----------------------------------------------------------------------

    private ItemStack buildItemFromSection(ConfigurationSection section) {
        Material material = parseMaterial(section.getString("material", "STONE").toUpperCase());
        String name = section.getString("name", " ");
        List<String> lore = section.getStringList("lore");
        boolean glow = section.getBoolean("glow", false);

        ItemStack item = buildSimpleItem(material, name, lore.isEmpty() ? null : lore);
        if (glow) applyGlow(item);
        return item;
    }

    private ItemStack buildEventItem(FileConfiguration gui, SmpEvent event, SmpEvent current) {
        // Icon
        String iconName = gui.getString("event-icons." + event.getId(), "PAPER").toUpperCase();
        Material icon = parseMaterial(iconName);

        boolean isActive = current != null && current.getId().equals(event.getId());
        String duration  = EventManager.formatDuration(event.getDuration());
        List<String> descriptionLines = event.getDescriptionLines();

        // Build lore from template
        List<String> templateLines = gui.getStringList("event-lore-template");
        if (templateLines.isEmpty()) {
            templateLines = List.of(
                    "&8&m--------------------",
                    "{description_lines}",
                    "&8&m--------------------",
                    "&8⏱ &7Duration: &e{duration}",
                    "&8&m--------------------"
            );
        }

        List<String> lore = new ArrayList<>();
        for (String line : templateLines) {
            if (line.contains("{description_lines}")) {
                if (descriptionLines.isEmpty()) {
                    lore.add("");
                } else {
                    lore.addAll(descriptionLines);
                }
            } else {
                lore.add(line.replace("{duration}", duration));
            }
        }

        // Active badge
        if (isActive) {
            List<String> badge = gui.getStringList("active-event-badge");
            if (badge.isEmpty()) badge = List.of("", "&a▶ &a&lCurrently Active!");
            int insertAt = findLastSeparator(lore);
            if (insertAt >= 0) {
                lore.addAll(insertAt, badge);
            } else {
                lore.addAll(badge);
            }
        }

        ItemStack item = buildSimpleItem(icon, event.getDisplayName(), lore);

        if (isActive && gui.getBoolean("active-event-glow", true)) {
            applyGlow(item);
        }
        return item;
    }

    private ItemStack buildSimpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.colorize(name));

        if (lore != null && !lore.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) components.add(ColorUtil.colorize(line));
            meta.lore(components);
        }

        item.setItemMeta(meta);
        return item;
    }

    private void applyGlow(ItemStack item) {
        item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }

    // -----------------------------------------------------------------------
    //  HELPERS
    // -----------------------------------------------------------------------

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[EventsGui] Unknown material '" + name + "', using STONE.");
            return Material.STONE;
        }
    }

    /** Returns the index of the last &m separator line in the list, or -1. */
    private int findLastSeparator(List<String> lore) {
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line.contains("&m") || line.contains("§m")) return i;
        }
        return -1;
    }

    /** Safe map key getter with a default value. */
    private String getString(Map<?, ?> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    // -----------------------------------------------------------------------
    //  EVENTS
    // -----------------------------------------------------------------------

    public void open() {
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
        if (closeButtonSlot >= 0 && event.getSlot() == closeButtonSlot) {
            viewer.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }
}