package com.oakkits.listeners;

import com.oakkits.OakKits;
import com.oakkits.managers.EditorGUIManager;
import com.oakkits.models.Kit;
import com.oakkits.utils.ActionBarUtil;
import com.oakkits.utils.ColorUtil;
import com.oakkits.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditorGUIListener implements Listener {
    
    private final OakKits plugin;
    
    public EditorGUIListener(OakKits plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (!editor.isEditing(uuid)) return;
        
        // ALWAYS cancel ALL events first - no item can ever be moved
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
        
        String kitId = editor.getEditingKit(uuid);
        Kit kit = plugin.getKitsManager().getKit(kitId);
        if (kit == null) {
            player.closeInventory();
            editor.clearFullSession(uuid);
            return;
        }
        
        String menu = editor.getCurrentMenu(uuid);
        int slot = event.getRawSlot();
        int inventorySize = event.getInventory().getSize();
        
        // Block any click outside the top inventory
        if (slot < 0 || slot >= inventorySize) {
            return;
        }
        
        // Check if click is on a filler or border (no sound, no action)
        ItemStack clickedItem = event.getCurrentItem();
        if (isFillerOrBorder(clickedItem)) {
            return;
        }
        
        // Play UI click sound for actual buttons only
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            playButtonSound(player);
        }
        
        switch (menu) {
            case "main":
                handleMainMenu(player, kit, slot);
                break;
            case "items":
                handleItemsMenu(player, kit, slot);
                break;
            case "armor":
                handleArmorMenu(player, kit, slot);
                break;
            case "cooldown":
                handleCooldownMenu(player, kit, slot);
                break;
            case "cost":
                handleCostMenu(player, kit, slot);
                break;
            case "displayname":
                handleDisplayNameMenu(player, kit, slot);
                break;
            case "permission":
                handlePermissionMenu(player, kit, slot);
                break;
            case "commands":
                handleCommandsMenu(player, kit, slot);
                break;
        }
    }
    
    private boolean isFillerOrBorder(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.equals(" ") || name.isEmpty()) {
                return true;
            }
            return false;
        }
        
        Material type = item.getType();
        if (type.name().contains("STAINED_GLASS_PANE")) {
            return true;
        }
        
        return false;
    }
    
    private void playButtonSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception e) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 0.5f, 1.0f);
            } catch (Exception ignored) {}
        }
    }
    
    private void handleMainMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        switch (slot) {
            case 10:
                editor.openItemsEditor(player, kit);
                break;
            case 12:
                editor.openCommandsEditor(player, kit);
                break;
            case 14:
                editor.openArmorEditor(player, kit);
                break;
            case 16:
                editor.openCostEditor(player, kit);
                break;
            case 28:
                editor.openCooldownEditor(player, kit);
                break;
            case 30:
                editor.openPermissionEditor(player, kit);
                break;
            case 32:
                editor.openDisplayNameEditor(player, kit);
                break;
            case 34:
                kit.setOneTime(!kit.isOneTime());
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&aOne-time use: " + (kit.isOneTime() ? "&aON" : "&cOFF")));
                ActionBarUtil.sendActionBar(player, "&aOne-time: " + (kit.isOneTime() ? "ON" : "OFF"));
                editor.openMainEditor(player, kit);
                break;
            case 40:
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&a✓ Kit saved successfully!"));
                ActionBarUtil.sendActionBar(player, "&a✓ Kit saved!");
                player.closeInventory();
                break;
            case 44:
                player.closeInventory();
                break;
        }
    }
    
    private void handleItemsMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        switch (slot) {
            case 45:
                kit.getItems().clear();
                for (int i = 0; i < 36; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        kit.addItem(item.clone());
                    }
                }
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&aItems imported from inventory!"));
                ActionBarUtil.sendActionBar(player, "&a✓ Items imported!");
                editor.openItemsEditor(player, kit);
                break;
            case 49:
                kit.getItems().clear();
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&cAll items cleared!"));
                ActionBarUtil.sendActionBar(player, "&c✓ Items cleared!");
                editor.openItemsEditor(player, kit);
                break;
            case 53:
                editor.openMainEditor(player, kit);
                break;
        }
    }
    
    private void handleArmorMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        switch (slot) {
            case 15:
                if (player.getInventory().getHelmet() != null)
                    kit.setArmorPiece("helmet", player.getInventory().getHelmet().clone());
                if (player.getInventory().getChestplate() != null)
                    kit.setArmorPiece("chestplate", player.getInventory().getChestplate().clone());
                if (player.getInventory().getLeggings() != null)
                    kit.setArmorPiece("leggings", player.getInventory().getLeggings().clone());
                if (player.getInventory().getBoots() != null)
                    kit.setArmorPiece("boots", player.getInventory().getBoots().clone());
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&aArmor imported!"));
                ActionBarUtil.sendActionBar(player, "&a✓ Armor imported!");
                editor.openArmorEditor(player, kit);
                break;
            case 16:
                kit.getArmor().clear();
                plugin.getKitsManager().saveKit(kit);
                player.sendMessage(ColorUtil.colorize("&cArmor cleared!"));
                ActionBarUtil.sendActionBar(player, "&c✓ Armor cleared!");
                editor.openArmorEditor(player, kit);
                break;
            case 22:
                editor.openMainEditor(player, kit);
                break;
        }
    }
    
    private void handleCooldownMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        long currentCooldown = kit.getCooldown();
        
        if (slot == 40) {
            editor.openMainEditor(player, kit);
            return;
        }
        
        if (slot == editor.getCooldownResetSlot()) {
            kit.setCooldown("0");
            plugin.getKitsManager().saveKit(kit);
            String msg = editor.getMessage("cooldown-reset");
            if (msg.isEmpty()) msg = "&a✓ Cooldown reset to 0!";
            player.sendMessage(ColorUtil.colorize(msg));
            ActionBarUtil.sendActionBar(player, "&a✓ Cooldown: 0");
            editor.openCooldownEditor(player, kit);
            return;
        }
        
        long change = editor.getCooldownTimeForSlot(slot);
        if (change == 0) return;
        
        long newCooldown = Math.max(0, currentCooldown + change);
        kit.setCooldownRaw(newCooldown);
        plugin.getKitsManager().saveKit(kit);
        
        String msg = editor.getMessage("cooldown-updated");
        if (msg.isEmpty()) msg = "&a✓ Cooldown: {cooldown}";
        player.sendMessage(ColorUtil.colorize(msg.replace("{cooldown}", kit.getFormattedCooldown())));
        ActionBarUtil.sendActionBar(player, "&a✓ " + kit.getFormattedCooldown());
        editor.openCooldownEditor(player, kit);
    }
    
    private void handleCostMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        double currentCost = kit.getCost();
        
        if (slot == 40) {
            editor.openMainEditor(player, kit);
            return;
        }
        
        if (slot == editor.getCostFreeSlot()) {
            kit.setCost(0);
            plugin.getKitsManager().saveKit(kit);
            String msg = editor.getMessage("cost-free");
            if (msg.isEmpty()) msg = "&a✓ Kit is now free!";
            player.sendMessage(ColorUtil.colorize(msg));
            ActionBarUtil.sendActionBar(player, "&a✓ Cost: $0");
            editor.openCostEditor(player, kit);
            return;
        }
        
        int change = editor.getCostAmountForSlot(slot);
        if (change == 0) return;
        
        double newCost = Math.max(0, currentCost + change);
        kit.setCost(newCost);
        plugin.getKitsManager().saveKit(kit);
        
        String msg = editor.getMessage("cost-updated");
        if (msg.isEmpty()) msg = "&a✓ Cost: ${cost}";
        player.sendMessage(ColorUtil.colorize(msg.replace("{cost}", String.valueOf((int) newCost))));
        ActionBarUtil.sendActionBar(player, "&a✓ $" + (int) newCost);
        editor.openCostEditor(player, kit);
    }
    
    private void handleDisplayNameMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (slot == editor.getDisplayNameChatSlot()) {
            editor.prepareForChatInput(player, "displayname");
            String msg = editor.getMessage("type-displayname");
            if (msg.isEmpty()) msg = "&e&l✎ &eType the new display name in chat:";
            player.sendMessage(ColorUtil.colorize(msg));
            String hint = editor.getMessage("type-displayname-hint");
            if (!hint.isEmpty()) player.sendMessage(ColorUtil.colorize(hint));
            return;
        }
        
        if (slot == 44) {
            editor.openMainEditor(player, kit);
            return;
        }
        
        String colorCode = getColorFromSlot(slot);
        if (colorCode != null) {
            String current = kit.getDisplayName();
            kit.setDisplayName(current + colorCode);
            plugin.getKitsManager().saveKit(kit);
            player.sendMessage(ColorUtil.colorize("&7Added: " + colorCode + colorCode.replace("&", "")));
            ActionBarUtil.sendActionBar(player, "&a✓ " + ColorUtil.colorize(kit.getDisplayName()));
            editor.openDisplayNameEditor(player, kit);
        }
    }
    
    private void handlePermissionMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (slot == editor.getPermissionPublicSlot()) {
            kit.setPermission("");
            plugin.getKitsManager().saveKit(kit);
            String msg = editor.getMessage("permission-public");
            if (msg.isEmpty()) msg = "&a✓ Kit is now public!";
            player.sendMessage(ColorUtil.colorize(msg));
            ActionBarUtil.sendActionBar(player, "&a✓ Public kit!");
            editor.openPermissionEditor(player, kit);
            return;
        }
        
        if (slot == editor.getPermissionCustomSlot()) {
            editor.prepareForChatInput(player, "permission");
            String msg = editor.getMessage("type-permission");
            if (msg.isEmpty()) msg = "&e&l✎ &eType the new permission in chat:";
            player.sendMessage(ColorUtil.colorize(msg));
            String hint = editor.getMessage("type-permission-hint");
            if (!hint.isEmpty()) player.sendMessage(ColorUtil.colorize(hint));
            return;
        }
        
        if (slot == editor.getPermissionResetSlot()) {
            kit.setPermission("oakkits.kit." + kit.getId());
            plugin.getKitsManager().saveKit(kit);
            String msg = editor.getMessage("permission-reset");
            if (msg.isEmpty()) msg = "&a✓ Permission reset to default!";
            player.sendMessage(ColorUtil.colorize(msg));
            ActionBarUtil.sendActionBar(player, "&a✓ Permission reset!");
            editor.openPermissionEditor(player, kit);
            return;
        }
        
        if (slot == 22) {
            editor.openMainEditor(player, kit);
        }
    }
    
    private void handleCommandsMenu(Player player, Kit kit, int slot) {
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (slot == 48) {
            editor.prepareForChatInput(player, "command");
            player.sendMessage(ColorUtil.colorize("&eType the command to add (without /):"));
            player.sendMessage(ColorUtil.colorize("&7Use &e{player} &7for player name. Type &c'cancel' &7to cancel."));
            return;
        }
        
        if (slot == 50) {
            kit.getCommands().clear();
            plugin.getKitsManager().saveKit(kit);
            player.sendMessage(ColorUtil.colorize("&cAll commands cleared!"));
            ActionBarUtil.sendActionBar(player, "&c✓ Commands cleared!");
            editor.openCommandsEditor(player, kit);
            return;
        }
        
        if (slot == 53) {
            editor.openMainEditor(player, kit);
            return;
        }
        
        int cmdIndex = getCommandIndex(slot);
        if (cmdIndex >= 0 && cmdIndex < kit.getCommands().size()) {
            String removed = kit.getCommands().remove(cmdIndex);
            plugin.getKitsManager().saveKit(kit);
            player.sendMessage(ColorUtil.colorize("&cRemoved command: /" + removed));
            ActionBarUtil.sendActionBar(player, "&c✓ Command removed!");
            editor.openCommandsEditor(player, kit);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (!editor.isEditing(player.getUniqueId())) return;
        
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (editor.shouldSuppressClose(uuid)) {
            return;
        }
        
        if (editor.isEditing(uuid) && !editor.hasPendingInput(uuid)) {
            editor.clearFullSession(uuid);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        EditorGUIManager editor = plugin.getEditorGUIManager();
        
        if (!editor.hasPendingInput(uuid)) return;
        
        event.setCancelled(true);
        
        String input = event.getMessage();
        String inputType = editor.getPendingInput(uuid);
        String kitId = editor.getEditingKit(uuid);
        
        editor.clearPendingInput(uuid);
        
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.colorize("&cInput cancelled."));
            Bukkit.getScheduler().runTask(plugin, () -> {
                Kit kit = plugin.getKitsManager().getKit(kitId);
                if (kit != null) {
                    editor.openMainEditor(player, kit);
                }
            });
            return;
        }
        
        Kit kit = plugin.getKitsManager().getKit(kitId);
        if (kit == null) {
            player.sendMessage(ColorUtil.colorize("&cKit not found!"));
            return;
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (inputType) {
                case "displayname":
                    kit.setDisplayName(input);
                    plugin.getKitsManager().saveKit(kit);
                    player.sendMessage(ColorUtil.colorize("&aDisplay name set to: " + input));
                    player.sendMessage(ColorUtil.colorize("&7Preview: " + ColorUtil.colorize(input)));
                    ActionBarUtil.sendActionBar(player, "&a✓ " + ColorUtil.colorize(input));
                    editor.openDisplayNameEditor(player, kit);
                    break;
                case "permission":
                    kit.setPermission(input);
                    plugin.getKitsManager().saveKit(kit);
                    player.sendMessage(ColorUtil.colorize("&aPermission set to: " + input));
                    ActionBarUtil.sendActionBar(player, "&a✓ Permission saved!");
                    editor.openPermissionEditor(player, kit);
                    break;
                case "command":
                    kit.getCommands().add(input);
                    plugin.getKitsManager().saveKit(kit);
                    player.sendMessage(ColorUtil.colorize("&aCommand added: /" + input));
                    ActionBarUtil.sendActionBar(player, "&a✓ Command added!");
                    editor.openCommandsEditor(player, kit);
                    break;
            }
        });
    }
    
    private String getColorFromSlot(int slot) {
        switch (slot) {
            case 28: return "&c";
            case 29: return "&6";
            case 30: return "&e";
            case 31: return "&a";
            case 32: return "&b";
            case 33: return "&9";
            case 34: return "&5";
            case 37: return "&d";
            case 38: return "&f";
            case 39: return "&7";
            case 40: return "&8";
            case 41: return "&0";
            case 42: return "&l";
            case 43: return "&n";
            default: return null;
        }
    }
    
    private int getCommandIndex(int slot) {
        if (slot < 10 || slot > 43) return -1;
        int row = (slot - 10) / 9;
        int col = (slot - 10) % 9;
        if (col > 6) return -1;
        return row * 7 + col;
    }
}
