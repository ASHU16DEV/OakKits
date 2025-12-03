package com.oakkits.commands;

import com.oakkits.OakKits;
import com.oakkits.managers.MessagesManager;
import com.oakkits.models.Kit;
import com.oakkits.utils.ActionBarUtil;
import com.oakkits.utils.ColorUtil;
import com.oakkits.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KitCommand implements CommandExecutor {
    
    private final OakKits plugin;
    
    public KitCommand(OakKits plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            if (sender instanceof Player) {
                showKitList((Player) sender);
            } else {
                sender.sendMessage(ColorUtil.colorize("&cUsage: /kit <kitname>"));
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "give":
                return handleGive(sender, args);
            case "setcooldown":
                return handleSetCooldown(sender, args);
            case "setcost":
                return handleSetCost(sender, args);
            case "setpermission":
                return handleSetPermission(sender, args);
            case "resetcooldown":
                return handleResetCooldown(sender, args);
            case "preview":
                return handlePreview(sender, args);
            case "edit":
                return handleEdit(sender, args);
            case "reload":
                return handleReload(sender);
            case "list":
                if (sender instanceof Player) {
                    showKitList((Player) sender);
                }
                return true;
            default:
                if (sender instanceof Player) {
                    claimKit((Player) sender, subCommand);
                } else {
                    sender.sendMessage(ColorUtil.colorize("&cOnly players can claim kits!"));
                }
                return true;
        }
    }
    
    private void showKitList(Player player) {
        MessagesManager msg = plugin.getMessagesManager();
        List<Kit> availableKits = plugin.getKitsManager().getAvailableKits(player);
        
        if (availableKits.isEmpty()) {
            String noKitsMsg = msg.getPrefix() + msg.getMessage("kit-list.no-kits");
            player.sendMessage(formatForPlayer(player, noKitsMsg));
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, "&c✗ No kits available"));
            return;
        }
        
        if (plugin.getBedrockManager().isBedrockPlayer(player) && 
            plugin.getBedrockManager().shouldUseForms()) {
            plugin.getBedrockManager().openKitSelectionForm(player, availableKits);
            return;
        }
        
        player.sendMessage(formatForPlayer(player, msg.getMessage("kit-list.header")));
        player.sendMessage(formatForPlayer(player, msg.getMessage("kit-list.title")));
        player.sendMessage("");
        
        UUID uuid = player.getUniqueId();
        
        for (Kit kit : availableKits) {
            String kitId = kit.getId();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("kit", kit.getDisplayName());
            
            String line;
            if (kit.isOneTime() && plugin.getDataManager().hasUsedOneTime(uuid, kitId)) {
                line = msg.getMessage("kit-list.format-used", placeholders);
            } else if (plugin.getDataManager().isOnCooldown(uuid, kitId)) {
                long remaining = plugin.getDataManager().getRemainingCooldown(uuid, kitId);
                placeholders.put("time", TimeUtil.formatTime(remaining));
                line = msg.getMessage("kit-list.format-cooldown", placeholders);
            } else {
                line = msg.getMessage("kit-list.format-ready", placeholders);
            }
            player.sendMessage(formatForPlayer(player, line));
        }
        
        player.sendMessage("");
        player.sendMessage(formatForPlayer(player, msg.getMessage("kit-list.footer")));
        
        ActionBarUtil.sendActionBar(player, formatForPlayer(player, "&a✓ " + availableKits.size() + " kits available"));
    }
    
    private void claimKit(Player player, String kitName) {
        MessagesManager msg = plugin.getMessagesManager();
        Kit kit = plugin.getKitsManager().getKit(kitName);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kit", kitName);
        
        if (kit == null) {
            player.sendMessage(formatForPlayer(player, msg.getChatMessage("kit-not-found", placeholders)));
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("kit-not-found", placeholders)));
            return;
        }
        
        placeholders.put("kit", kit.getDisplayName());
        
        String perm = kit.getPermission();
        if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage(formatForPlayer(player, msg.getChatMessage("no-permission", placeholders)));
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("no-permission", placeholders)));
            return;
        }
        
        UUID uuid = player.getUniqueId();
        boolean bypassCooldown = player.isOp() || player.hasPermission("oakkits.bypass.cooldown");
        
        if (kit.isOneTime() && plugin.getDataManager().hasUsedOneTime(uuid, kit.getId()) && !bypassCooldown) {
            player.sendMessage(formatForPlayer(player, msg.getChatMessage("one-time-used", placeholders)));
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("one-time-used", placeholders)));
            return;
        }
        
        if (!bypassCooldown && plugin.getDataManager().isOnCooldown(uuid, kit.getId())) {
            long remaining = plugin.getDataManager().getRemainingCooldown(uuid, kit.getId());
            placeholders.put("time", TimeUtil.formatTime(remaining));
            player.sendMessage(formatForPlayer(player, msg.getChatMessage("cooldown-active", placeholders)));
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("cooldown-active", placeholders)));
            return;
        }
        
        double cost = kit.getCost();
        if (cost > 0 && plugin.getEconomyManager().isEnabled()) {
            double balance = plugin.getEconomyManager().getBalance(player);
            placeholders.put("cost", String.valueOf(cost));
            placeholders.put("balance", String.valueOf(balance));
            
            if (!plugin.getEconomyManager().hasBalance(player, cost)) {
                player.sendMessage(formatForPlayer(player, msg.getChatMessage("not-enough-money", placeholders)));
                ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("not-enough-money", placeholders)));
                return;
            }
        }
        
        if (plugin.getConfigManager().isDenyIfFull()) {
            int emptySlots = getEmptySlots(player);
            int neededSlots = kit.getTotalSlots();
            
            if (emptySlots < neededSlots) {
                player.sendMessage(formatForPlayer(player, msg.getChatMessage("inventory-full-denied", placeholders)));
                if (plugin.getConfigManager().isActionbarEnabled()) {
                    ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("inventory-full-denied", placeholders)));
                }
                return;
            }
        }
        
        if (cost > 0 && plugin.getEconomyManager().isEnabled()) {
            plugin.getEconomyManager().withdraw(player, cost);
            placeholders.put("cost", String.valueOf(cost));
            player.sendMessage(formatForPlayer(player, msg.getChatMessage("money-deducted", placeholders)));
        }
        
        if (plugin.getConfigManager().isClearBeforeGive()) {
            player.getInventory().clear();
        }
        
        giveKitItems(player, kit);
        
        if (kit.getCooldown() > 0 && !bypassCooldown) {
            long endTime = System.currentTimeMillis() + kit.getCooldown();
            plugin.getDataManager().setCooldown(uuid, kit.getId(), endTime);
        }
        
        if (kit.isOneTime()) {
            plugin.getDataManager().setOneTimeUsed(uuid, kit.getId());
        }
        
        for (String cmd : kit.getCommands()) {
            String parsedCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        if (plugin.getConfigManager().isClaimSoundEnabled()) {
            player.playSound(player.getLocation(), 
                plugin.getConfigManager().getClaimSound(),
                plugin.getConfigManager().getClaimSoundVolume(),
                plugin.getConfigManager().getClaimSoundPitch());
        }
        
        if (plugin.getConfigManager().isTitleEnabled()) {
            String titleMain = msg.getMessage("title.kit-claimed.main", placeholders);
            String titleSub = msg.getMessage("title.kit-claimed.subtitle", placeholders);
            player.sendTitle(
                titleMain,
                titleSub,
                plugin.getConfigManager().getTitleFadeIn(),
                plugin.getConfigManager().getTitleStay(),
                plugin.getConfigManager().getTitleFadeOut()
            );
        }
        
        if (plugin.getConfigManager().isBroadcastEnabled() && cost >= plugin.getConfigManager().getBroadcastMinCost()) {
            Map<String, String> broadcastPlaceholders = new HashMap<>(placeholders);
            broadcastPlaceholders.put("player", player.getName());
            String broadcastMsg = msg.getMessage("broadcast.kit-claimed", broadcastPlaceholders);
            Bukkit.broadcastMessage(broadcastMsg);
        }
        
        if (plugin.getConfigManager().isLogClaims()) {
            plugin.log("&7" + player.getName() + " claimed kit &e" + kit.getId());
        }
        
        player.sendMessage(formatForPlayer(player, msg.getChatMessage("kit-claimed", placeholders)));
        if (plugin.getConfigManager().isActionbarEnabled()) {
            ActionBarUtil.sendActionBar(player, formatForPlayer(player, msg.getActionBarMessage("kit-claimed", placeholders)));
        }
    }
    
    private String formatForPlayer(Player player, String message) {
        if (plugin.getBedrockManager().isBedrockPlayer(player)) {
            return plugin.getBedrockManager().getMobileFriendlyMessage(message);
        }
        return message;
    }
    
    private void giveKitItems(Player player, Kit kit) {
        PlayerInventory inv = player.getInventory();
        boolean dropOnGround = plugin.getConfigManager().isDropOnGround();
        boolean autoEquipArmor = plugin.getConfigManager().isAutoEquipArmor();
        
        for (ItemStack item : kit.getItems()) {
            HashMap<Integer, ItemStack> leftover = inv.addItem(item.clone());
            if (!leftover.isEmpty() && dropOnGround) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
        
        if (autoEquipArmor) {
            if (kit.getHelmet() != null) {
                if (inv.getHelmet() == null) {
                    inv.setHelmet(kit.getHelmet().clone());
                } else {
                    giveOrDrop(player, kit.getHelmet().clone(), dropOnGround);
                }
            }
            if (kit.getChestplate() != null) {
                if (inv.getChestplate() == null) {
                    inv.setChestplate(kit.getChestplate().clone());
                } else {
                    giveOrDrop(player, kit.getChestplate().clone(), dropOnGround);
                }
            }
            if (kit.getLeggings() != null) {
                if (inv.getLeggings() == null) {
                    inv.setLeggings(kit.getLeggings().clone());
                } else {
                    giveOrDrop(player, kit.getLeggings().clone(), dropOnGround);
                }
            }
            if (kit.getBoots() != null) {
                if (inv.getBoots() == null) {
                    inv.setBoots(kit.getBoots().clone());
                } else {
                    giveOrDrop(player, kit.getBoots().clone(), dropOnGround);
                }
            }
        } else {
            if (kit.getHelmet() != null) giveOrDrop(player, kit.getHelmet().clone(), dropOnGround);
            if (kit.getChestplate() != null) giveOrDrop(player, kit.getChestplate().clone(), dropOnGround);
            if (kit.getLeggings() != null) giveOrDrop(player, kit.getLeggings().clone(), dropOnGround);
            if (kit.getBoots() != null) giveOrDrop(player, kit.getBoots().clone(), dropOnGround);
        }
    }
    
    private void giveOrDrop(Player player, ItemStack item, boolean dropOnGround) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty() && dropOnGround) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }
    
    private int getEmptySlots(Player player) {
        int empty = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                empty++;
            }
        }
        return empty;
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.create")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.colorize("&cOnly players can create kits!"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit create <name>"));
            return true;
        }
        
        StringBuilder fullName = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) fullName.append(" ");
            fullName.append(args[i]);
        }
        String displayName = fullName.toString();
        String kitId = ColorUtil.stripColor(ColorUtil.colorize(displayName)).toLowerCase().replace(" ", "_");
        
        MessagesManager msg = plugin.getMessagesManager();
        Map<String, String> placeholders = MessagesManager.of("kit", ColorUtil.colorize(displayName));
        
        if (plugin.getKitsManager().kitExists(kitId)) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.kit-already-exists", placeholders));
            return true;
        }
        
        plugin.getKitsManager().createKit(kitId, displayName, (Player) sender);
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.kit-created", placeholders));
        ActionBarUtil.sendActionBar((Player) sender, "&a✓ Kit &e" + displayName + " &acreated!");
        
        return true;
    }
    
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.delete")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit delete <name>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        MessagesManager msg = plugin.getMessagesManager();
        Map<String, String> placeholders = MessagesManager.of("kit", kitName);
        
        if (!plugin.getKitsManager().kitExists(kitName)) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", placeholders));
            return true;
        }
        
        plugin.getKitsManager().deleteKit(kitName);
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.kit-deleted", placeholders));
        
        if (sender instanceof Player) {
            ActionBarUtil.sendActionBar((Player) sender, "&a✓ Kit deleted!");
        }
        
        return true;
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.give")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit give <player> <kit>"));
            return true;
        }
        
        String playerName = args[1];
        String kitName = args[2].toLowerCase();
        MessagesManager msg = plugin.getMessagesManager();
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.player-not-found", MessagesManager.of("player", playerName)));
            return true;
        }
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        giveKitItems(target, kit);
        
        for (String cmd : kit.getCommands()) {
            String parsedCmd = cmd.replace("{player}", target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.kit-given", 
            MessagesManager.of("kit", kit.getDisplayName(), "player", target.getName())));
        target.sendMessage(formatForPlayer(target, msg.getPrefix() + msg.getMessage("admin.kit-given-target", 
            MessagesManager.of("kit", kit.getDisplayName()))));
        ActionBarUtil.sendActionBar(target, formatForPlayer(target, "&a✓ " + ColorUtil.stripColor(kit.getDisplayName()) + " received!"));
        
        return true;
    }
    
    private boolean handleSetCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.setcooldown")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit setcooldown <kit> <time>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        String timeStr = args[2];
        MessagesManager msg = plugin.getMessagesManager();
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        long time = TimeUtil.parseTime(timeStr);
        if (time == 0 && !timeStr.equals("0") && !timeStr.equals("0s")) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.invalid-time"));
            return true;
        }
        
        kit.setCooldown(time);
        plugin.getKitsManager().saveKit(kit);
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.cooldown-set", 
            MessagesManager.of("kit", kit.getDisplayName(), "time", TimeUtil.formatTime(time))));
        
        return true;
    }
    
    private boolean handleSetCost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.setcost")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit setcost <kit> <amount>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        MessagesManager msg = plugin.getMessagesManager();
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        double cost;
        try {
            cost = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.invalid-cost"));
            return true;
        }
        
        kit.setCost(cost);
        plugin.getKitsManager().saveKit(kit);
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.cost-set", 
            MessagesManager.of("kit", kit.getDisplayName(), "cost", String.valueOf(cost))));
        
        return true;
    }
    
    private boolean handleSetPermission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.setpermission")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit setpermission <kit> <permission>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        String permission = args[2];
        MessagesManager msg = plugin.getMessagesManager();
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        kit.setPermission(permission);
        plugin.getKitsManager().saveKit(kit);
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.permission-set", 
            MessagesManager.of("kit", kit.getDisplayName(), "permission", permission)));
        
        return true;
    }
    
    private boolean handleResetCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.resetcooldown")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit resetcooldown <player> <kit>"));
            return true;
        }
        
        String playerName = args[1];
        String kitName = args[2].toLowerCase();
        MessagesManager msg = plugin.getMessagesManager();
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.player-not-found", MessagesManager.of("player", playerName)));
            return true;
        }
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        plugin.getDataManager().resetCooldown(target.getUniqueId(), kitName);
        plugin.getDataManager().resetOneTime(target.getUniqueId(), kitName);
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.cooldown-reset", 
            MessagesManager.of("player", target.getName(), "kit", kit.getDisplayName())));
        
        return true;
    }
    
    private boolean handlePreview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin.preview")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /kit preview <kit> <player>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        String playerName = args[2];
        MessagesManager msg = plugin.getMessagesManager();
        
        Kit kit = plugin.getKitsManager().getKit(kitName);
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.player-not-found", MessagesManager.of("player", playerName)));
            return true;
        }
        
        if (plugin.getBedrockManager().isBedrockPlayer(target) && 
            plugin.getBedrockManager().shouldUseForms()) {
            plugin.getBedrockManager().openKitPreviewForm(target, kit);
        } else {
            plugin.getGUIManager().openPreview(target, kit);
        }
        
        sender.sendMessage(msg.getPrefix() + msg.getMessage("admin.preview-opened", 
            MessagesManager.of("kit", kit.getDisplayName(), "player", target.getName())));
        
        return true;
    }
    
    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("oakkits.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.colorize("&cOnly players can use the kit editor!"));
            return true;
        }
        
        Player player = (Player) sender;
        MessagesManager msg = plugin.getMessagesManager();
        
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize("&6Available kits to edit:"));
            for (String kitName : plugin.getKitsManager().getKitNames()) {
                Kit kit = plugin.getKitsManager().getKit(kitName);
                player.sendMessage(ColorUtil.colorize("&7- &e" + kitName + " &7(" + kit.getDisplayName() + "&7)"));
            }
            player.sendMessage(ColorUtil.colorize("&7Use: &e/kit edit <kitname>"));
            return true;
        }
        
        String kitName = args[1].toLowerCase();
        Kit kit = plugin.getKitsManager().getKit(kitName);
        
        if (kit == null) {
            sender.sendMessage(msg.getChatMessage("kit-not-found", MessagesManager.of("kit", kitName)));
            return true;
        }
        
        plugin.getEditorGUIManager().openMainEditor(player, kit);
        ActionBarUtil.sendActionBar(player, "&a✓ Opening editor for " + kit.getDisplayName());
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("oakkits.admin.reload")) {
            sender.sendMessage(plugin.getMessagesManager().getChatMessage("no-permission", null));
            return true;
        }
        
        plugin.reloadAllConfigs();
        
        sender.sendMessage(plugin.getMessagesManager().getPrefix() + 
            plugin.getMessagesManager().getMessage("admin.config-reloaded"));
        
        if (sender instanceof Player) {
            ActionBarUtil.sendActionBar((Player) sender, "&a✓ Configs reloaded!");
        }
        
        return true;
    }
}
