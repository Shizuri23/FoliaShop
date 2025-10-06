package com.example.foliashop.service;

import com.example.foliashop.FoliaShopPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class EconomyService {
    private final FoliaShopPlugin plugin;
    private final Economy economy; // may be null if no Vault

    public EconomyService(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        this.economy = null;
    }

    public EconomyService(FoliaShopPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean ready() { return economy != null; }

    public boolean has(Player p, double amount) {
        return economy != null && economy.has(p, amount);
    }

    public boolean withdraw(Player p, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(Player p, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(p, amount).transactionSuccess();
    }
}