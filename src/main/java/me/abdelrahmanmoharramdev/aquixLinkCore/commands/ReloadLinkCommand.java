package me.abdelrahmanmoharramdev.aquixLinkCore.commands;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadLinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        //check the premission
        if (!sender.hasPermission("aquixlink.reload")) {
            sender.sendMessage("§cYou do not have permission to reload the plugin.");
            return true;
        }

        AquixLinkCore plugin = AquixLinkCore.getInstance();

        if (plugin == null) {
            sender.sendMessage("§cPlugin instance not available.");
            return true;
        }

        // Reload only the config file
        plugin.reloadConfig();
        sender.sendMessage("§aAquixLinkCore config has been reloaded.");
        return true;
    }
}
