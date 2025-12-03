package com.oakkits.commands;

import com.oakkits.OakKits;
import com.oakkits.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KitTabCompleter implements TabCompleter {
    
    private final OakKits plugin;
    
    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
        "create", "delete", "give", "setcooldown", "setcost", "setpermission", "resetcooldown", "preview", "edit", "reload", "list"
    );
    
    private static final List<String> TIME_SUGGESTIONS = Arrays.asList(
        "1h", "6h", "12h", "1d", "7d", "30m", "0"
    );
    
    private static final List<String> COST_SUGGESTIONS = Arrays.asList(
        "0", "100", "500", "1000", "5000", "10000"
    );
    
    public KitTabCompleter(OakKits plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                for (Kit kit : plugin.getKitsManager().getAvailableKits(player)) {
                    if (kit.getId().toLowerCase().startsWith(input)) {
                        completions.add(kit.getId());
                    }
                }
            }
            
            if (sender.hasPermission("oakkits.admin")) {
                for (String cmd : ADMIN_COMMANDS) {
                    if (cmd.startsWith(input)) {
                        completions.add(cmd);
                    }
                }
            }
            
            return completions;
        }
        
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[args.length - 1].toLowerCase();
            
            switch (subCommand) {
                case "delete":
                case "setcooldown":
                case "setcost":
                case "setpermission":
                    if (args.length == 2 && sender.hasPermission("oakkits.admin." + subCommand)) {
                        return getKitCompletions(input);
                    }
                    if (args.length == 3) {
                        if (subCommand.equals("setcooldown")) {
                            return filterStartsWith(TIME_SUGGESTIONS, input);
                        }
                        if (subCommand.equals("setcost")) {
                            return filterStartsWith(COST_SUGGESTIONS, input);
                        }
                        if (subCommand.equals("setpermission")) {
                            return Arrays.asList("oakkits.kit." + args[1].toLowerCase());
                        }
                    }
                    break;
                    
                case "give":
                case "resetcooldown":
                    if (args.length == 2 && sender.hasPermission("oakkits.admin." + subCommand)) {
                        return getPlayerCompletions(input);
                    }
                    if (args.length == 3 && sender.hasPermission("oakkits.admin." + subCommand)) {
                        return getKitCompletions(input);
                    }
                    break;
                    
                case "preview":
                    if (args.length == 2 && sender.hasPermission("oakkits.admin.preview")) {
                        return getKitCompletions(input);
                    }
                    if (args.length == 3 && sender.hasPermission("oakkits.admin.preview")) {
                        return getPlayerCompletions(input);
                    }
                    break;
                    
                case "edit":
                    if (args.length == 2 && sender.hasPermission("oakkits.admin")) {
                        return getKitCompletions(input);
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    private List<String> getKitCompletions(String input) {
        return plugin.getKitsManager().getKitNames().stream()
            .filter(name -> name.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    private List<String> getPlayerCompletions(String input) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
    
    private List<String> filterStartsWith(List<String> list, String input) {
        return list.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
}
