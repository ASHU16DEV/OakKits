package com.oakkits.managers;

import com.oakkits.OakKits;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    
    private final OakKits plugin;
    private Economy economy;
    private boolean enabled;
    
    public EconomyManager(OakKits plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.log("&eVault not found. Economy features disabled.");
            enabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.log("&eNo economy plugin found. Economy features disabled.");
            enabled = false;
            return;
        }
        
        economy = rsp.getProvider();
        enabled = true;
        plugin.log("&aVault economy hooked successfully!");
    }
    
    public boolean isEnabled() {
        return enabled && economy != null && plugin.getConfigManager().isEconomyEnabled();
    }
    
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }
    
    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) return true;
        return economy.has(player, amount);
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled() || amount <= 0) return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean deposit(Player player, double amount) {
        if (!isEnabled() || amount <= 0) return true;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    public String formatBalance(double amount) {
        if (!isEnabled()) return String.valueOf(amount);
        return economy.format(amount);
    }
}
