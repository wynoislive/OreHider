package com.wyno.orehider.analysis;

import com.wyno.orehider.OreHider;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final OreHider plugin;
    private Connection connection;

    public DatabaseManager(OreHider plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "data.db");
            if (!dataFolder.getParentFile().exists()) dataFolder.getParentFile().mkdirs();
            
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            
            try (Statement s = connection.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS mining_stats (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "stone_mined INTEGER DEFAULT 0, " +
                        "ores_mined INTEGER DEFAULT 0, " +
                        "alerts_triggered INTEGER DEFAULT 0)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to init SQLite", e);
        }
    }

    public void updateStats(UUID uuid, boolean isOre) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO mining_stats (uuid, stone_mined, ores_mined) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "stone_mined = stone_mined + ?, ores_mined = ores_mined + ?")) {
            
            int stone = isOre ? 0 : 1;
            int ore = isOre ? 1 : 0;
            
            ps.setString(1, uuid.toString());
            ps.setInt(2, stone);
            ps.setInt(3, ore);
            ps.setInt(4, stone);
            ps.setInt(5, ore);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getRatio(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT stone_mined, ores_mined FROM mining_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int stone = rs.getInt("stone_mined");
                int ores = rs.getInt("ores_mined");
                if (stone == 0) return ores > 0 ? 100.0 : 0.0;
                return (double) ores / (stone + ores) * 100.0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
    
    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException e) {}
    }
}