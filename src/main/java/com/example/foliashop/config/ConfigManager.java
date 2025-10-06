package com.example.foliashop.config;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.util.Message;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final FoliaShopPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration messages;

    public ConfigManager(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        reloadAll();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        this.items = YamlConfiguration.loadConfiguration(itemsFile);

        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(msgFile);
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration items() { return items; }

    public Message getMessage() {
        Map<String, String> m = new HashMap<>();
        for (String key : messages.getKeys(false)) {
            m.put(key, messages.getString(key, key));
        }
        return new Message(m);
    }

    public int rows() { return config.getInt("gui.rows", 6); }
    public int itemsPerPage() { return config.getInt("pagination.items-per-page", 45); }
    public int slotPrev() { return config.getInt("gui.previous-slot", 46); }
    public int slotClose() { return config.getInt("gui.close-slot", 49); }
    public int slotNext() { return config.getInt("gui.next-slot", 52); }

    public String namePrev() { return config.getString("gui.previous-name", "« 上一页"); }
    public String nameClose() { return config.getString("gui.close-name", "✖ 关闭"); }
    public String nameNext() { return config.getString("gui.next-name", "下一页 »"); }
    public String nameConfirmSell() { return config.getString("gui.confirm-sell-name", "✔ 确定卖出"); }

    public boolean showZeroStock() { return config.getBoolean("inventory.show-zero-stock", false); }

    public double feePercent() { return config.getDouble("economy.fee-percent", 5.0); }
    public double buyFeePercent() { return config.getDouble("economy.buy-fee-percent", 0.0); }
    public String feeRounding() { return config.getString("economy.fee-rounding", "ROUND_HALF_UP"); }

    public long clickCooldownMs() { return config.getLong("sell.click-cooldown-ms", 300L); }

    public boolean allowBlacklist() { return config.getBoolean("sell.allow-blacklist", false); }
    public java.util.Set<String> blacklist() { return new java.util.HashSet<>(config.getStringList("sell.blacklist")); }
    public java.util.Set<String> whitelist() { return new java.util.HashSet<>(config.getStringList("sell.whitelist")); }
}