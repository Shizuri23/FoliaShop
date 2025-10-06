package com.example.foliashop.gui;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.config.ItemsCatalog;
import com.example.foliashop.model.InventoryEntry;
import com.example.foliashop.util.ItemStackUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ShopGui implements InventoryHolder {
    private final FoliaShopPlugin plugin;
    private final ConfigManager cfg;
    private final ItemsCatalog catalog;
    private final int page, pages;
    private final List<InventoryEntry> entries;
    private final Inventory inv;

    private final Map<Integer, EntryWrap> slotMap = new HashMap<>();

    // 关键修复：对外可见 + 静态嵌套，record 访问器 material()/stock() 也随之为 public
    public static record EntryWrap(Material material, long stock) {}

    public ShopGui(FoliaShopPlugin plugin, int page, int pages, List<InventoryEntry> entries) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        this.catalog = plugin.getItemsCatalog();
        this.page = page; this.pages = pages; this.entries = entries;
        this.inv = Bukkit.createInventory(this, cfg.rows() * 9, Component.text("商店 - 第 " + page + "/" + pages + " 页"));
        render();
    }

    @Override public Inventory getInventory() { return inv; }

    private void render() {
        int per = cfg.itemsPerPage();
        int used = 0;
        for (InventoryEntry e : entries) {
            Material mat = Material.matchMaterial(e.material());
            if (mat == null) continue;
            ItemsCatalog.Price price = catalog.price(mat);
            if (price == null || price.buyPrice <= 0) continue;

            List<String> lore = new ArrayList<>();
            lore.add("§7售价: §a" + price.buyPrice);
            lore.add(cfg.config().getString("ui.stock-lore-format", "§7库存: §e{stock}")
                    .replace("{stock}", String.valueOf(e.quantity())));
            if (cfg.config().getBoolean("ui.source-lore", true)) {
                lore.add("§7来源：玩家回收库存");
            }

            ItemStack icon = ItemStackUtil.simple(
                    mat,
                    price.displayName != null ? price.displayName : mat.name(),
                    lore
            );
            int slot = used++;
            if (slot >= per) break;
            inv.setItem(slot, icon);
            slotMap.put(slot, new EntryWrap(mat, e.quantity()));
        }

        // 底部按钮
        inv.setItem(cfg.slotClose(), ItemStackUtil.simple(Material.BARRIER, cfg.nameClose(), null));
        if (page > 1) {
            inv.setItem(cfg.slotPrev(), ItemStackUtil.simple(Material.ARROW, cfg.namePrev(), null));
        }
        if (page < pages) {
            inv.setItem(cfg.slotNext(), ItemStackUtil.simple(Material.ARROW, cfg.nameNext(), null));
        }
    }

    // 保证公开返回可见的 EntryWrap
    public EntryWrap entryAt(int slot) {
        var w = slotMap.get(slot);
        if (w == null) return null;
        return w;
    }

    public int page() { return page; }
    public int pages() { return pages; }
}