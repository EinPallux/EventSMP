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

    // Fixed event slots per page — 21 slots across 3 rows
    private static final List<Integer> EVENT_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );
    private static final int SLOTS_PER_PAGE = EVENT_SLOTS.size(); // 21

    // Navigation button slots
    private static final int SLOT_PREV   = 45;
    private static final int SLOT_NEXT   = 53;
    private static final int SLOT_CLOSE  = 49;
    private static final int SLOT_INFO   = 4;  // page indicator (top-middle)

    private final EventSMP plugin;
    private final Player viewer;
    private Inventory inventory;

    /** All registered events in order */
    private final List<SmpEvent> allEvents;
    private int currentPage = 0; // zero-based

    public EventsGui(EventSMP plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.allEvents = new ArrayList<>(plugin.getEventManager().getRegisteredEvents().values());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // -----------------------------------------------------------------------
    //  PUBLIC ENTRY POINT
    // -----------------------------------------------------------------------

    public void open() {
        buildPage();
        viewer.openInventory(inventory);
        // After openInventory the server may wrap our inventory — track the live one
        this.inventory = viewer.getOpenInventory().getTopInventory();
    }

    // -----------------------------------------------------------------------
    //  PAGE BUILDING
    // -----------------------------------------------------------------------

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) allEvents.size() / SLOTS_PER_PAGE));
    }

    private void buildPage() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String rawTitle = gui.getString("title", "&8&l✦ &8Events ➠ Overview");

        // Create (or recreate) inventory
        Inventory newInv = Bukkit.createInventory(null, 54, ColorUtil.colorize(rawTitle));

        // 1. Filler layer
        applyFillerItems(gui, newInv);

        // 2. Static slots (close button, etc.)
        applyStaticSlots(gui, newInv);

        // 3. Navigation buttons
        applyNavigationButtons(gui, newInv);

        // 4. Page-indicator item in slot 4
        applyPageIndicator(newInv);

        // 5. Event items for this page (empties filled with gray dye)
        applyEventItems(gui, newInv);

        this.inventory = newInv;
    }

    /** Refreshes the open inventory in-place (no close/reopen flicker). */
    private void refreshPage() {
        // Build into a temporary inventory, then push its contents into the
        // already-open inventory so the viewer's reference never becomes stale.
        Inventory live = viewer.getOpenInventory().getTopInventory();
        buildPage();                          // this.inventory = temporary new inv
        live.setContents(inventory.getContents());
        this.inventory = live;               // always track the live object
    }

    // -----------------------------------------------------------------------
    //  FILLER ITEMS
    // -----------------------------------------------------------------------

    private void applyFillerItems(FileConfiguration gui, Inventory inv) {
        List<Map<?, ?>> fillerList = gui.getMapList("filler-items");
        if (fillerList.isEmpty()) {
            ItemStack fallback = buildSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
            for (int i = 0; i < 54; i++) inv.setItem(i, fallback);
            return;
        }
        for (Map<?, ?> entry : fillerList) {
            String matName = getString(entry, "material", "GRAY_STAINED_GLASS_PANE").toUpperCase();
            String name    = getString(entry, "name", " ");
            @SuppressWarnings("unchecked")
            List<String> lore = entry.containsKey("lore") ? (List<String>) entry.get("lore") : null;
            Material material = parseMaterial(matName);
            ItemStack item = buildSimpleItem(material, name, lore);
            for (int slot : parseSlotsList(entry.get("slots"))) {
                if (slot >= 0 && slot < 54) inv.setItem(slot, item);
            }
        }
    }

    // -----------------------------------------------------------------------
    //  STATIC SLOTS
    // -----------------------------------------------------------------------

    private void applyStaticSlots(FileConfiguration gui, Inventory inv) {
        ConfigurationSection staticSection = gui.getConfigurationSection("static-slots");
        if (staticSection == null) return;
        for (String key : staticSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                if (slot < 0 || slot >= 54) continue;
                // Don't overwrite nav slots — those are handled separately
                if (slot == SLOT_PREV || slot == SLOT_NEXT) continue;
                ConfigurationSection itemSection = staticSection.getConfigurationSection(key);
                if (itemSection == null) continue;
                inv.setItem(slot, buildItemFromSection(itemSection));
            } catch (NumberFormatException ignored) {}
        }
    }

    // -----------------------------------------------------------------------
    //  NAVIGATION BUTTONS
    // -----------------------------------------------------------------------

    private void applyNavigationButtons(FileConfiguration gui, Inventory inv) {
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages() - 1;

        // Previous button
        if (hasPrev) {
            List<String> prevLore = gui.getStringList("nav-prev-lore");
            if (prevLore.isEmpty()) prevLore = List.of("&7ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ᴛᴏ ᴛʜᴇ ᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ.");
            String prevName = gui.getString("nav-prev-name", "&e&l← ᴘʀᴇᴠɪᴏᴜꜱ");
            String prevMat  = gui.getString("nav-prev-material", "ARROW").toUpperCase();
            inv.setItem(SLOT_PREV, buildSimpleItem(parseMaterial(prevMat), prevName, prevLore));
        } else {
            // Show disabled / blank filler in that slot
            inv.setItem(SLOT_PREV, buildSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        }

        // Next button
        if (hasNext) {
            List<String> nextLore = gui.getStringList("nav-next-lore");
            if (nextLore.isEmpty()) nextLore = List.of("&7ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ᴛᴏ ᴛʜᴇ ɴᴇxᴛ ᴘᴀɢᴇ.");
            String nextName = gui.getString("nav-next-name", "&e&lɴᴇxᴛ →");
            String nextMat  = gui.getString("nav-next-material", "ARROW").toUpperCase();
            inv.setItem(SLOT_NEXT, buildSimpleItem(parseMaterial(nextMat), nextName, nextLore));
        } else {
            inv.setItem(SLOT_NEXT, buildSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        }
    }

    // -----------------------------------------------------------------------
    //  PAGE INDICATOR
    // -----------------------------------------------------------------------

    private void applyPageIndicator(Inventory inv) {
        if (totalPages() <= 1) return; // no indicator needed on a single page
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String mat  = gui.getString("page-indicator-material", "BOOK").toUpperCase();
        String name = gui.getString("page-indicator-name", "&7ᴘᴀɢᴇ &e{page} &7/ &e{total}");
        name = name.replace("{page}", String.valueOf(currentPage + 1))
                .replace("{total}", String.valueOf(totalPages()));
        List<String> lore = new ArrayList<>(gui.getStringList("page-indicator-lore"));
        if (lore.isEmpty()) lore = List.of("&8" + allEvents.size() + " events total");
        inv.setItem(SLOT_INFO, buildSimpleItem(parseMaterial(mat), name, lore));
    }

    // -----------------------------------------------------------------------
    //  EVENT ITEMS
    // -----------------------------------------------------------------------

    private void applyEventItems(FileConfiguration gui, Inventory inv) {
        SmpEvent currentEvent = plugin.getEventManager().getCurrentEvent();

        // Empty-slot filler — fully configurable from event-gui.yml
        String emptyMat  = gui.getString("empty-slot-material", "GRAY_DYE").toUpperCase();
        String emptyName = gui.getString("empty-slot-name", "&7&o&lɴᴏ ᴇᴠᴇɴᴛ");
        List<String> emptyLore = gui.getStringList("empty-slot-lore");
        ItemStack emptySlotItem = buildSimpleItem(
                parseMaterial(emptyMat),
                emptyName,
                emptyLore.isEmpty() ? null : emptyLore
        );

        int startIndex = currentPage * SLOTS_PER_PAGE;

        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            int slot = EVENT_SLOTS.get(i);
            int eventIndex = startIndex + i;

            if (eventIndex < allEvents.size()) {
                SmpEvent event = allEvents.get(eventIndex);
                inv.setItem(slot, buildEventItem(gui, event, currentEvent));
            } else {
                // Not enough events to fill this slot — use gray dye filler
                inv.setItem(slot, emptySlotItem);
            }
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
        String iconName = gui.getString("event-icons." + event.getId(), "PAPER").toUpperCase();
        Material icon = parseMaterial(iconName);

        boolean isActive = current != null && current.getId().equals(event.getId());
        String duration  = EventManager.formatDuration(event.getDuration());
        List<String> descriptionLines = event.getDescriptionLines();

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
                lore.addAll(descriptionLines.isEmpty() ? List.of("") : descriptionLines);
            } else {
                lore.add(line.replace("{duration}", duration));
            }
        }

        if (isActive) {
            List<String> badge = gui.getStringList("active-event-badge");
            if (badge.isEmpty()) badge = List.of("", "&a▶ &a&lCurrently Active!");
            int insertAt = findLastSeparator(lore);
            if (insertAt >= 0) lore.addAll(insertAt, badge);
            else lore.addAll(badge);
        }

        ItemStack item = buildSimpleItem(icon, event.getDisplayName(), lore);
        if (isActive && gui.getBoolean("active-event-glow", true)) applyGlow(item);
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
    //  CLICK HANDLING
    // -----------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Guard: only care about our viewer's open GUI
        Inventory top = event.getView().getTopInventory();
        if (!top.equals(inventory)) return;

        // Cancel ALL clicks that touch the top inventory (including shift-clicks
        // from the bottom inventory which have getClickedInventory() != top)
        event.setCancelled(true);

        // Ignore clicks that landed in the player's own bottom inventory
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(top)) return;

        int slot = event.getSlot();

        if (slot == SLOT_CLOSE) {
            viewer.closeInventory();
            return;
        }

        if (slot == SLOT_PREV && currentPage > 0) {
            currentPage--;
            refreshPage();
            return;
        }

        if (slot == SLOT_NEXT && currentPage < totalPages() - 1) {
            currentPage++;
            refreshPage();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }

    // -----------------------------------------------------------------------
    //  HELPERS
    // -----------------------------------------------------------------------

    private Set<Integer> parseSlotsList(Object rawSlots) {
        Set<Integer> result = new LinkedHashSet<>();
        if (!(rawSlots instanceof List<?> list)) return result;
        for (Object element : list) {
            String token = element.toString().trim();
            if (token.contains("-")) {
                String[] parts = token.split("-", 2);
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end   = Integer.parseInt(parts[1].trim());
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) result.add(i);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[EventsGui] Invalid slot range '" + token + "' — skipping.");
                }
            } else {
                try { result.add(Integer.parseInt(token)); }
                catch (NumberFormatException e) {
                    plugin.getLogger().warning("[EventsGui] Invalid slot '" + token + "' — skipping.");
                }
            }
        }
        return result;
    }

    private Material parseMaterial(String name) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[EventsGui] Unknown material '" + name + "', using STONE.");
            return Material.STONE;
        }
    }

    private int findLastSeparator(List<String> lore) {
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line.contains("&m") || line.contains("§m")) return i;
        }
        return -1;
    }

    private String getString(Map<?, ?> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }
}