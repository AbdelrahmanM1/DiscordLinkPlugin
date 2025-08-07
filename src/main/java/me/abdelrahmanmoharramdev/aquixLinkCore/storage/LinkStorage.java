package me.abdelrahmanmoharramdev.aquixLinkCore.storage;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LinkStorage {

    private final AquixLinkCore plugin;
    private final DatabaseManager db;

    public LinkStorage(AquixLinkCore plugin) {
        this.plugin = plugin;
        this.db = new DatabaseManager(plugin);
    }

    public void setPendingVerification(UUID uuid, String discordId, String code) {
        String sql = """
            INSERT OR REPLACE INTO pending_verifications (uuid, discord_id, code, created_at)
            VALUES (?, ?, ?, datetime('now','localtime'))
        """; // use localtime for SQLite

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setString(3, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error storing pending verification: " + e.getMessage());
        }
    }

    public boolean hasPendingVerification(UUID uuid) {
        String sql = "SELECT 1 FROM pending_verifications WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking pending verification: " + e.getMessage());
            return false;
        }
    }

    public boolean isCodeValid(UUID uuid, String code) {
        String sql = "SELECT 1 FROM pending_verifications WHERE uuid = ? AND code = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking code validity: " + e.getMessage());
            return false;
        }
    }

    public boolean isVerificationExpired(UUID uuid) {
        String sql = "SELECT created_at FROM pending_verifications WHERE uuid = ?";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String createdAtStr = rs.getString("created_at");
                    plugin.getLogger().info("Verification code created at: " + createdAtStr);
                    LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, formatter);
                    long createdAtMillis = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                    long now = System.currentTimeMillis();
                    plugin.getLogger().info("Current time millis: " + now + ", Created time millis: " + createdAtMillis);
                    plugin.getLogger().info("Elapsed millis: " + (now - createdAtMillis));

                    return (now - createdAtMillis) > (5 * 60 * 1000); // expired after 5 minutes
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking code expiry: " + e.getMessage());
        }

        // Default to expired on error or not found
        return true;
    }

    public void removePendingVerification(UUID uuid) {
        String sql = "DELETE FROM pending_verifications WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error removing pending verification: " + e.getMessage());
        }
    }

    public void confirmVerification(UUID uuid) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement select = conn.prepareStatement("SELECT discord_id FROM pending_verifications WHERE uuid = ?");
                    PreparedStatement insert = conn.prepareStatement("INSERT OR REPLACE INTO links (uuid, discord_id) VALUES (?, ?)");
                    PreparedStatement delete = conn.prepareStatement("DELETE FROM pending_verifications WHERE uuid = ?")
            ) {
                select.setString(1, uuid.toString());
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        String discordId = rs.getString("discord_id");

                        insert.setString(1, uuid.toString());
                        insert.setString(2, discordId);
                        insert.executeUpdate();

                        delete.setString(1, uuid.toString());
                        delete.executeUpdate();

                        conn.commit();
                        return;
                    } else {
                        plugin.getLogger().warning("No valid pending verification found for UUID: " + uuid);
                    }
                }
                conn.rollback();
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
        String sql = "SELECT 1 FROM links WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking linked player: " + e.getMessage());
            return false;
        }
    }

    public String getDiscordId(UUID uuid) {
        String sql = "SELECT discord_id FROM links WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error retrieving Discord ID for UUID: " + uuid + " — " + e.getMessage());
        }

        return null;
    }

    public void unlinkPlayer(UUID uuid) {
        String sql = "DELETE FROM links WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error unlinking player with UUID: " + uuid + " — " + e.getMessage());
        }
    }

    public boolean isDiscordIdLinked(String discordId) {
        String sql = "SELECT 1 FROM links WHERE discord_id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking if Discord ID is already linked: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        db.close();
    }
}
