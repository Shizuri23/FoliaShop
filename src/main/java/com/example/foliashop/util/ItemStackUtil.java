package com.example.foliashop.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemStackUtil {
    private ItemStackUtil(){}

    public static ItemStack simple(Material mat, String displayName, List<String> lore) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (displayName != null) meta.setDisplayName(displayName);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        it.setItemMeta(meta);
        return it;
    }

    public static List<String> loreLines(String... lines) {
        List<String> l = new ArrayList<>();
        for (String s : lines) l.add(ChatColor.translateAlternateColorCodes('&', s));
        return l;
    }
}