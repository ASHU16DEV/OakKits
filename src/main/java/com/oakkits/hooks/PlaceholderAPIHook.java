package com.oakkits.hooks;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import com.oakkits.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    
    private final OakKits plugin;
    
    public PlaceholderAPIHook(OakKits plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "oakkits";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        
        if (params.equalsIgnoreCase("total_kits")) {
            return String.valueOf(plugin.getKitsManager().getKitNames().size());
        }
        
        if (params.equalsIgnoreCase("available_kits")) {
            return String.valueOf(plugin.getKitsManager().getAvailableKits(player).size());
        }
        
        if (params.startsWith("kit_")) {
            String[] parts = params.substring(4).split("_", 2);
            if (parts.length < 2) return "";
            
            String kitId = parts[0];
            String property = parts[1];
            
            Kit kit = plugin.getKitsManager().getKit(kitId);
            if (kit == null) return "";
            
            switch (property.toLowerCase()) {
                case "cooldown":
                    long remaining = plugin.getDataManager().getRemainingCooldown(player.getUniqueId(), kitId);
                    return remaining > 0 ? TimeUtil.formatTime(remaining) : "Ready";
                case "cost":
                    return String.valueOf((int) kit.getCost());
                case "permission":
                    return kit.getPermission();
                case "available":
                    if (!player.hasPermission(kit.getPermission())) return "No";
                    if (kit.isOneTime() && plugin.getDataManager().hasUsedOneTime(player.getUniqueId(), kitId)) return "No";
                    if (plugin.getDataManager().isOnCooldown(player.getUniqueId(), kitId)) return "No";
                    return "Yes";
                case "displayname":
                    return kit.getDisplayName();
                case "onetime":
                    return kit.isOneTime() ? "Yes" : "No";
                case "has_permission":
                    return player.hasPermission(kit.getPermission()) ? "Yes" : "No";
                case "on_cooldown":
                    return plugin.getDataManager().isOnCooldown(player.getUniqueId(), kitId) ? "Yes" : "No";
                case "items_count":
                    return String.valueOf(kit.getItems().size());
                case "commands_count":
                    return String.valueOf(kit.getCommands().size());
            }
        }
        
        return null;
    }
}
