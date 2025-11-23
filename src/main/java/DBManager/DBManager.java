package DBManager;

import Challenges.Challenges;
import me.sachin.decemberChallenges.DecemberChallenges;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DBManager {

    private final DecemberChallenges plugin;
    private Connection connection;

    private static final String TABLE_NAME = "Challenges";

    public DBManager(DecemberChallenges plugin) {
        this.plugin = plugin;
    }

    // Call from onEnable
    public void init() {
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {
                // Driver may already be loaded
            }

            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            File dbFile = new File(plugin.getDataFolder(), "challenges.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            createTable();
            plugin.getLogger().info("[DecemberChallenges] SQLite database initialised.");
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Could not initialise SQLite database:");
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "UUID TEXT PRIMARY KEY,"
                + "ChestLootCount INTEGER NOT NULL DEFAULT 0,"
                + "CriminalKillCount INTEGER NOT NULL DEFAULT 0,"
                + "CopsKillCount INTEGER NOT NULL DEFAULT 0,"
                + "PlayerKillCount INTEGER NOT NULL DEFAULT 0,"
                + "BalanceChallengeStatus INTEGER NOT NULL DEFAULT 0"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Call from onDisable
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("[DecemberChallenges] Error closing SQLite connection:");
                e.printStackTrace();
            }
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            init();
        }
        return connection;
    }

    public void ensurePlayerRow(UUID uuid) {
        String sql = "INSERT OR IGNORE INTO " + TABLE_NAME + " (UUID) VALUES (?);";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to ensure player row for " + uuid + ":");
            e.printStackTrace();
        }
    }

    // Generic helper to increment a column by 1, capped at max
    private void incrementColumn(UUID uuid, String column, int max) {
        ensurePlayerRow(uuid);
        String sql = "UPDATE " + TABLE_NAME + " "
                + "SET " + column + " = " + column + " + 1 "
                + "WHERE UUID = ? AND " + column + " < ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, max);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to increment " + column + " for " + uuid + ":");
            e.printStackTrace();
        }
    }

    // Increment methods

    public void incrementChestLoot(UUID uuid) {
        incrementColumn(uuid, "ChestLootCount", Challenges.CHEST_GOAL);
    }

    public void incrementCriminalKills(UUID uuid) {
        incrementColumn(uuid, "CriminalKillCount", Challenges.CRIMINAL_GOAL);
    }

    public void incrementCopsKills(UUID uuid) {
        incrementColumn(uuid, "CopsKillCount", Challenges.COPS_GOAL);
    }

    public void incrementPlayerKills(UUID uuid) {
        incrementColumn(uuid, "PlayerKillCount", Challenges.PLAYER_GOAL);
    }

    // Getters

    private int getInt(UUID uuid, String column) {
        ensurePlayerRow(uuid);
        String sql = "SELECT " + column + " FROM " + TABLE_NAME + " WHERE UUID = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(column);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to get " + column + " for " + uuid + ":");
            e.printStackTrace();
        }
        return 0;
    }

    public int getChestLootCount(UUID uuid) {
        return getInt(uuid, "ChestLootCount");
    }

    public int getCriminalKillCount(UUID uuid) {
        return getInt(uuid, "CriminalKillCount");
    }

    public int getCopsKillCount(UUID uuid) {
        return getInt(uuid, "CopsKillCount");
    }

    public int getPlayerKillCount(UUID uuid) {
        return getInt(uuid, "PlayerKillCount");
    }

    // Balance challenge flag

    public boolean isBalanceChallengeComplete(UUID uuid) {
        ensurePlayerRow(uuid);
        String sql = "SELECT BalanceChallengeStatus FROM " + TABLE_NAME + " WHERE UUID = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("BalanceChallengeStatus") == 1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to get BalanceChallengeStatus for " + uuid + ":");
            e.printStackTrace();
        }
        return false;
    }

    public void setBalanceChallengeComplete(UUID uuid, boolean complete) {
        ensurePlayerRow(uuid);
        String sql = "UPDATE " + TABLE_NAME + " SET BalanceChallengeStatus = ? WHERE UUID = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, complete ? 1 : 0);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to set BalanceChallengeStatus for " + uuid + ":");
            e.printStackTrace();
        }
    }

    // Reset methods

    public void resetChestLoot(UUID uuid) {
        setInt(uuid, "ChestLootCount", 0);
    }

    public void resetCriminalKills(UUID uuid) {
        setInt(uuid, "CriminalKillCount", 0);
    }

    public void resetCopsKills(UUID uuid) {
        setInt(uuid, "CopsKillCount", 0);
    }

    public void resetPlayerKills(UUID uuid) {
        setInt(uuid, "PlayerKillCount", 0);
    }

    private void setInt(UUID uuid, String column, int value) {
        ensurePlayerRow(uuid);
        String sql = "UPDATE " + TABLE_NAME + " SET " + column + " = ? WHERE UUID = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[DecemberChallenges] Failed to set " + column + " for " + uuid + ":");
            e.printStackTrace();
        }
    }

    // Boost / reduce

    public void boostProgress(UUID uuid, int challengeId, int amount) {
        if (amount <= 0) return;
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                boostColumn(uuid, "ChestLootCount", amount, Challenges.CHEST_GOAL);
                break;
            case Challenges.CRIMINAL_CHALLENGE_ID:
                boostColumn(uuid, "CriminalKillCount", amount, Challenges.CRIMINAL_GOAL);
                break;
            case Challenges.COPS_CHALLENGE_ID:
                boostColumn(uuid, "CopsKillCount", amount, Challenges.COPS_GOAL);
                break;
            case Challenges.PLAYER_CHALLENGE_ID:
                boostColumn(uuid, "PlayerKillCount", amount, Challenges.PLAYER_GOAL);
                break;
            default:
                // Balance is handled separately in Commands
                break;
        }
    }

    private void boostColumn(UUID uuid, String column, int amount, int max) {
        ensurePlayerRow(uuid);
        int current = getInt(uuid, column);
        int updated = current + amount;
        if (updated > max) updated = max;
        setInt(uuid, column, updated);
    }

    public void reduceProgress(UUID uuid, int challengeId, int amount) {
        if (amount <= 0) return;
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                reduceColumn(uuid, "ChestLootCount", amount);
                break;
            case Challenges.CRIMINAL_CHALLENGE_ID:
                reduceColumn(uuid, "CriminalKillCount", amount);
                break;
            case Challenges.COPS_CHALLENGE_ID:
                reduceColumn(uuid, "CopsKillCount", amount);
                break;
            case Challenges.PLAYER_CHALLENGE_ID:
                reduceColumn(uuid, "PlayerKillCount", amount);
                break;
            default:
                // Balance handled separately
                break;
        }
    }

    private void reduceColumn(UUID uuid, String column, int amount) {
        ensurePlayerRow(uuid);
        int current = getInt(uuid, column);
        int updated = current - amount;
        if (updated < 0) updated = 0;
        setInt(uuid, column, updated);
    }
}
