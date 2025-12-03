package com.oakkits;

import com.oakkits.commands.KitCommand;
import com.oakkits.commands.KitTabCompleter;
import com.oakkits.hooks.PlaceholderAPIHook;
import com.oakkits.listeners.EditorGUIListener;
import com.oakkits.listeners.GUIListener;
import com.oakkits.managers.*;
import com.oakkits.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OakKits extends JavaPlugin {
    
    private static OakKits instance;
    
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private GUIManager guiManager;
    private KitsManager kitsManager;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private BedrockManager bedrockManager;
    private EditorGUIManager editorGUIManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        long startTime = System.currentTimeMillis();
        
        printStartupBanner();
        
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        log("&7Loading configurations...");
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        guiManager = new GUIManager(this);
        kitsManager = new KitsManager(this);
        dataManager = new DataManager(this);
        
        log("&7Setting up economy...");
        economyManager = new EconomyManager(this);
        
        log("&7Setting up Bedrock support...");
        bedrockManager = new BedrockManager(this);
        
        log("&7Setting up Editor GUI...");
        editorGUIManager = new EditorGUIManager(this);
        
        log("&7Registering commands...");
        registerCommands();
        
        log("&7Registering listeners...");
        registerListeners();
        
        log("&7Setting up PlaceholderAPI...");
        setupPlaceholderAPI();
        
        dataManager.cleanupExpiredCooldowns();
        
        long loadTime = System.currentTimeMillis() - startTime;
        
        log("");
        log("&a✓ Plugin enabled successfully!");
        log("&7  Load time: &e" + loadTime + "ms");
        log("&7  Kits loaded: &e" + kitsManager.getKitNames().size());
        log("&7  Economy: " + (economyManager.isEnabled() ? "&aEnabled" : "&cDisabled"));
        log("&7  Bedrock: " + (bedrockManager.isFloodgateEnabled() ? "&aEnabled" : "&eDisabled"));
        log("&7  PlaceholderAPI: " + (placeholderAPIEnabled ? "&aEnabled" : "&eDisabled"));
        log("");
        printFooter();
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
        
        log("");
        log("&c✗ Plugin disabled!");
        log("");
        
        instance = null;
    }
    
    private void registerCommands() {
        PluginCommand kitCommand = getCommand("kit");
        if (kitCommand != null) {
            KitCommand executor = new KitCommand(this);
            KitTabCompleter tabCompleter = new KitTabCompleter(this);
            
            kitCommand.setExecutor(executor);
            kitCommand.setTabCompleter(tabCompleter);
        }
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EditorGUIListener(this), this);
    }
    
    private boolean placeholderAPIEnabled = false;
    
    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            placeholderAPIEnabled = true;
            log("&aPlaceholderAPI hooked! Placeholders registered.");
        } else {
            log("&ePlaceholderAPI not found. Placeholders disabled.");
        }
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    public void reloadAllConfigs() {
        log("&7Reloading all configurations...");
        
        configManager.reload();
        messagesManager.reload();
        guiManager.reload();
        kitsManager.reload();
        dataManager.reload();
        bedrockManager.reload();
        editorGUIManager.reload();
        
        log("&a✓ All configurations reloaded!");
    }
    
    private void printStartupBanner() {
        log("");
        log("&6  ██████╗  █████╗ ██╗  ██╗██╗  ██╗██╗████████╗███████╗");
        log("&6 ██╔═══██╗██╔══██╗██║ ██╔╝██║ ██╔╝██║╚══██╔══╝██╔════╝");
        log("&6 ██║   ██║███████║█████╔╝ █████╔╝ ██║   ██║   ███████╗");
        log("&6 ██║   ██║██╔══██║██╔═██╗ ██╔═██╗ ██║   ██║   ╚════██║");
        log("&6 ╚██████╔╝██║  ██║██║  ██╗██║  ██╗██║   ██║   ███████║");
        log("&6  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝   ╚═╝   ╚══════╝");
        log("");
        log("&7  Version: &e" + getDescription().getVersion() + " &7| Author: &e" + getDescription().getAuthors().get(0));
        log("&7  Minecraft: &e1.16.5 - 1.21.10 &7| API: &eSpigot/Paper");
        log("&7  GeyserMC: &eSupported &7| Bedrock Forms: &eEnabled");
        log("");
    }
    
    private void printFooter() {
        log("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    public void log(String message) {
        if (configManager != null && configManager.isColoredConsole()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&8[&6OakKits&8] " + message));
        } else {
            getLogger().info(ColorUtil.stripColor(message));
        }
    }
    
    public void debug(String message) {
        if (configManager != null && configManager.isDebug()) {
            log("&7[DEBUG] " + message);
        }
    }
    
    public static OakKits getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public KitsManager getKitsManager() {
        return kitsManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public BedrockManager getBedrockManager() {
        return bedrockManager;
    }
    
    public EditorGUIManager getEditorGUIManager() {
        return editorGUIManager;
    }
}
