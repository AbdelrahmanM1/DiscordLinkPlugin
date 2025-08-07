package me.abdelrahmanmoharramdev.aquixLinkCore.storage;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;

import java.sql.*;
import java.util.UUID;

public class LinkStorage {

    private final AquixLinkCore plugin;
    private final DatabaseManager db;

    public LinkStorage(AquixLinkCore plugin) {
        this.plugin = plugin;
        this.db = new DatabaseManager(plugin);
    }

    public void setPendingVerification(UUID uuid, String discordId, String code) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            INSERT OR REPLACE INTO pending_verifications (uuid, discord_id, code)
            VALUES (?, ?, ?)
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setString(3, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error storing pending verification: " + e.getMessage());
        }
    }

    public boolean hasPendingVerification(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            SELECT 1 FROM pending_verifications WHERE uuid = ?
        """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking pending verification: " + e.getMessage());
            return false;
        }
    }

    public boolean isCodeValid(UUID uuid, String code) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            SELECT 1 FROM pending_verifications WHERE uuid = ? AND code = ?
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking code validity: " + e.getMessage());
            return false;
        }
    }

    public void confirmVerification(UUID uuid) {
        try (Connection conn = db.getConnection()) {
            if (conn == null) {
                plugin.getLogger().severe("Database connection is null during verification.");
                return;
            }

            conn.setAutoCommit(false);

            try (
                    PreparedStatement select = conn.prepareStatement("""
                    SELECT discord_id FROM pending_verifications WHERE uuid = ?
                """);
                    PreparedStatement insert = conn.prepareStatement("""
                    INSERT OR REPLACE INTO links (uuid, discord_id) VALUES (?, ?)
                """);
                    PreparedStatement delete = conn.prepareStatement("""
                    DELETE FROM pending_verifications WHERE uuid = ?
                """)
            ) {
                select.setString(1, uuid.toString());
                ResultSet rs = select.executeQuery();

                if (rs.next()) {
                    String discordId = rs.getString("discord_id");

                    insert.setString(1, uuid.toString());
                    insert.setString(2, discordId);
                    insert.executeUpdate();

                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();

                    conn.commit();
                } else {
                    plugin.getLogger().warning("No pending verification found for UUID: " + uuid);
                }

            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().severe("Verification failed for UUID: " + uuid + " — " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to confirm verification for UUID: " + uuid + " — " + e.getMessage());
        }
    }

    public boolean isPlayerLinked(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            SELECT 1 FROM links WHERE uuid = ?
        """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking linked player: " + e.getMessage());
            return false;
        }
    }

    public String getDiscordId(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            SELECT discord_id FROM links WHERE uuid = ?
        """)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving Discord ID for UUID: " + uuid + " — " + e.getMessage());
        }
        return null;
    }

    public void unlinkPlayer(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            DELETE FROM links WHERE uuid = ?
        """)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error unlinking player with UUID: " + uuid + " — " + e.getMessage());
        }
    }

    public void close() {
        db.close();
    }

    //  utility method — useful for checking duplicate links
    public boolean isDiscordIdLinked(String discordId) {
        try (PreparedStatement ps = db.getConnection().prepareStatement("""
            SELECT 1 FROM links WHERE discord_id = ?
        """)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking if Discord ID is already linked: " + e.getMessage());
            return false;
        }
        }
}
