package com.example.foliashop.gui;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SellGui implements InventoryHolder {
    private final FoliaShopPlugin plugin;
    private final ConfigManager cfg;
    private final Inventory inv;

    public SellGui(FoliaShopPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        this.inv = Bukkit.createInventory(this, cfg.rows() * 9, Component.text("卖出 / 回收"));
        render();
    }

    private void render() {
        int close = cfg.slotClose();
        int confirm = cfg.slotClose(); // 中间按钮共用 close-slot 位置以“中间”为准
        inv.setItem(cfg.slotClose(), ItemStackUtil.simple(Material.BARRIER, cfg.nameClose(), null));
        inv.setItem(confirm, ItemStackUtil.simple(Material.LIME_CONCRETE, cfg.nameConfirmSell(), null));
    }

    @Override public Inventory getInventory() { return inv; }
}