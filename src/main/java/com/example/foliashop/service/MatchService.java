package com.example.foliashop.service;

import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.model.MatchKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MatchService {
    private final ConfigManager config;

    public MatchService(ConfigManager config) { this.config = config; }

    /** Current implementation: MATERIAL_ONLY */
    public MatchKey keyOf(ItemStack is) {
        if (is == null || is.getType() == Material.AIR) return null;
        return new MatchKey(is.getType());
    }

    public MatchKey keyOf(Material mat) {
        return new MatchKey(mat);
    }
}