package com.oakkits.managers;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import com.oakkits.utils.ColorUtil;
import com.oakkits.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class BedrockManager {
    
    private final OakKits plugin;
    private boolean floodgateEnabled;
    private Object floodgateApi;
    private Method isFloodgatePlayerMethod;
    private Method sendFormMethod;
    
    private boolean bedrockEnabled;
    private boolean useForms;
    private String formTitle;
    private boolean showKitInfo;
    private boolean mobileFriendlyMessages;
    
    public BedrockManager(OakKits plugin) {
        this.plugin = plugin;
        loadConfig();
        setupFloodgate();
    }
    
    public void loadConfig() {
        bedrockEnabled = plugin.getConfigManager().getConfig().getBoolean("bedrock.enabled", true);
        useForms = plugin.getConfigManager().getConfig().getBoolean("bedrock.use-forms", true);
        formTitle = plugin.getConfigManager().getConfig().getString("bedrock.form-title", "OakKits");
        showKitInfo = plugin.getConfigManager().getConfig().getBoolean("bedrock.show-kit-info-in-form", true);
        mobileFriendlyMessages = plugin.getConfigManager().getConfig().getBoolean("bedrock.mobile-friendly-messages", true);
    }
    
    private void setupFloodgate() {
        if (!bedrockEnabled) {
            plugin.log("&7Bedrock support disabled in config");
            return;
        }
        
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            plugin.log("&eFloodgate not found. Bedrock Forms disabled.");
            plugin.log("&7Install Floodgate for Bedrock player support.");
            floodgateEnabled = false;
            return;
        }
        
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstanceMethod = floodgateApiClass.getMethod("getInstance");
            floodgateApi = getInstanceMethod.invoke(null);
            isFloodgatePlayerMethod = floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class);
            sendFormMethod = floodgateApiClass.getMethod("sendForm", UUID.class, Object.class);
            
            floodgateEnabled = true;
            plugin.log("&aFloodgate hooked! Bedrock Forms enabled.");
        } catch (Exception e) {
            plugin.log("&eFailed to hook Floodgate: " + e.getMessage());
            floodgateEnabled = false;
        }
    }
    
    public void reload() {
        loadConfig();
        setupFloodgate();
    }
    
    public boolean isFloodgateEnabled() {
        return floodgateEnabled && bedrockEnabled;
    }
    
    public boolean isBedrockPlayer(Player player) {
        if (!floodgateEnabled || player == null) return false;
        
        try {
            return (boolean) isFloodgatePlayerMethod.invoke(floodgateApi, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean shouldUseForms() {
        return floodgateEnabled && useForms;
    }
    
    public boolean shouldUseMobileFriendlyMessages() {
        return mobileFriendlyMessages;
    }
    
    public void openKitSelectionForm(Player player, List<Kit> availableKits) {
        if (!shouldUseForms() || !isBedrockPlayer(player)) return;
        
        try {
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Class<?> simpleFormBuilderClass = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder");
            
            Method builderMethod = simpleFormClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            Method titleMethod = simpleFormBuilderClass.getMethod("title", String.class);
            builder = titleMethod.invoke(builder, formTitle);
            
            Method contentMethod = simpleFormBuilderClass.getMethod("content", String.class);
            builder = contentMethod.invoke(builder, "Select a kit to claim:");
            
            Method buttonMethod = simpleFormBuilderClass.getMethod("button", String.class);
            
            UUID uuid = player.getUniqueId();
            
            for (Kit kit : availableKits) {
                StringBuilder buttonText = new StringBuilder();
                buttonText.append(ColorUtil.stripColor(kit.getDisplayName()));
                
                if (showKitInfo) {
                    buttonText.append("\n");
                    
                    if (kit.isOneTime() && plugin.getDataManager().hasUsedOneTime(uuid, kit.getId())) {
                        buttonText.append("USED");
                    } else if (plugin.getDataManager().isOnCooldown(uuid, kit.getId())) {
                        long remaining = plugin.getDataManager().getRemainingCooldown(uuid, kit.getId());
                        buttonText.append("Cooldown: ").append(TimeUtil.formatTimeShort(remaining));
                    } else {
                        buttonText.append("READY");
                        if (kit.getCost() > 0) {
                            buttonText.append(" - $").append((int) kit.getCost());
                        }
                    }
                }
                
                builder = buttonMethod.invoke(builder, buttonText.toString());
            }
            
            final List<Kit> kits = availableKits;
            
            Class<?> formResponseHandlerClass = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse");
            
            Method validResultHandlerMethod = simpleFormBuilderClass.getMethod("validResultHandler", 
                Class.forName("java.util.function.BiConsumer"));
            
            builder = validResultHandlerMethod.invoke(builder, 
                (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                    try {
                        Method clickedButtonIdMethod = response.getClass().getMethod("clickedButtonId");
                        int buttonId = (int) clickedButtonIdMethod.invoke(response);
                        
                        if (buttonId >= 0 && buttonId < kits.size()) {
                            Kit selectedKit = kits.get(buttonId);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.performCommand("kit " + selectedKit.getId());
                            });
                        }
                    } catch (Exception e) {
                        plugin.debug("Form response error: " + e.getMessage());
                    }
                });
            
            Method buildMethod = simpleFormBuilderClass.getMethod("build");
            Object form = buildMethod.invoke(builder);
            
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);
            
        } catch (Exception e) {
            plugin.debug("Failed to open Bedrock form: " + e.getMessage());
            showChatKitList(player, availableKits);
        }
    }
    
    public void openKitPreviewForm(Player player, Kit kit) {
        if (!shouldUseForms() || !isBedrockPlayer(player)) return;
        
        try {
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Class<?> simpleFormBuilderClass = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder");
            
            Method builderMethod = simpleFormClass.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            
            Method titleMethod = simpleFormBuilderClass.getMethod("title", String.class);
            builder = titleMethod.invoke(builder, "Kit Preview: " + ColorUtil.stripColor(kit.getDisplayName()));
            
            StringBuilder content = new StringBuilder();
            content.append("=== KIT INFO ===\n");
            content.append("Cooldown: ").append(kit.getFormattedCooldown()).append("\n");
            content.append("Cost: $").append((int) kit.getCost()).append("\n");
            content.append("One-Time: ").append(kit.isOneTime() ? "Yes" : "No").append("\n\n");
            
            content.append("=== ITEMS ===\n");
            for (org.bukkit.inventory.ItemStack item : kit.getItems()) {
                content.append("- ").append(item.getType().name().replace("_", " "));
                content.append(" x").append(item.getAmount()).append("\n");
            }
            
            if (!kit.getArmor().isEmpty()) {
                content.append("\n=== ARMOR ===\n");
                if (kit.getHelmet() != null) content.append("- Helmet: ").append(kit.getHelmet().getType().name().replace("_", " ")).append("\n");
                if (kit.getChestplate() != null) content.append("- Chestplate: ").append(kit.getChestplate().getType().name().replace("_", " ")).append("\n");
                if (kit.getLeggings() != null) content.append("- Leggings: ").append(kit.getLeggings().getType().name().replace("_", " ")).append("\n");
                if (kit.getBoots() != null) content.append("- Boots: ").append(kit.getBoots().getType().name().replace("_", " ")).append("\n");
            }
            
            Method contentMethod = simpleFormBuilderClass.getMethod("content", String.class);
            builder = contentMethod.invoke(builder, content.toString());
            
            Method buttonMethod = simpleFormBuilderClass.getMethod("button", String.class);
            builder = buttonMethod.invoke(builder, "Claim Kit");
            builder = buttonMethod.invoke(builder, "Close");
            
            final Kit selectedKit = kit;
            
            Method validResultHandlerMethod = simpleFormBuilderClass.getMethod("validResultHandler", 
                Class.forName("java.util.function.BiConsumer"));
            
            builder = validResultHandlerMethod.invoke(builder, 
                (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                    try {
                        Method clickedButtonIdMethod = response.getClass().getMethod("clickedButtonId");
                        int buttonId = (int) clickedButtonIdMethod.invoke(response);
                        
                        if (buttonId == 0) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.performCommand("kit " + selectedKit.getId());
                            });
                        }
                    } catch (Exception e) {
                        plugin.debug("Form response error: " + e.getMessage());
                    }
                });
            
            Method buildMethod = simpleFormBuilderClass.getMethod("build");
            Object form = buildMethod.invoke(builder);
            
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);
            
        } catch (Exception e) {
            plugin.debug("Failed to open Bedrock preview form: " + e.getMessage());
        }
    }
    
    private void showChatKitList(Player player, List<Kit> kits) {
        player.sendMessage(ColorUtil.colorize("&6Available Kits:"));
        for (Kit kit : kits) {
            player.sendMessage(ColorUtil.colorize("&7- &e" + kit.getId() + " &7(/kit " + kit.getId() + ")"));
        }
    }
    
    public String getMobileFriendlyMessage(String message) {
        if (!mobileFriendlyMessages) return message;
        return message
            .replace("━", "-")
            .replace("✦", "*")
            .replace("✓", "[OK]")
            .replace("✗", "[X]")
            .replace("⏱", "[TIME]")
            .replace("⚠", "[!]")
            .replace("•", "-");
    }
}
