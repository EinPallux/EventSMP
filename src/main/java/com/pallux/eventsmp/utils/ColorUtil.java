package com.pallux.eventsmp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN     = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_ALT = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {}

    /**
     * Translates a string with &#RRGGBB, <#RRGGBB>, and & legacy codes into a Component.
     * Italic is explicitly set to false so item display names / lores never appear in cursive.
     */
    public static Component colorize(String input) {
        if (input == null) return Component.empty();
        return LEGACY.deserialize(translateHex(input))
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Translates to a legacy §-coded string (for APIs that still require plain strings).
     */
    public static String colorizeString(String input) {
        if (input == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', translateHex(input));
    }

    /** Converts &#RRGGBB and <#RRGGBB> to §x§R§R§G§G§B§B Bukkit format. */
    private static String translateHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, buildBukkitHex(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        input = buffer.toString();

        matcher = HEX_PATTERN_ALT.matcher(input);
        buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, buildBukkitHex(matcher.group(1)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String buildBukkitHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }

    /** Replace placeholders: replace(msg, "{key}", "value", ...) */
    public static String replace(String input, String... replacements) {
        if (input == null) return "";
        for (int i = 0; i + 1 < replacements.length; i += 2)
            input = input.replace(replacements[i], replacements[i + 1]);
        return input;
    }

    public static Component colorizeReplaced(String input, String... replacements) {
        return colorize(replace(input, replacements));
    }
}