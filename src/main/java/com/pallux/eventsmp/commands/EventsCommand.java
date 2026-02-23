package com.pallux.eventsmp.commands;

import com.pallux.eventsmp.EventSMP;
import com.pallux.eventsmp.gui.EventsGui;
import com.pallux.eventsmp.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventsCommand implements CommandExecutor {

    private final EventSMP plugin;

    public EventsCommand(EventSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getConfig().getString("plugin-prefix", "&8&lEVENTSMP &7➠ ");

        if (!sender.hasPermission("esmp.player")) {
            sender.sendMessage(ColorUtil.colorize(prefix +
                    plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize(prefix +
                    plugin.getConfigManager().getMessage("player-only")));
            return true;
        }
        new EventsGui(plugin, player).open();
        return true;
    }
}
