package me.abdelrahmanmoharramdev.aquixLinkCore.commands;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import me.abdelrahmanmoharramdev.aquixLinkCore.storage.LinkStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public class LinkCommand implements CommandExecutor {

    private static final int CODE_LENGTH = 6;
    private static final int MAX_CODE = 1_000_000; // for 6 digits
    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        LinkStorage linkStorage = AquixLinkCore.getInstance().getLinkStorage();

        if (linkStorage == null) {
            player.sendMessage(ChatColor.RED + "Internal error: link storage unavailable.");
            AquixLinkCore.getInstance().getLogger().severe("LinkStorage instance is null in LinkCommand.");
            return true;
        }

        // Already linked
        if (linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already linked to a Discord account. " +
                    ChatColor.GREEN + "To unlink, use /unlink.");
            return true;
        }

        // Pending verification exists? Check if expired
        if (linkStorage.hasPendingVerification(player.getUniqueId())) {
            if (linkStorage.isVerificationExpired(player.getUniqueId())) {
                // Remove expired pending verification so player can create a new one
                linkStorage.removePendingVerification(player.getUniqueId());
            } else {
                player.sendMessage(ChatColor.RED + "You already have a pending verification request.");
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/verifylink <code>" + ChatColor.GRAY + " to complete linking.");
                return true;
            }
        }

        // Validate command usage
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <discord_id>");
            return true;
        }

        String discordId = args[0];

        // Validate Discord ID format
        if (!discordId.matches("^\\d{17,20}$")) {
            player.sendMessage(ChatColor.RED + "Invalid Discord ID. It should be a 17–20 digit number.");
            return true;
        }

        // Check if Discord ID is already linked to another player
        if (linkStorage.isDiscordIdLinked(discordId)) {
            player.sendMessage(ChatColor.RED + "This Discord ID is already linked to another player.");
            return true;
        }

        // Generate and store a 6-digit verification code
        String code = String.format("%0" + CODE_LENGTH + "d", random.nextInt(MAX_CODE));
        linkStorage.setPendingVerification(player.getUniqueId(), discordId, code);

        // Send success messages
        player.sendMessage(ChatColor.GREEN + "✅ A verification code has been generated.");
        player.sendMessage(ChatColor.GRAY + "To complete the process, type:");
        player.sendMessage(ChatColor.YELLOW + "/verifylink " + code);
        player.sendMessage(ChatColor.GRAY + "Note: This code is valid for 5 minutes.");

        return true;
    }
}
