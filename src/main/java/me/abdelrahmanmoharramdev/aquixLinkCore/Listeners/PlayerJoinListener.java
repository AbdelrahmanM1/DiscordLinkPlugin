package me.abdelrahmanmoharramdev.aquixLinkCore.Listeners;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean isLinked = AquixLinkCore.getInstance()
                .getLinkStorage()
                .isPlayerLinked(player.getUniqueId());

        if (!isLinked) {
            player.sendMessage(ChatColor.RED + "============================================");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Welcome to AquixMC, " + player.getName() + "!");
            player.sendMessage(ChatColor.YELLOW + "It looks like your Minecraft account is " + ChatColor.RED + "NOT linked " + ChatColor.YELLOW + "with your Discord account.");
            player.sendMessage(ChatColor.GRAY + "Linking your account allows you to verify your identity, get special ranks, and access exclusive Discord channels.");
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "To link your account, simply type: " + ChatColor.GREEN + "/linkdiscord");
            player.sendMessage(ChatColor.GRAY + "After typing the command, follow the instructions shown to complete the process.");
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_GREEN + "Enjoy your stay at " + ChatColor.BOLD + "AquixMC" + ChatColor.DARK_GREEN + "!");
            player.sendMessage(ChatColor.RED + "============================================");
        } else {
            player.sendMessage(ChatColor.GREEN + "============================================");
            player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Welcome back to AquixMC, " + player.getName() + "!");
            player.sendMessage(ChatColor.YELLOW + "Your Minecraft account is already linked with your Discord.");
            player.sendMessage(ChatColor.GRAY + "You now have full access to our Discord-linked features.");
            player.sendMessage(ChatColor.GREEN + "============================================");
        }
    }
}
