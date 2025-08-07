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

        if (linkStorage == null) {
            player.sendMessage(ChatColor.RED + "Internal error: Link storage unavailable.");
            return true;
        }

        // Already linked
        if (linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already linked to a Discord account.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /verifylink <code>");
            return true;
        }

        String code = args[0];

        // No pending verification
        if (!linkStorage.hasPendingVerification(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have no pending link request.");
            return true;
        }

        // Expired?
        if (linkStorage.isVerificationExpired(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Your verification code has expired. Please use /linkdiscord again.");
            linkStorage.removePendingVerification(player.getUniqueId());
            return true;
        }

        // Code match?
        if (!linkStorage.isCodeValid(player.getUniqueId(), code)) {
            player.sendMessage(ChatColor.RED + "Invalid verification code.");
            return true;
        }

        // Success
        linkStorage.confirmVerification(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Your Discord account has been successfully linked!");
        return true;
    }
}
