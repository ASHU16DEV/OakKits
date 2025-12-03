package com.oakkits.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.oakkits.utils.ColorUtil;
import com.oakkits.utils.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Kit {
    
    private final String id;
    private volatile String displayName;
    private volatile String permission;
    private volatile long cooldown;
    private volatile double cost;
    private volatile boolean oneTime;
    private final List<ItemStack> items;
    private final Map<String, ItemStack> armor;
    private final List<String> commands;
    
    public Kit(String id) {
        this.id = id;
        this.displayName = id;
        this.permission = "oakkits.kit." + id;
        this.cooldown = 0;
        this.cost = 0;
        this.oneTime = false;
        this.items = new CopyOnWriteArrayList<>();
        this.armor = new ConcurrentHashMap<>();
        this.commands = new CopyOnWriteArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
    
    public void setCooldown(String cooldownStr) {
        this.cooldown = TimeUtil.parseTime(cooldownStr);
    }
    
    public void setCooldownRaw(long cooldownMs) {
        this.cooldown = cooldownMs;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public boolean isOneTime() {
        return oneTime;
    }
    
    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }
    
    public List<ItemStack> getItems() {
        return items;
    }
    
    public List<ItemStack> getItemsCopy() {
        return new ArrayList<>(items);
    }
    
    public void setItems(List<ItemStack> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }
    
    public void addItem(ItemStack item) {
        if (item != null) {
            this.items.add(item.clone());
        }
    }
    
    public Map<String, ItemStack> getArmor() {
        return armor;
    }
    
    public Map<String, ItemStack> getArmorCopy() {
        return new HashMap<>(armor);
    }
    
    public void setArmor(Map<String, ItemStack> armor) {
        this.armor.clear();
        if (armor != null) {
            this.armor.putAll(armor);
        }
    }
    
    public void setArmorPiece(String slot, ItemStack item) {
        if (slot != null && item != null) {
            this.armor.put(slot.toLowerCase(), item.clone());
        }
    }
    
    public ItemStack getHelmet() {
        return armor.get("helmet");
    }
    
    public ItemStack getChestplate() {
        return armor.get("chestplate");
    }
    
    public ItemStack getLeggings() {
        return armor.get("leggings");
    }
    
    public ItemStack getBoots() {
        return armor.get("boots");
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public List<String> getCommandsCopy() {
        return new ArrayList<>(commands);
    }
    
    public void setCommands(List<String> commands) {
        this.commands.clear();
        if (commands != null) {
            this.commands.addAll(commands);
        }
    }
    
    public List<ItemStack> getAllItems() {
        List<ItemStack> allItems = new ArrayList<>(items);
        if (armor.get("helmet") != null) allItems.add(armor.get("helmet"));
        if (armor.get("chestplate") != null) allItems.add(armor.get("chestplate"));
        if (armor.get("leggings") != null) allItems.add(armor.get("leggings"));
        if (armor.get("boots") != null) allItems.add(armor.get("boots"));
        return allItems;
    }
    
    public int getTotalSlots() {
        return items.size();
    }
    
    public String getFormattedCooldown() {
        return TimeUtil.formatTime(cooldown);
    }
    
    @SuppressWarnings("deprecation")
    public static ItemStack createItem(Material material, int amount, String name, List<String> lore, Map<String, Integer> enchantments) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ColorUtil.colorize(name));
            }
            
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
        }
        
        if (enchantments != null) {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                try {
                    Enchantment enchant = Enchantment.getByName(entry.getKey().toUpperCase());
                    if (enchant != null) {
                        item.addUnsafeEnchantment(enchant, entry.getValue());
                    }
                } catch (Exception ignored) {}
            }
        }
        
        return item;
    }
}
