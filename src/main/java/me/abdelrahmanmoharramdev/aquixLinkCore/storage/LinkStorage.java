package me.abdelrahmanmoharramdev.aquixLinkCore.storage;

import me.abdelrahmanmoharramdev.aquixLinkCore.AquixLinkCore;
import me.abdelrahmanmoharramdev.aquixLinkCore.Cache.CacheProcessor;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LinkStorage {

    private final AquixLinkCore plugin;
    private final DatabaseManager db;
    private final CacheProcessor cacheProcessor;

    public LinkStorage(AquixLinkCore plugin) {
        this.plugin = plugin;
        this.db = new DatabaseManager(plugin);
        this.cacheProcessor = new CacheProcessor();
    }

    public void setPendingVerification(UUID uuid, String discordId, String code) {
        String sql = """
            INSERT OR REPLACE INTO pending_verifications (uuid, discord_id, code, created_at)
            VALUES (?, ?, ?, datetime('now','localtime'))
        """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, discordId);
            ps.setString(3, code);
            ps.executeUpdate();

            // Cache the pending verification code
            cacheProcessor.cachePendingVerification(uuid, code);

        } catch (SQLException e) {
            plugin.getLogger().severe("Error storing pending verification: " + e.getMessage());
        }
    }

    public boolean hasPendingVerification(UUID uuid) {
        // Check cache first for pending code existence
        if (cacheProcessor.getCachedPendingCode(uuid) != null) return true;

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
        // Check cache first for code validity
        String cachedCode = cacheProcessor.getCachedPendingCode(uuid);
        if (cachedCode != null) {
            return cachedCode.equals(code);
        }

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

                    return (now - createdAtMillis) > (5 * 60 * 1000); // 5 min expiry
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking code expiry: " + e.getMessage());
        }
        return true; // expire by default on error or not found
    }

    public void removePendingVerification(UUID uuid) {
        String sql = "DELETE FROM pending_verifications WHERE uuid = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();

            // Invalidate cache
            cacheProcessor.invalidatePendingVerification(uuid);

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
                    PreparedStatement delete = conn.prepareStatement("DELETE FROM pending_verifications WHERE uuid = ?");
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

                        // Update caches
                        cacheProcessor.cacheLinkedPlayer(uuid, discordId);
                        cacheProcessor.invalidatePendingVerification(uuid);

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
        // Check cache first
        if (cacheProcessor.getCachedDiscordId(uuid) != null) return true;

        String sql = "SELECT 1 FROM links WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                boolean linked = rs.next();
                if (linked) {
                    // Cache linked player for future calls
                    String discordId = getDiscordId(uuid);
                    if (discordId != null) {
                        cacheProcessor.cacheLinkedPlayer(uuid, discordId);
                    }
                }
                return linked;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking linked player: " + e.getMessage());
            return false;
        }
    }

    public String getDiscordId(UUID uuid) {
        // Check cache first
        String cached = cacheProcessor.getCachedDiscordId(uuid);
        if (cached != null) return cached;

        String sql = "SELECT discord_id FROM links WHERE uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String discordId = rs.getString("discord_id");
                    // Cache it
                    cacheProcessor.cacheLinkedPlayer(uuid, discordId);
                    return discordId;
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

            // Remove from cache
            cacheProcessor.invalidateLinkedPlayer(uuid);

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
