package com.example.foliashop.config;

import com.example.foliashop.FoliaShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ItemsCatalog {

    public static class Price {
        public final double buyPrice;   // 玩家购买价
        public final double sellPrice;  // 玩家卖出基价（未扣手续费）
        public final String displayName;

        public Price(double buy, double sell, String dn){
            this.buyPrice = buy;
            this.sellPrice = sell;
            this.displayName = dn;
        }
    }

    private final Map<Material, Price> byMaterial = new EnumMap<>(Material.class);

    public ItemsCatalog(FoliaShopPlugin plugin) {
        reload(plugin);
    }

    public void reload(FoliaShopPlugin plugin) {
        byMaterial.clear();
        FileConfiguration f = plugin.getConfigManager().items();
        for (Map<?,?> raw : f.getMapList("items")) {
            String matName = String.valueOf(raw.get("material")).toUpperCase(Locale.ROOT);
            Material mat = Material.matchMaterial(matName);
            if (mat == null) continue;
            double buy = raw.get("buy-price") == null ? 0.0 : Double.parseDouble(String.valueOf(raw.get("buy-price")));
            double sell = raw.get("sell-price") == null ? 0.0 : Double.parseDouble(String.valueOf(raw.get("sell-price")));
            String dn = raw.get("display-name") == null ? mat.name() : String.valueOf(raw.get("display-name"));
            byMaterial.put(mat, new Price(buy, sell, dn));
        }
    }

    public Price price(Material mat) { return byMaterial.get(mat); }

    public Set<Material> allPriced() { return Collections.unmodifiableSet(byMaterial.keySet()); }
}