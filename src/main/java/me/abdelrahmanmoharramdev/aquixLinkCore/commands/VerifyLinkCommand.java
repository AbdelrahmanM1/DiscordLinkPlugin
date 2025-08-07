package me.abdelrahmanmoharramdev.aquixLinkCore.commands;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import me.abdelrahmanmoharramdev.aquixLinkCore.storage.LinkStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VerifyLinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        LinkStorage linkStorage = AquixLinkCore.getInstance().getLinkStorage();

        // Check already linked
        if (linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already linked to a Discord account.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /verifylink <code>");
            return true;
        }

        String code = args[0];

        // Check if pending verification exists
        if (!linkStorage.hasPendingVerification(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have no pending link request.");
            return true;
        }

        // Check if code matches
        if (!linkStorage.isCodeValid(player.getUniqueId(), code)) {
            player.sendMessage(ChatColor.RED + "Invalid verification code.");
            return true;
        }

        // Confirm verification
        linkStorage.confirmVerification(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Your Discord account has been successfully linked!");
        return true;
    }
}
