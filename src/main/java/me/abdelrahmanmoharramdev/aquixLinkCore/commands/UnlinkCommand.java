package me.abdelrahmanmoharramdev.aquixLinkCore.commands;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import me.abdelrahmanmoharramdev.aquixLinkCore.storage.LinkStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        LinkStorage linkStorage = AquixLinkCore.getInstance().getLinkStorage();

        if (linkStorage == null) {
            player.sendMessage(ChatColor.RED + "An internal error occurred (link storage is null).");
            return true;
        }

        if (!linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not linked.");
            return true;
        }

        linkStorage.unlinkPlayer(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Successfully unlinked your Discord account.");
        return true;
    }
}
