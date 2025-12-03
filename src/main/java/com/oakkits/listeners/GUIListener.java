package com.oakkits.listeners;

import com.oakkits.OakKits;
import com.oakkits.utils.ActionBarUtil;
import com.oakkits.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {
    
    private final OakKits plugin;
    
    public GUIListener(OakKits plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getGUIManager().isPreviewViewer(player.getUniqueId())) return;
        
        event.setCancelled(true);
        
        if (event.getRawSlot() == plugin.getGUIManager().getCloseSlot()) {
            player.closeInventory();
            return;
        }
        
        String message = plugin.getMessagesManager().getMessage("preview.cannot-take");
        ActionBarUtil.sendActionBar(player, message);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getGUIManager().isPreviewViewer(player.getUniqueId())) return;
        
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        plugin.getGUIManager().removePreviewViewer(player.getUniqueId());
    }
}
