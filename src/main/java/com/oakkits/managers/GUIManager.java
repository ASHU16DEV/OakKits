package com.oakkits.managers;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import com.oakkits.utils.ColorUtil;
import com.oakkits.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager {
    
    private final OakKits plugin;
    private FileConfiguration guiConfig;
    
    private String previewTitle;
    private int previewSize;
    private boolean borderEnabled;
    private Material borderMaterial;
    private String borderName;
    private List<Integer> borderSlots;
    private boolean fillerEnabled;
    private Material fillerMaterial;
    private String fillerName;
    private boolean infoEnabled;
    private int infoSlot;
    private ItemStack infoItem;
    private boolean closeEnabled;
    private int closeSlot;
    private ItemStack closeItem;
    private int itemsStartSlot;
    private Map<String, Integer> armorSlots;
    
    private final Set<UUID> previewViewers = ConcurrentHashMap.newKeySet();
    
    public GUIManager(OakKits plugin) {
        this.plugin = plugin;
        loadGUI();
    }
    
    public void loadGUI() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        ConfigurationSection preview = guiConfig.getConfigurationSection("preview");
        if (preview == null) return;
        
        previewTitle = preview.getString("title", "&8✦ &6Kit Preview: &e{kit} &8✦");
        previewSize = preview.getInt("size", 54);
        
        ConfigurationSection border = preview.getConfigurationSection("border");
        if (border != null) {
            borderEnabled = border.getBoolean("enabled", true);
            borderMaterial = Material.matchMaterial(border.getString("material", "BLACK_STAINED_GLASS_PANE"));
            if (borderMaterial == null) borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
            borderName = border.getString("name", "&8");
            borderSlots = border.getIntegerList("slots");
        }
        
        ConfigurationSection filler = preview.getConfigurationSection("filler");
        if (filler != null) {
            fillerEnabled = filler.getBoolean("enabled", true);
            fillerMaterial = Material.matchMaterial(filler.getString("material", "GRAY_STAINED_GLASS_PANE"));
            if (fillerMaterial == null) fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
            fillerName = filler.getString("name", "&7");
        }
        
        ConfigurationSection info = preview.getConfigurationSection("info-item");
        if (info != null) {
            infoEnabled = info.getBoolean("enabled", true);
            infoSlot = info.getInt("slot", 49);
            Material infoMat = Material.matchMaterial(info.getString("material", "PAPER"));
            if (infoMat == null) infoMat = Material.PAPER;
            infoItem = createConfigItem(infoMat, info.getString("name", "&e&lKit Information"), info.getStringList("lore"));
        }
        
        ConfigurationSection close = preview.getConfigurationSection("close-button");
        if (close != null) {
            closeEnabled = close.getBoolean("enabled", true);
            closeSlot = close.getInt("slot", 53);
            Material closeMat = Material.matchMaterial(close.getString("material", "BARRIER"));
            if (closeMat == null) closeMat = Material.BARRIER;
            closeItem = createConfigItem(closeMat, close.getString("name", "&c&lClose Preview"), close.getStringList("lore"));
        }
        
        itemsStartSlot = preview.getInt("items-start-slot", 9);
        
        armorSlots = new HashMap<>();
        ConfigurationSection armorSection = preview.getConfigurationSection("armor-slots");
        if (armorSection != null) {
            armorSlots.put("helmet", armorSection.getInt("helmet", 36));
            armorSlots.put("chestplate", armorSection.getInt("chestplate", 37));
            armorSlots.put("leggings", armorSection.getInt("leggings", 38));
            armorSlots.put("boots", armorSection.getInt("boots", 39));
        }
    }
    
    public void reload() {
        loadGUI();
    }
    
    public void openPreview(Player player, Kit kit) {
        String title = ColorUtil.colorize(previewTitle.replace("{kit}", kit.getDisplayName()));
        Inventory inv = Bukkit.createInventory(null, previewSize, title);
        
        if (fillerEnabled) {
            ItemStack fillerItem = createFillerItem(fillerMaterial, fillerName);
            for (int i = 0; i < previewSize; i++) {
                inv.setItem(i, fillerItem);
            }
        }
        
        if (borderEnabled) {
            ItemStack borderItem = createFillerItem(borderMaterial, borderName);
            for (int slot : borderSlots) {
                if (slot >= 0 && slot < previewSize) {
                    inv.setItem(slot, borderItem);
                }
            }
        }
        
        int slot = itemsStartSlot;
        for (ItemStack item : kit.getItems()) {
            if (slot < previewSize && !borderSlots.contains(slot)) {
                inv.setItem(slot, item.clone());
                slot++;
                while (borderSlots.contains(slot) && slot < previewSize) {
                    slot++;
                }
            }
        }
        
        if (kit.getHelmet() != null && armorSlots.containsKey("helmet")) {
            inv.setItem(armorSlots.get("helmet"), kit.getHelmet().clone());
        }
        if (kit.getChestplate() != null && armorSlots.containsKey("chestplate")) {
            inv.setItem(armorSlots.get("chestplate"), kit.getChestplate().clone());
        }
        if (kit.getLeggings() != null && armorSlots.containsKey("leggings")) {
            inv.setItem(armorSlots.get("leggings"), kit.getLeggings().clone());
        }
        if (kit.getBoots() != null && armorSlots.containsKey("boots")) {
            inv.setItem(armorSlots.get("boots"), kit.getBoots().clone());
        }
        
        if (infoEnabled && infoItem != null) {
            ItemStack info = infoItem.clone();
            ItemMeta meta = info.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    line = line.replace("{kit}", ColorUtil.stripColor(kit.getDisplayName()));
                    line = line.replace("{cooldown}", kit.getFormattedCooldown());
                    line = line.replace("{cost}", String.valueOf(kit.getCost()));
                    lore.add(ColorUtil.colorize(line));
                }
                meta.setLore(lore);
                info.setItemMeta(meta);
            }
            inv.setItem(infoSlot, info);
        }
        
        if (closeEnabled && closeItem != null) {
            inv.setItem(closeSlot, closeItem.clone());
        }
        
        previewViewers.add(player.getUniqueId());
        player.openInventory(inv);
    }
    
    public boolean isPreviewViewer(UUID uuid) {
        return previewViewers.contains(uuid);
    }
    
    public void removePreviewViewer(UUID uuid) {
        previewViewers.remove(uuid);
    }
    
    public int getCloseSlot() {
        return closeSlot;
    }
    
    private ItemStack createFillerItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createConfigItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
