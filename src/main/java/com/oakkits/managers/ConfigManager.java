package com.oakkits.managers;

import com.oakkits.OakKits;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    
    private final OakKits plugin;
    private FileConfiguration config;
    
    private boolean denyIfFull;
    private boolean dropOnGround;
    private boolean autoEquipArmor;
    private boolean clearBeforeGive;
    private boolean claimSoundEnabled;
    private Sound claimSound;
    private float claimSoundVolume;
    private float claimSoundPitch;
    
    private boolean economyEnabled;
    private boolean refundOnFail;
    private String currencySymbol;
    
    private boolean saveOnQuit;
    private int globalCooldown;
    
    private boolean actionbarEnabled;
    private boolean titleEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private boolean broadcastEnabled;
    private double broadcastMinCost;
    
    private boolean coloredConsole;
    private boolean debug;
    private boolean logClaims;
    
    private boolean autoSaveEnabled;
    private int autoSaveInterval;
    
    private boolean checkUpdates;
    private boolean guiEnabled;
    private String defaultCooldown;
    private double defaultCost;
    
    public ConfigManager(OakKits plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        denyIfFull = config.getBoolean("inventory.deny-if-full", false);
        dropOnGround = config.getBoolean("inventory.drop-on-ground", true);
        autoEquipArmor = config.getBoolean("inventory.auto-equip-armor", true);
        clearBeforeGive = config.getBoolean("inventory.clear-before-give", false);
        claimSoundEnabled = config.getBoolean("inventory.claim-sound.enabled", true);
        String soundName = config.getString("inventory.claim-sound.sound", "ENTITY_PLAYER_LEVELUP");
        try {
            claimSound = Sound.valueOf(soundName);
        } catch (Exception e) {
            claimSound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        claimSoundVolume = (float) config.getDouble("inventory.claim-sound.volume", 1.0);
        claimSoundPitch = (float) config.getDouble("inventory.claim-sound.pitch", 1.0);
        
        economyEnabled = config.getBoolean("economy.enabled", true);
        refundOnFail = config.getBoolean("economy.refund-on-fail", true);
        currencySymbol = config.getString("economy.currency-symbol", "$");
        
        saveOnQuit = config.getBoolean("cooldown.save-on-quit", true);
        globalCooldown = config.getInt("cooldown.global-cooldown", 0);
        
        actionbarEnabled = config.getBoolean("notifications.actionbar-enabled", true);
        titleEnabled = config.getBoolean("notifications.title.enabled", false);
        titleFadeIn = config.getInt("notifications.title.fade-in", 10);
        titleStay = config.getInt("notifications.title.stay", 40);
        titleFadeOut = config.getInt("notifications.title.fade-out", 10);
        broadcastEnabled = config.getBoolean("notifications.broadcast.enabled", false);
        broadcastMinCost = config.getDouble("notifications.broadcast.min-cost", 1000);
        
        coloredConsole = config.getBoolean("console.colored", true);
        debug = config.getBoolean("console.debug", false);
        logClaims = config.getBoolean("console.log-claims", true);
        
        autoSaveEnabled = config.getBoolean("auto-save.enabled", true);
        autoSaveInterval = config.getInt("auto-save.interval", 5);
        
        checkUpdates = config.getBoolean("misc.check-updates", true);
        guiEnabled = config.getBoolean("misc.gui-enabled", true);
        defaultCooldown = config.getString("misc.default-cooldown", "0");
        defaultCost = config.getDouble("misc.default-cost", 0);
    }
    
    public void reload() {
        loadConfig();
    }
    
    public boolean isDenyIfFull() {
        return denyIfFull;
    }
    
    public boolean isDropOnGround() {
        return dropOnGround;
    }
    
    public boolean isAutoEquipArmor() {
        return autoEquipArmor;
    }
    
    public boolean isClearBeforeGive() {
        return clearBeforeGive;
    }
    
    public boolean isClaimSoundEnabled() {
        return claimSoundEnabled;
    }
    
    public Sound getClaimSound() {
        return claimSound;
    }
    
    public float getClaimSoundVolume() {
        return claimSoundVolume;
    }
    
    public float getClaimSoundPitch() {
        return claimSoundPitch;
    }
    
    public boolean isEconomyEnabled() {
        return economyEnabled;
    }
    
    public boolean isRefundOnFail() {
        return refundOnFail;
    }
    
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    public boolean isSaveOnQuit() {
        return saveOnQuit;
    }
    
    public int getGlobalCooldown() {
        return globalCooldown;
    }
    
    public boolean isActionbarEnabled() {
        return actionbarEnabled;
    }
    
    public boolean isTitleEnabled() {
        return titleEnabled;
    }
    
    public int getTitleFadeIn() {
        return titleFadeIn;
    }
    
    public int getTitleStay() {
        return titleStay;
    }
    
    public int getTitleFadeOut() {
        return titleFadeOut;
    }
    
    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }
    
    public double getBroadcastMinCost() {
        return broadcastMinCost;
    }
    
    public boolean isColoredConsole() {
        return coloredConsole;
    }
    
    public boolean isDebug() {
        return debug;
    }
    
    public boolean isLogClaims() {
        return logClaims;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }
    
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }
    
    public boolean isCheckUpdates() {
        return checkUpdates;
    }
    
    public boolean isGuiEnabled() {
        return guiEnabled;
    }
    
    public String getDefaultCooldown() {
        return defaultCooldown;
    }
    
    public double getDefaultCost() {
        return defaultCost;
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
