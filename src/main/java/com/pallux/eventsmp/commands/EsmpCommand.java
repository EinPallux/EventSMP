package com.pallux.eventsmp.commands;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.gui.EventsGui;
import com.pallux.eventsmp.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EsmpCommand implements CommandExecutor, TabCompleter {

    private final EventSMP plugin;

    public EsmpCommand(EventSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getConfig()
                .getString("plugin-prefix", "&8&lEVENTSMP &7➠ ");

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                if (!sender.hasPermission("esmp.admin")) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("reload-success")));
            }

            case "start" -> {
                if (!sender.hasPermission("esmp.admin")) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("usage-start")));
                    return true;
                }
                String eventId = args[1].toLowerCase();
                if (plugin.getEventManager().getEvent(eventId) == null) {
                    String msg = ColorUtil.replace(plugin.getConfigManager().getMessage("unknown-event"), "{event}", eventId);
                    sender.sendMessage(ColorUtil.colorize(prefix + msg));
                    return true;
                }
                if (plugin.getEventManager().getCurrentEvent() != null) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("event-already-active")));
                    return true;
                }
                if (plugin.getEventManager().startEvent(eventId)) {
                    String msg = ColorUtil.replace(plugin.getConfigManager().getMessage("event-start-admin"), "{event}", eventId);
                    sender.sendMessage(ColorUtil.colorize(prefix + msg));
                }
            }

            case "stop" -> {
                if (!sender.hasPermission("esmp.admin")) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (plugin.getEventManager().getCurrentEvent() == null) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-event-active")));
                    return true;
                }
                String eventId = plugin.getEventManager().getCurrentEvent().getId();
                plugin.getEventManager().stopCurrentEvent(true);
                String msg = ColorUtil.replace(plugin.getConfigManager().getMessage("event-stop-admin"), "{event}", eventId);
                sender.sendMessage(ColorUtil.colorize(prefix + msg));
            }

            case "events" -> {
                if (!sender.hasPermission("esmp.player")) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("player-only")));
                    return true;
                }
                new EventsGui(plugin, player).open();
            }

            case "togglebar" -> {
                if (!sender.hasPermission("esmp.player")) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("no-permission")));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.colorize(prefix + plugin.getConfigManager().getMessage("player-only")));
                    return true;
                }
                boolean nowHidden = plugin.getDisplayManager().toggleBar(player);
                String msgKey = nowHidden ? "togglebar-hidden" : "togglebar-shown";
                player.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage(msgKey)));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-header")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-reload")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-start")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-stop")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-events")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-togglebar")));
        sender.sendMessage(ColorUtil.colorize(plugin.getConfigManager().getMessage("help-footer")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("reload", "start", "stop", "events", "togglebar"));
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return plugin.getEventManager().getRegisteredEvents().keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
