package com.oakkits.managers;

import com.oakkits.OakKits;
import com.oakkits.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesManager {
    
    private final OakKits plugin;
    private FileConfiguration messages;
    private String prefix;
    
    public MessagesManager(OakKits plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = ColorUtil.colorize(messages.getString("prefix", "&8[&6OakKits&8] "));
    }
    
    public void reload() {
        loadMessages();
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getMessage(String path) {
        return ColorUtil.colorize(messages.getString(path, "&cMessage not found: " + path));
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ColorUtil.colorize(message);
    }
    
    public String getChatMessage(String path) {
        return prefix + getMessage(path + ".chat");
    }
    
    public String getChatMessage(String path, Map<String, String> placeholders) {
        return prefix + getMessage(path + ".chat", placeholders);
    }
    
    public String getActionBarMessage(String path) {
        return getMessage(path + ".actionbar");
    }
    
    public String getActionBarMessage(String path, Map<String, String> placeholders) {
        return getMessage(path + ".actionbar", placeholders);
    }
    
    public List<String> getStringList(String path) {
        return messages.getStringList(path);
    }
    
    public static Map<String, String> of(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
