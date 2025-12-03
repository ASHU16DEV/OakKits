package com.oakkits.managers;

import com.oakkits.OakKits;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataManager {
    
    private final OakKits plugin;
    private Connection connection;
    private final Object dbLock = new Object();
    
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> oneTimeUsed = new ConcurrentHashMap<>();
    
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private BukkitTask saveTask;
    private static final long SAVE_DELAY_TICKS = 100L;
    
    public DataManager(OakKits plugin) {
        this.plugin = plugin;
        initDatabase();
        loadData();
    }
    
    private void initDatabase() {
        synchronized (dbLock) {
            try {
                File dbFile = new File(plugin.getDataFolder(), "data.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(url);
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS cooldowns (" +
                        "uuid TEXT NOT NULL, " +
                        "kit_id TEXT NOT NULL, " +
                        "end_time INTEGER NOT NULL, " +
                        "PRIMARY KEY (uuid, kit_id)" +
                        ")");
                    
                    stmt.execute("CREATE TABLE IF NOT EXISTS one_time (" +
                        "uuid TEXT NOT NULL, " +
                        "kit_id TEXT NOT NULL, " +
                        "used INTEGER NOT NULL DEFAULT 1, " +
                        "PRIMARY KEY (uuid, kit_id)" +
                        ")");
                    
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_cooldowns_kit ON cooldowns(kit_id)");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_onetime_kit ON one_time(kit_id)");
                }
                
                plugin.log("&aSQLite database initialized!");
            } catch (SQLException e) {
                plugin.log("&cFailed to initialize SQLite: " + e.getMessage());
            }
        }
    }
    
    public void loadData() {
        cooldowns.clear();
        oneTimeUsed.clear();
        
        synchronized (dbLock) {
            if (connection == null) return;
            
            long now = System.currentTimeMillis();
            
            try {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT uuid, kit_id, end_time FROM cooldowns")) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String kitId = rs.getString("kit_id");
                        long endTime = rs.getLong("end_time");
                        if (endTime > now) {
                            cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitId, endTime);
                        }
                    }
                }
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT uuid, kit_id FROM one_time WHERE used = 1")) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String kitId = rs.getString("kit_id");
                        oneTimeUsed.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitId, true);
                    }
                }
                
            } catch (SQLException e) {
                plugin.log("&cFailed to load data: " + e.getMessage());
            }
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::cleanupExpiredCooldowns);
    }
    
    public void reload() {
        loadData();
    }
    
    public void saveData() {
        scheduleSave();
    }
    
    private void scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            if (saveTask != null) {
                saveTask.cancel();
            }
            saveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                saveDataNow();
                saveScheduled.set(false);
            }, SAVE_DELAY_TICKS);
        }
    }
    
    public void saveDataNow() {
        synchronized (dbLock) {
            if (connection == null) return;
            
            try {
                connection.setAutoCommit(false);
            
                try (PreparedStatement deleteCD = connection.prepareStatement("DELETE FROM cooldowns");
                     PreparedStatement deleteOT = connection.prepareStatement("DELETE FROM one_time")) {
                    deleteCD.executeUpdate();
                    deleteOT.executeUpdate();
                }
                
                try (PreparedStatement insertCD = connection.prepareStatement(
                        "INSERT INTO cooldowns (uuid, kit_id, end_time) VALUES (?, ?, ?)")) {
                    for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                        String uuidStr = entry.getKey().toString();
                        for (Map.Entry<String, Long> cooldown : entry.getValue().entrySet()) {
                            insertCD.setString(1, uuidStr);
                            insertCD.setString(2, cooldown.getKey());
                            insertCD.setLong(3, cooldown.getValue());
                            insertCD.addBatch();
                        }
                    }
                    insertCD.executeBatch();
                }
                
                try (PreparedStatement insertOT = connection.prepareStatement(
                        "INSERT INTO one_time (uuid, kit_id, used) VALUES (?, ?, 1)")) {
                    for (Map.Entry<UUID, Map<String, Boolean>> entry : oneTimeUsed.entrySet()) {
                        String uuidStr = entry.getKey().toString();
                        for (Map.Entry<String, Boolean> oneTime : entry.getValue().entrySet()) {
                            if (oneTime.getValue()) {
                                insertOT.setString(1, uuidStr);
                                insertOT.setString(2, oneTime.getKey());
                                insertOT.addBatch();
                            }
                        }
                    }
                    insertOT.executeBatch();
                }
                
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.log("&cFailed to save data: " + e.getMessage());
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {}
            }
        }
    }
    
    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        saveDataNow();
        
        synchronized (dbLock) {
            if (connection != null) {
                try {
                    connection.close();
                    connection = null;
                } catch (SQLException ignored) {}
            }
        }
    }
    
    public void setCooldown(UUID uuid, String kitId, long endTime) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitId.toLowerCase(), endTime);
        saveData();
    }
    
    public long getCooldownEnd(UUID uuid, String kitId) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        String kitIdLower = kitId.toLowerCase();
        return playerCooldowns.getOrDefault(kitIdLower, 0L);
    }
    
    public long getRemainingCooldown(UUID uuid, String kitId) {
        long endTime = getCooldownEnd(uuid, kitId);
        if (endTime == 0) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
    
    public boolean isOnCooldown(UUID uuid, String kitId) {
        return getRemainingCooldown(uuid, kitId) > 0;
    }
    
    public void resetCooldown(UUID uuid, String kitId) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(kitId.toLowerCase());
            saveData();
        }
    }
    
    public void setOneTimeUsed(UUID uuid, String kitId) {
        oneTimeUsed.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitId.toLowerCase(), true);
        saveData();
    }
    
    public boolean hasUsedOneTime(UUID uuid, String kitId) {
        Map<String, Boolean> playerOneTime = oneTimeUsed.get(uuid);
        if (playerOneTime == null) return false;
        String kitIdLower = kitId.toLowerCase();
        return playerOneTime.getOrDefault(kitIdLower, false);
    }
    
    public void resetOneTime(UUID uuid, String kitId) {
        Map<String, Boolean> playerOneTime = oneTimeUsed.get(uuid);
        if (playerOneTime != null) {
            playerOneTime.remove(kitId.toLowerCase());
            saveData();
        }
    }
    
    public void clearKitData(String kitId) {
        String kitIdLower = kitId.toLowerCase();
        
        for (Map<String, Long> playerCooldowns : cooldowns.values()) {
            playerCooldowns.remove(kitIdLower);
        }
        
        for (Map<String, Boolean> playerOneTime : oneTimeUsed.values()) {
            playerOneTime.remove(kitIdLower);
        }
        
        synchronized (dbLock) {
            if (connection != null) {
                try {
                    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM cooldowns WHERE kit_id = ?")) {
                        stmt.setString(1, kitIdLower);
                        stmt.executeUpdate();
                    }
                    try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM one_time WHERE kit_id = ?")) {
                        stmt.setString(1, kitIdLower);
                        stmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.log("&cFailed to clear kit data: " + e.getMessage());
                }
            }
        }
    }
    
    public void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        
        for (Map<String, Long> playerCooldowns : cooldowns.values()) {
            changed |= playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
        }
        
        if (changed) {
            saveData();
        }
        
        synchronized (dbLock) {
            if (connection != null) {
                try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM cooldowns WHERE end_time < ?")) {
                    stmt.setLong(1, now);
                    int deleted = stmt.executeUpdate();
                    if (deleted > 0) {
                        plugin.log("&7Cleaned up &e" + deleted + " &7expired cooldowns");
                    }
                } catch (SQLException ignored) {}
            }
        }
    }
}
