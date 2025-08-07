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

        if (linkStorage.isPlayerLinked(player.getUniqueId())) {
            player.sendMessage("§cYou are already linked to a Discord account.");
            return true;
        }

        if (linkStorage.hasPendingVerification(player.getUniqueId())) {
            player.sendMessage("§cYou already have a pending verification.");
            player.sendMessage("§7Use §e/verifylink <code> §7to complete the linking process.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /linkdiscord <discord_id>");
            return true;
        }

        String discordId = args[0];

        // Validate Discord ID
        if (!discordId.matches("^\\d{17,20}$")) {
            player.sendMessage("§cInvalid Discord ID. Please use your 18-digit numeric Discord user ID.");
            return true;
        }

        // Generate 6-digit verification code
        String code = String.format("%06d", random.nextInt(1_000_000));
        linkStorage.setPendingVerification(player.getUniqueId(), discordId, code);

        player.sendMessage("§aVerification code generated!");
        player.sendMessage("§7Use §e/verifylink " + code + " §7to confirm the link.");
        return true;
    }
}
