package com.oakkits.managers;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import com.oakkits.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitsManager {
    
    private final OakKits plugin;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private volatile File kitsFile;
    private volatile FileConfiguration kitsConfig;
    private final Object saveLock = new Object();
    
    public KitsManager(OakKits plugin) {
        this.plugin = plugin;
        loadKits();
    }
    
    public void loadKits() {
        kits.clear();
        
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        
        ConfigurationSection kitsSection = kitsConfig.getConfigurationSection("kits");
        if (kitsSection == null) return;
        
        for (String kitId : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitId);
            if (kitSection == null) continue;
            
            Kit kit = new Kit(kitId);
            kit.setDisplayName(kitSection.getString("display-name", kitId));
            kit.setPermission(kitSection.getString("permission", "oakkits.kit." + kitId));
            kit.setCooldown(kitSection.getString("cooldown", "0"));
            kit.setCost(kitSection.getDouble("cost", 0));
            kit.setOneTime(kitSection.getBoolean("one-time", false));
            kit.setCommands(kitSection.getStringList("commands"));
            
            List<Map<?, ?>> itemsList = kitSection.getMapList("items");
            for (Map<?, ?> itemMap : itemsList) {
                ItemStack item = parseItem(itemMap);
                if (item != null) {
                    kit.addItem(item);
                }
            }
            
            ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
            if (armorSection != null) {
                for (String slot : armorSection.getKeys(false)) {
                    ConfigurationSection armorPiece = armorSection.getConfigurationSection(slot);
                    if (armorPiece != null) {
                        ItemStack armor = parseArmorPiece(armorPiece);
                        if (armor != null) {
                            kit.setArmorPiece(slot, armor);
                        }
                    }
                }
            }
            
            kits.put(kitId.toLowerCase(), kit);
        }
        
        plugin.log("&aLoaded &e" + kits.size() + " &akits");
    }
    
    public void reload() {
        loadKits();
    }
    
    public Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }
    
    public Collection<Kit> getAllKits() {
        return Collections.unmodifiableCollection(kits.values());
    }
    
    public Set<String> getKitNames() {
        return Collections.unmodifiableSet(kits.keySet());
    }
    
    public List<Kit> getAvailableKits(Player player) {
        List<Kit> available = new ArrayList<>();
        for (Kit kit : kits.values()) {
            String perm = kit.getPermission();
            if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {
                available.add(kit);
            }
        }
        return available;
    }
    
    public boolean kitExists(String id) {
        return kits.containsKey(id.toLowerCase());
    }
    
    public void createKit(String id, String displayName, Player player) {
        Kit kit = new Kit(id);
        kit.setDisplayName(displayName);
        kit.setPermission("oakkits.kit." + id);
        
        PlayerInventory inv = player.getInventory();
        
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                kit.addItem(item.clone());
            }
        }
        
        if (inv.getHelmet() != null) kit.setArmorPiece("helmet", inv.getHelmet().clone());
        if (inv.getChestplate() != null) kit.setArmorPiece("chestplate", inv.getChestplate().clone());
        if (inv.getLeggings() != null) kit.setArmorPiece("leggings", inv.getLeggings().clone());
        if (inv.getBoots() != null) kit.setArmorPiece("boots", inv.getBoots().clone());
        
        kits.put(id.toLowerCase(), kit);
        saveKit(kit);
    }
    
    public void createKit(String id, Player player) {
        createKit(id, "&f" + id, player);
    }
    
    public void deleteKit(String id) {
        kits.remove(id.toLowerCase());
        kitsConfig.set("kits." + id.toLowerCase(), null);
        saveKitsConfig();
        plugin.getDataManager().clearKitData(id);
    }
    
    public void saveKit(Kit kit) {
        String path = "kits." + kit.getId();
        
        kitsConfig.set(path + ".display-name", kit.getDisplayName());
        kitsConfig.set(path + ".permission", kit.getPermission());
        kitsConfig.set(path + ".cooldown", kit.getFormattedCooldown());
        kitsConfig.set(path + ".cost", kit.getCost());
        kitsConfig.set(path + ".one-time", kit.isOneTime());
        kitsConfig.set(path + ".commands", kit.getCommands());
        
        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (ItemStack item : kit.getItems()) {
            itemsList.add(serializeItem(item));
        }
        kitsConfig.set(path + ".items", itemsList);
        
        Map<String, ItemStack> armor = kit.getArmor();
        for (Map.Entry<String, ItemStack> entry : armor.entrySet()) {
            kitsConfig.set(path + ".armor." + entry.getKey(), serializeItem(entry.getValue()));
        }
        
        saveKitsConfig();
    }
    
    private void saveKitsConfig() {
        synchronized (saveLock) {
            try {
                kitsConfig.save(kitsFile);
            } catch (IOException e) {
                plugin.log("&cFailed to save kits.yml: " + e.getMessage());
            }
        }
    }
    
    @SuppressWarnings("deprecation")
    private ItemStack parseItem(Map<?, ?> itemMap) {
        try {
            String materialName = String.valueOf(itemMap.get("material"));
            Material material = Material.matchMaterial(materialName);
            if (material == null) return null;
            
            int amount = itemMap.containsKey("amount") ? ((Number) itemMap.get("amount")).intValue() : 1;
            
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                if (itemMap.containsKey("name")) {
                    meta.setDisplayName(ColorUtil.colorize(String.valueOf(itemMap.get("name"))));
                }
                
                if (itemMap.containsKey("lore")) {
                    List<?> loreList = (List<?>) itemMap.get("lore");
                    List<String> coloredLore = new ArrayList<>();
                    for (Object line : loreList) {
                        coloredLore.add(ColorUtil.colorize(String.valueOf(line)));
                    }
                    meta.setLore(coloredLore);
                }
                
                item.setItemMeta(meta);
            }
            
            if (itemMap.containsKey("enchantments")) {
                Map<?, ?> enchants = (Map<?, ?>) itemMap.get("enchantments");
                for (Map.Entry<?, ?> entry : enchants.entrySet()) {
                    try {
                        Enchantment enchant = Enchantment.getByName(String.valueOf(entry.getKey()).toUpperCase());
                        if (enchant != null) {
                            item.addUnsafeEnchantment(enchant, ((Number) entry.getValue()).intValue());
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            return item;
        } catch (Exception e) {
            return null;
        }
    }
    
    @SuppressWarnings("deprecation")
    private ItemStack parseArmorPiece(ConfigurationSection section) {
        try {
            Material material = Material.matchMaterial(section.getString("material", "AIR"));
            if (material == null || material == Material.AIR) return null;
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                if (section.contains("name")) {
                    meta.setDisplayName(ColorUtil.colorize(section.getString("name")));
                }
                
                if (section.contains("lore")) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : section.getStringList("lore")) {
                        coloredLore.add(ColorUtil.colorize(line));
                    }
                    meta.setLore(coloredLore);
                }
                
                item.setItemMeta(meta);
            }
            
            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String key : enchants.getKeys(false)) {
                    try {
                        Enchantment enchant = Enchantment.getByName(key.toUpperCase());
                        if (enchant != null) {
                            item.addUnsafeEnchantment(enchant, enchants.getInt(key));
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            return item;
        } catch (Exception e) {
            return null;
        }
    }
    
    @SuppressWarnings("deprecation")
    private Map<String, Object> serializeItem(ItemStack item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", item.getType().name());
        map.put("amount", item.getAmount());
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                map.put("name", meta.getDisplayName().replace("§", "&"));
            }
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(line.replace("§", "&"));
                }
                map.put("lore", lore);
            }
        }
        
        if (!item.getEnchantments().isEmpty()) {
            Map<String, Integer> enchants = new LinkedHashMap<>();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                enchants.put(entry.getKey().getName(), entry.getValue());
            }
            map.put("enchantments", enchants);
        }
        
        return map;
    }
}
