package me.abdelrahmanmoharramdev.aquixLinkCore.commands;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import me.abdelrahmanmoharramdev.aquixLinkCore.storage.LinkStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public class LinkCommand implements CommandExecutor {

    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        LinkStorage linkStorage = AquixLinkCore.getInstance().getLinkStorage();

        if (linkStorage == null) {
            player.sendMessage("§cInternal error: link storage unavailable.");
            return true;
        }

        // Already linked
        if (linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage("§cYou are already linked to a Discord account.");
            return true;
        }

        // Pending verification exists? But check if expired first.
        if (linkStorage.hasPendingVerification(player.getUniqueId())) {
            if (linkStorage.isVerificationExpired(player.getUniqueId())) {
                // Remove expired pending verification so player can create a new one
                linkStorage.removePendingVerification(player.getUniqueId());
            } else {
                player.sendMessage("§cYou already have a pending verification request.");
                player.sendMessage("§7Use §e/verifylink <code> §7to complete linking.");
                return true;
            }
        }

        // Validate command usage
        if (args.length != 1) {
            player.sendMessage("§cUsage: /linkdiscord <discord_id>");
            return true;
        }

        String discordId = args[0];

        // Validate Discord ID format
        if (!discordId.matches("^\\d{17,20}$")) {
            player.sendMessage("§cInvalid Discord ID. It should be a 17–20 digit number.");
            return true;
        }

        // Check if Discord ID is already linked to another player
        if (linkStorage.isDiscordIdLinked(discordId)) {
            player.sendMessage("§cThis Discord ID is already linked to another player.");
            return true;
        }

        // Generate and store a 6-digit verification code
        String code = String.format("%06d", random.nextInt(1_000_000));
        linkStorage.setPendingVerification(player.getUniqueId(), discordId, code);

        // Send success messages
        player.sendMessage("§a✅ A verification code has been generated.");
        player.sendMessage("§7To complete the process, type:");
        player.sendMessage("§e/verifylink " + code);
        player.sendMessage("§7Note: This code is valid for 5 minutes.");

        return true;
    }
}
