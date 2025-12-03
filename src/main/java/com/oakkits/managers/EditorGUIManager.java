package com.oakkits.managers;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import com.oakkits.utils.ColorUtil;
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

public class EditorGUIManager {

    private final OakKits plugin;
    private FileConfiguration guiConfig;
    
    private final Map<UUID, String> editingKit = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentMenu = new ConcurrentHashMap<>();
    private final Map<UUID, Object> pendingInput = new ConcurrentHashMap<>();
    private final Set<UUID> suppressCloseCleanup = ConcurrentHashMap.newKeySet();
    
    private String mainTitle;
    private String itemsTitle;
    private String armorTitle;
    private String cooldownTitle;
    private String costTitle;
    private String permissionTitle;
    private String displayNameTitle;
    private String commandsTitle;
    
    private Material borderMaterial;
    private Material fillerMaterial;
    
    public EditorGUIManager(OakKits plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "editor-gui.yml");
        if (!file.exists()) {
            plugin.saveResource("editor-gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(file);
        
        mainTitle = guiConfig.getString("titles.main", "&8Kit Editor: &6{kit}");
        itemsTitle = guiConfig.getString("titles.items", "&8Edit Items: &6{kit}");
        armorTitle = guiConfig.getString("titles.armor", "&8Edit Armor: &6{kit}");
        cooldownTitle = guiConfig.getString("titles.cooldown", "&8Set Cooldown: &6{kit}");
        costTitle = guiConfig.getString("titles.cost", "&8Set Cost: &6{kit}");
        permissionTitle = guiConfig.getString("titles.permission", "&8Set Permission: &6{kit}");
        displayNameTitle = guiConfig.getString("titles.displayname", "&8Set Display Name: &6{kit}");
        commandsTitle = guiConfig.getString("titles.commands", "&8Edit Commands: &6{kit}");
        
        borderMaterial = Material.matchMaterial(guiConfig.getString("materials.border", "BLACK_STAINED_GLASS_PANE"));
        fillerMaterial = Material.matchMaterial(guiConfig.getString("materials.filler", "GRAY_STAINED_GLASS_PANE"));
        
        if (borderMaterial == null) borderMaterial = Material.BLACK_STAINED_GLASS_PANE;
        if (fillerMaterial == null) fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    }
    
    public void reload() {
        loadConfig();
    }
    
    public void openMainEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(mainTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 45, title);
        
        fillBorder(inv, 45);
        
        inv.setItem(10, createButton(
            getMaterial("buttons.items.material", "CHEST"),
            guiConfig.getString("buttons.items.name", "&6Items"),
            guiConfig.getStringList("buttons.items.lore")
        ));
        
        inv.setItem(12, createButton(
            getMaterial("buttons.commands.material", "COMMAND_BLOCK"),
            guiConfig.getString("buttons.commands.name", "&6Commands"),
            guiConfig.getStringList("buttons.commands.lore")
        ));
        
        inv.setItem(14, createButton(
            getMaterial("buttons.armor.material", "DIAMOND_CHESTPLATE"),
            guiConfig.getString("buttons.armor.name", "&6Armor"),
            guiConfig.getStringList("buttons.armor.lore")
        ));
        
        inv.setItem(16, createButton(
            getMaterial("buttons.cost.material", "GOLD_INGOT"),
            guiConfig.getString("buttons.cost.name", "&6Cost"),
            replacePlaceholders(guiConfig.getStringList("buttons.cost.lore"), kit)
        ));
        
        inv.setItem(28, createButton(
            getMaterial("buttons.cooldown.material", "CLOCK"),
            guiConfig.getString("buttons.cooldown.name", "&6Cooldown"),
            replacePlaceholders(guiConfig.getStringList("buttons.cooldown.lore"), kit)
        ));
        
        inv.setItem(30, createButton(
            getMaterial("buttons.permission.material", "IRON_BARS"),
            guiConfig.getString("buttons.permission.name", "&6Permission"),
            replacePlaceholders(guiConfig.getStringList("buttons.permission.lore"), kit)
        ));
        
        inv.setItem(32, createButton(
            getMaterial("buttons.displayname.material", "NAME_TAG"),
            guiConfig.getString("buttons.displayname.name", "&6Display Name"),
            replacePlaceholders(guiConfig.getStringList("buttons.displayname.lore"), kit)
        ));
        
        inv.setItem(34, createToggleButton(
            getMaterial("buttons.onetime.material", kit.isOneTime() ? "LIME_DYE" : "GRAY_DYE"),
            kit.isOneTime() ? "&aOne-Time: ON" : "&cOne-Time: OFF",
            guiConfig.getStringList("buttons.onetime.lore"),
            kit.isOneTime()
        ));
        
        inv.setItem(40, createButton(
            getMaterial("buttons.save.material", "LIME_WOOL"),
            guiConfig.getString("buttons.save.name", "&a&lSAVE"),
            guiConfig.getStringList("buttons.save.lore")
        ));
        
        inv.setItem(44, createButton(
            getMaterial("buttons.close.material", "BARRIER"),
            guiConfig.getString("buttons.close.name", "&c&lCLOSE"),
            guiConfig.getStringList("buttons.close.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "main");
        
        player.openInventory(inv);
    }
    
    public void openItemsEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(itemsTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        List<ItemStack> items = kit.getItems();
        for (int i = 0; i < Math.min(items.size(), 36); i++) {
            inv.setItem(i, items.get(i).clone());
        }
        
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, createFiller());
        }
        
        inv.setItem(45, createButton(
            getMaterial("buttons.import-inventory.material", "HOPPER"),
            guiConfig.getString("buttons.import-inventory.name", "&aImport from Inventory"),
            guiConfig.getStringList("buttons.import-inventory.lore")
        ));
        
        inv.setItem(49, createButton(
            getMaterial("buttons.clear-all.material", "TNT"),
            guiConfig.getString("buttons.clear-all.name", "&cClear All"),
            guiConfig.getStringList("buttons.clear-all.lore")
        ));
        
        inv.setItem(53, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "items");
        
        player.openInventory(inv);
    }
    
    public void openArmorEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(armorTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        fillBorder(inv, 27);
        
        inv.setItem(10, kit.getHelmet() != null ? kit.getHelmet().clone() : createEmptySlot("&7Helmet Slot"));
        inv.setItem(11, kit.getChestplate() != null ? kit.getChestplate().clone() : createEmptySlot("&7Chestplate Slot"));
        inv.setItem(12, kit.getLeggings() != null ? kit.getLeggings().clone() : createEmptySlot("&7Leggings Slot"));
        inv.setItem(13, kit.getBoots() != null ? kit.getBoots().clone() : createEmptySlot("&7Boots Slot"));
        
        inv.setItem(15, createButton(
            getMaterial("buttons.import-armor.material", "ARMOR_STAND"),
            guiConfig.getString("buttons.import-armor.name", "&aImport Worn Armor"),
            guiConfig.getStringList("buttons.import-armor.lore")
        ));
        
        inv.setItem(16, createButton(
            getMaterial("buttons.clear-armor.material", "TNT"),
            guiConfig.getString("buttons.clear-armor.name", "&cClear Armor"),
            guiConfig.getStringList("buttons.clear-armor.lore")
        ));
        
        inv.setItem(22, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "armor");
        
        player.openInventory(inv);
    }
    
    public void openCooldownEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(cooldownTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 45, title);
        
        fillBorder(inv, 45);
        
        inv.setItem(4, createInfoItem("&6&lCurrent Cooldown", Arrays.asList(
            "",
            "&f" + kit.getFormattedCooldown(),
            ""
        )));
        
        List<Map<?, ?>> addButtons = guiConfig.getMapList("cooldown-editor.add-buttons");
        for (Map<?, ?> btn : addButtons) {
            try {
                Object slotObj = btn.get("slot");
                if (slotObj == null) continue;
                int slot = ((Number) slotObj).intValue();
                String name = btn.get("name") != null ? btn.get("name").toString() : "&a&l+";
                String matName = btn.get("material") != null ? btn.get("material").toString() : "LIME_STAINED_GLASS_PANE";
                List<?> loreList = (List<?>) btn.get("lore");
                List<String> lore = new ArrayList<>();
                if (loreList != null) {
                    for (Object l : loreList) lore.add(l.toString());
                }
                Material mat = getSafeMaterial(matName, Material.LIME_STAINED_GLASS_PANE);
                inv.setItem(slot, createButton(mat, name, lore));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading cooldown add button: " + e.getMessage());
            }
        }
        
        List<Map<?, ?>> removeButtons = guiConfig.getMapList("cooldown-editor.remove-buttons");
        for (Map<?, ?> btn : removeButtons) {
            try {
                Object slotObj = btn.get("slot");
                if (slotObj == null) continue;
                int slot = ((Number) slotObj).intValue();
                String name = btn.get("name") != null ? btn.get("name").toString() : "&c&l-";
                String matName = btn.get("material") != null ? btn.get("material").toString() : "RED_STAINED_GLASS_PANE";
                List<?> loreList = (List<?>) btn.get("lore");
                List<String> lore = new ArrayList<>();
                if (loreList != null) {
                    for (Object l : loreList) lore.add(l.toString());
                }
                Material mat = getSafeMaterial(matName, Material.RED_STAINED_GLASS_PANE);
                inv.setItem(slot, createButton(mat, name, lore));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading cooldown remove button: " + e.getMessage());
            }
        }
        
        ConfigurationSection resetBtn = guiConfig.getConfigurationSection("cooldown-editor.reset-button");
        if (resetBtn != null) {
            int slot = resetBtn.getInt("slot", 39);
            String name = resetBtn.getString("name", "&e&lRESET");
            String matName = resetBtn.getString("material", "DANDELION");
            Material mat = getSafeMaterial(matName, Material.DANDELION);
            inv.setItem(slot, createButton(mat, name, resetBtn.getStringList("lore")));
        }
        
        inv.setItem(40, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "cooldown");
        
        player.openInventory(inv);
    }
    
    public void openCostEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(costTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 45, title);
        
        fillBorder(inv, 45);
        
        inv.setItem(4, createInfoItem("&6&lCurrent Cost", Arrays.asList(
            "",
            "&a$" + (int) kit.getCost(),
            ""
        )));
        
        List<Map<?, ?>> costAddButtons = guiConfig.getMapList("cost-editor.add-buttons");
        for (Map<?, ?> btn : costAddButtons) {
            try {
                Object slotObj = btn.get("slot");
                if (slotObj == null) continue;
                int slot = ((Number) slotObj).intValue();
                String name = btn.get("name") != null ? btn.get("name").toString() : "&a&l+";
                String matName = btn.get("material") != null ? btn.get("material").toString() : "GOLD_NUGGET";
                List<?> loreList = (List<?>) btn.get("lore");
                List<String> lore = new ArrayList<>();
                if (loreList != null) {
                    for (Object l : loreList) lore.add(l.toString());
                }
                Material mat = getSafeMaterial(matName, Material.GOLD_NUGGET);
                inv.setItem(slot, createButton(mat, name, lore));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading cost add button: " + e.getMessage());
            }
        }
        
        List<Map<?, ?>> costRemoveButtons = guiConfig.getMapList("cost-editor.remove-buttons");
        for (Map<?, ?> btn : costRemoveButtons) {
            try {
                Object slotObj = btn.get("slot");
                if (slotObj == null) continue;
                int slot = ((Number) slotObj).intValue();
                String name = btn.get("name") != null ? btn.get("name").toString() : "&c&l-";
                String matName = btn.get("material") != null ? btn.get("material").toString() : "IRON_NUGGET";
                List<?> loreList = (List<?>) btn.get("lore");
                List<String> lore = new ArrayList<>();
                if (loreList != null) {
                    for (Object l : loreList) lore.add(l.toString());
                }
                Material mat = getSafeMaterial(matName, Material.IRON_NUGGET);
                inv.setItem(slot, createButton(mat, name, lore));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading cost remove button: " + e.getMessage());
            }
        }
        
        ConfigurationSection freeBtn = guiConfig.getConfigurationSection("cost-editor.free-button");
        if (freeBtn != null) {
            int slot = freeBtn.getInt("slot", 39);
            String name = freeBtn.getString("name", "&e&lMAKE FREE");
            String matName = freeBtn.getString("material", "DANDELION");
            Material mat = getSafeMaterial(matName, Material.DANDELION);
            inv.setItem(slot, createButton(mat, name, freeBtn.getStringList("lore")));
        }
        
        inv.setItem(40, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "cost");
        
        player.openInventory(inv);
    }
    
    public void openDisplayNameEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(displayNameTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 45, title);
        
        fillBorder(inv, 45);
        
        inv.setItem(4, createInfoItem("&6&lCurrent Name", Arrays.asList(
            "",
            "&7Raw: &f" + kit.getDisplayName(),
            "&7Preview: " + ColorUtil.colorize(kit.getDisplayName()),
            ""
        )));
        
        ConfigurationSection chatBtn = guiConfig.getConfigurationSection("displayname-editor.chat-input-button");
        if (chatBtn != null) {
            int slot = chatBtn.getInt("slot", 13);
            String name = chatBtn.getString("name", "&e&lTYPE IN CHAT");
            String matName = chatBtn.getString("material", "WRITABLE_BOOK");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.WRITABLE_BOOK;
            inv.setItem(slot, createButton(mat, name, chatBtn.getStringList("lore")));
        } else {
            inv.setItem(13, createButton(Material.WRITABLE_BOOK, "&e&lTYPE IN CHAT", Arrays.asList(
                "", "&7Click to type a new", "&7display name in chat", "&7Use &c& &7for color codes!", ""
            )));
        }
        
        inv.setItem(28, createColorButton("&c", Material.RED_WOOL));
        inv.setItem(29, createColorButton("&6", Material.ORANGE_WOOL));
        inv.setItem(30, createColorButton("&e", Material.YELLOW_WOOL));
        inv.setItem(31, createColorButton("&a", Material.LIME_WOOL));
        inv.setItem(32, createColorButton("&b", Material.LIGHT_BLUE_WOOL));
        inv.setItem(33, createColorButton("&9", Material.BLUE_WOOL));
        inv.setItem(34, createColorButton("&5", Material.PURPLE_WOOL));
        
        inv.setItem(37, createColorButton("&d", Material.PINK_WOOL));
        inv.setItem(38, createColorButton("&f", Material.WHITE_WOOL));
        inv.setItem(39, createColorButton("&7", Material.LIGHT_GRAY_WOOL));
        inv.setItem(40, createColorButton("&8", Material.GRAY_WOOL));
        inv.setItem(41, createColorButton("&0", Material.BLACK_WOOL));
        inv.setItem(42, createColorButton("&l", Material.GOLD_BLOCK));
        inv.setItem(43, createColorButton("&n", Material.IRON_BLOCK));
        
        inv.setItem(44, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "displayname");
        
        player.openInventory(inv);
    }
    
    public void openPermissionEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(permissionTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        fillBorder(inv, 27);
        
        boolean isPublic = kit.getPermission() == null || kit.getPermission().isEmpty();
        String permDisplay = isPublic ? "&aNone (Public)" : "&f" + kit.getPermission();
        inv.setItem(4, createInfoItem("&6&lCurrent Permission", Arrays.asList("", permDisplay, "")));
        
        ConfigurationSection publicBtn = guiConfig.getConfigurationSection("permission-editor.public-button");
        if (publicBtn != null) {
            int slot = publicBtn.getInt("slot", 11);
            String name = isPublic ? publicBtn.getString("name-public", "&a&lPUBLIC KIT") : publicBtn.getString("name-private", "&e&lMAKE PUBLIC");
            String matName = isPublic ? publicBtn.getString("material-public", "LIME_DYE") : publicBtn.getString("material-private", "OAK_DOOR");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = isPublic ? Material.LIME_DYE : Material.OAK_DOOR;
            List<String> lore = new ArrayList<>();
            for (String line : publicBtn.getStringList("lore")) {
                lore.add(line.replace("{status}", isPublic ? "&a&lPUBLIC" : "&c&lPRIVATE"));
            }
            inv.setItem(slot, createButton(mat, name, lore));
        }
        
        ConfigurationSection customBtn = guiConfig.getConfigurationSection("permission-editor.custom-button");
        if (customBtn != null) {
            int slot = customBtn.getInt("slot", 13);
            String name = customBtn.getString("name", "&e&lCUSTOM PERMISSION");
            String matName = customBtn.getString("material", "PAPER");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.PAPER;
            inv.setItem(slot, createButton(mat, name, customBtn.getStringList("lore")));
        }
        
        ConfigurationSection resetBtn = guiConfig.getConfigurationSection("permission-editor.reset-button");
        if (resetBtn != null) {
            int slot = resetBtn.getInt("slot", 15);
            String name = resetBtn.getString("name", "&6&lRESET TO DEFAULT");
            String matName = resetBtn.getString("material", "TRIPWIRE_HOOK");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.TRIPWIRE_HOOK;
            List<String> lore = new ArrayList<>();
            for (String line : resetBtn.getStringList("lore")) {
                lore.add(line.replace("{kit}", kit.getId()));
            }
            inv.setItem(slot, createButton(mat, name, lore));
        }
        
        inv.setItem(22, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "permission");
        
        player.openInventory(inv);
    }
    
    public void openCommandsEditor(Player player, Kit kit) {
        String title = ColorUtil.colorize(commandsTitle.replace("{kit}", kit.getId()));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        fillBorder(inv, 54);
        
        List<String> commands = kit.getCommands();
        for (int i = 0; i < Math.min(commands.size(), 28); i++) {
            int slot = 10 + (i % 7) + ((i / 7) * 9);
            inv.setItem(slot, createCommandItem(commands.get(i), i));
        }
        
        inv.setItem(48, createButton(Material.WRITABLE_BOOK, "&aAdd Command", Arrays.asList(
            "&7Click to add a new",
            "&7command in chat.",
            "&7Use {player} placeholder."
        )));
        
        inv.setItem(50, createButton(Material.TNT, "&cClear All Commands", Collections.emptyList()));
        
        inv.setItem(53, createButton(
            getMaterial("buttons.back.material", "ARROW"),
            guiConfig.getString("buttons.back.name", "&7Back"),
            guiConfig.getStringList("buttons.back.lore")
        ));
        
        UUID uuid = player.getUniqueId();
        suppressNextClose(uuid);
        editingKit.put(uuid, kit.getId());
        currentMenu.put(uuid, "commands");
        
        player.openInventory(inv);
    }
    
    private void fillBorder(Inventory inv, int size) {
        ItemStack border = createBorder();
        ItemStack filler = createFiller();
        
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            } else {
                inv.setItem(i, filler);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(borderMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createFiller() {
        ItemStack item = new ItemStack(fillerMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createButton(Material material, String name, List<String> lore) {
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
    
    private ItemStack createToggleButton(Material material, String name, List<String> lore, boolean isOn) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            coloredLore.add(ColorUtil.colorize("&8━━━━━━━━━━━━━━━━"));
            if (lore != null) {
                for (String line : lore) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        coloredLore.add(ColorUtil.colorize(line));
                    }
                }
            }
            coloredLore.add("");
            coloredLore.add(ColorUtil.colorize("&7Status: " + (isOn ? "&a&lON" : "&c&lOFF")));
            coloredLore.add("");
            coloredLore.add(ColorUtil.colorize("&a▶ Click to toggle"));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createInfoItem(String name, List<String> lore) {
        return createButton(Material.BOOK, name, lore);
    }
    
    private ItemStack createEmptySlot(String name) {
        return createButton(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name, Collections.emptyList());
    }
    
    private ItemStack createTimeButton(String name, long milliseconds) {
        Material mat = milliseconds > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCostButton(String name, int amount) {
        Material mat = amount > 0 ? Material.GOLD_NUGGET : Material.IRON_NUGGET;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createColorButton(String code, Material wool) {
        ItemStack item = new ItemStack(wool);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(code + code.replace("&", "")));
            meta.setLore(Arrays.asList(ColorUtil.colorize("&7Click to add: &f" + code)));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCommandItem(String command, int index) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&e/" + command));
            meta.setLore(Arrays.asList(
                ColorUtil.colorize("&7Command #" + (index + 1)),
                "",
                ColorUtil.colorize("&cClick to remove")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private Material getMaterial(String path, String defaultMat) {
        String matName = guiConfig.getString(path, defaultMat);
        Material mat = Material.matchMaterial(matName);
        if (mat != null) return mat;
        
        mat = Material.matchMaterial(defaultMat);
        if (mat != null) return mat;
        
        return Material.STONE;
    }
    
    private Material getSafeMaterial(String matName, Material fallback) {
        if (matName == null || matName.isEmpty()) {
            return fallback;
        }
        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            plugin.getLogger().warning("[EditorGUI] Unknown material '" + matName + "', using fallback: " + fallback.name());
            return fallback;
        }
        return mat;
    }
    
    private List<String> replacePlaceholders(List<String> lore, Kit kit) {
        List<String> result = new ArrayList<>();
        for (String line : lore) {
            result.add(line
                .replace("{cooldown}", kit.getFormattedCooldown())
                .replace("{cost}", String.valueOf((int) kit.getCost()))
                .replace("{permission}", kit.getPermission())
                .replace("{displayname}", kit.getDisplayName())
            );
        }
        return result;
    }
    
    public String getEditingKit(UUID uuid) {
        return editingKit.get(uuid);
    }
    
    public String getCurrentMenu(UUID uuid) {
        return currentMenu.get(uuid);
    }
    
    public void clearSession(UUID uuid) {
        editingKit.remove(uuid);
        currentMenu.remove(uuid);
    }
    
    public void clearFullSession(UUID uuid) {
        editingKit.remove(uuid);
        currentMenu.remove(uuid);
        pendingInput.remove(uuid);
    }
    
    public void setPendingInput(UUID uuid, String type) {
        pendingInput.put(uuid, type);
    }
    
    public void prepareForChatInput(Player player, String inputType) {
        UUID uuid = player.getUniqueId();
        pendingInput.put(uuid, inputType);
        suppressCloseCleanup.add(uuid);
        Bukkit.getScheduler().runTask(plugin, player::closeInventory);
    }
    
    public String getPendingInput(UUID uuid) {
        Object input = pendingInput.get(uuid);
        return input != null ? input.toString() : null;
    }
    
    public void clearPendingInput(UUID uuid) {
        pendingInput.remove(uuid);
    }
    
    public boolean isEditing(UUID uuid) {
        return editingKit.containsKey(uuid);
    }
    
    public boolean hasPendingInput(UUID uuid) {
        return pendingInput.containsKey(uuid);
    }
    
    public void suppressNextClose(UUID uuid) {
        suppressCloseCleanup.add(uuid);
    }
    
    public boolean shouldSuppressClose(UUID uuid) {
        return suppressCloseCleanup.remove(uuid);
    }
    
    public long getCooldownTimeForSlot(int slot) {
        try {
            List<Map<?, ?>> addButtons = guiConfig.getMapList("cooldown-editor.add-buttons");
            for (Map<?, ?> btn : addButtons) {
                Object slotObj = btn.get("slot");
                Object timeObj = btn.get("time");
                if (slotObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'slot' in cooldown add-button config entry");
                    continue;
                }
                if (timeObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'time' in cooldown add-button at slot " + slotObj);
                    continue;
                }
                if (!(slotObj instanceof Number) || !(timeObj instanceof Number)) {
                    plugin.getLogger().warning("[EditorGUI] Invalid slot/time type in cooldown add-button config");
                    continue;
                }
                int btnSlot = ((Number) slotObj).intValue();
                if (btnSlot == slot) {
                    return ((Number) timeObj).longValue();
                }
            }
            
            List<Map<?, ?>> removeButtons = guiConfig.getMapList("cooldown-editor.remove-buttons");
            for (Map<?, ?> btn : removeButtons) {
                Object slotObj = btn.get("slot");
                Object timeObj = btn.get("time");
                if (slotObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'slot' in cooldown remove-button config entry");
                    continue;
                }
                if (timeObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'time' in cooldown remove-button at slot " + slotObj);
                    continue;
                }
                if (!(slotObj instanceof Number) || !(timeObj instanceof Number)) {
                    plugin.getLogger().warning("[EditorGUI] Invalid slot/time type in cooldown remove-button config");
                    continue;
                }
                int btnSlot = ((Number) slotObj).intValue();
                if (btnSlot == slot) {
                    return ((Number) timeObj).longValue();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EditorGUI] Error reading cooldown config for slot " + slot + ": " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getCooldownResetSlot() {
        return guiConfig.getInt("cooldown-editor.reset-button.slot", 39);
    }
    
    public int getCostAmountForSlot(int slot) {
        try {
            List<Map<?, ?>> addButtons = guiConfig.getMapList("cost-editor.add-buttons");
            for (Map<?, ?> btn : addButtons) {
                Object slotObj = btn.get("slot");
                Object amountObj = btn.get("amount");
                if (slotObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'slot' in cost add-button config entry");
                    continue;
                }
                if (amountObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'amount' in cost add-button at slot " + slotObj);
                    continue;
                }
                if (!(slotObj instanceof Number) || !(amountObj instanceof Number)) {
                    plugin.getLogger().warning("[EditorGUI] Invalid slot/amount type in cost add-button config");
                    continue;
                }
                int btnSlot = ((Number) slotObj).intValue();
                if (btnSlot == slot) {
                    return ((Number) amountObj).intValue();
                }
            }
            
            List<Map<?, ?>> removeButtons = guiConfig.getMapList("cost-editor.remove-buttons");
            for (Map<?, ?> btn : removeButtons) {
                Object slotObj = btn.get("slot");
                Object amountObj = btn.get("amount");
                if (slotObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'slot' in cost remove-button config entry");
                    continue;
                }
                if (amountObj == null) {
                    plugin.getLogger().warning("[EditorGUI] Missing 'amount' in cost remove-button at slot " + slotObj);
                    continue;
                }
                if (!(slotObj instanceof Number) || !(amountObj instanceof Number)) {
                    plugin.getLogger().warning("[EditorGUI] Invalid slot/amount type in cost remove-button config");
                    continue;
                }
                int btnSlot = ((Number) slotObj).intValue();
                if (btnSlot == slot) {
                    return ((Number) amountObj).intValue();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EditorGUI] Error reading cost config for slot " + slot + ": " + e.getMessage());
        }
        
        return 0;
    }
    
    public int getCostFreeSlot() {
        return guiConfig.getInt("cost-editor.free-button.slot", 39);
    }
    
    public int getPermissionPublicSlot() {
        return guiConfig.getInt("permission-editor.public-button.slot", 11);
    }
    
    public int getPermissionCustomSlot() {
        return guiConfig.getInt("permission-editor.custom-button.slot", 13);
    }
    
    public int getPermissionResetSlot() {
        return guiConfig.getInt("permission-editor.reset-button.slot", 15);
    }
    
    public int getDisplayNameChatSlot() {
        return guiConfig.getInt("displayname-editor.chat-input-button.slot", 13);
    }
    
    public String getMessage(String key) {
        return guiConfig.getString("messages." + key, "");
    }
}
