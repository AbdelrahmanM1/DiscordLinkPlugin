package me.abdelrahmanmoharramdev.aquixLinkCore;

import me.abdelrahmanmoharramdev.aquixLinkCore.commands.LinkCommand;
import me.abdelrahmanmoharramdev.aquixLinkCore.commands.ReloadLinkCommand;
import me.abdelrahmanmoharramdev.aquixLinkCore.commands.UnlinkCommand;
import me.abdelrahmanmoharramdev.aquixLinkCore.commands.VerifyLinkCommand;
import me.abdelrahmanmoharramdev.aquixLinkCore.storage.LinkStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class AquixLinkCore extends JavaPlugin {

    private static AquixLinkCore instance;
    private LinkStorage linkStorage;

    public static AquixLinkCore getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Initialize SQLite-based storage
        this.linkStorage = new LinkStorage(this);

        // Register commands
        getCommand("linkdiscord").setExecutor(new LinkCommand());
        getCommand("unlinkdiscord").setExecutor(new UnlinkCommand());
        getCommand("reloadlink").setExecutor(new ReloadLinkCommand());
        getCommand("verifylink").setExecutor(new VerifyLinkCommand());

        getLogger().info("AquixLinkCore has been enabled.");
    }

    @Override
    public void onDisable() {
        if (linkStorage != null) {
            linkStorage.close();
        }
        getLogger().info("AquixLinkCore has been disabled.");
    }

    public LinkStorage getLinkStorage() {
        return linkStorage;
    }
}
