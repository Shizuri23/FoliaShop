package com.example.foliashop.service;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.config.ItemsCatalog;
import com.example.foliashop.db.InventoryDao;
import com.example.foliashop.db.TransactionDao;
import com.example.foliashop.gui.ShopGui;
import com.example.foliashop.model.InventoryEntry;
import com.example.foliashop.model.MatchKey;
import com.example.foliashop.model.TxnDirection;
import com.example.foliashop.util.FoliaSchedulerUtil;
import com.example.foliashop.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class ShopService {
    private final FoliaShopPlugin plugin;
    private final ConfigManager cfg;
    private final ItemsCatalog items;
    private final EconomyService econ;
    private final TransactionDao txnDao;
    private final InventoryService invService;
    private final MatchService matchService;

    public ShopService(FoliaShopPlugin plugin, ConfigManager cfg, ItemsCatalog items, EconomyService econ, TransactionDao txnDao, InventoryService invService, MatchService matchService) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.items = items;
        this.econ = econ;
        this.txnDao = txnDao;
        this.invService = invService;
        this.matchService = matchService;
    }

    public void openShop(Player p, int page) {
        boolean includeZero = cfg.showZeroStock();
        int per = cfg.itemsPerPage();
        int offset = (page - 1) * per;
        List<InventoryEntry> list;
        int count;
        try {
            list = plugin.getInventoryDao().page(offset, per, includeZero);
            count = plugin.getInventoryDao().countAll(includeZero);
        } catch (SQLException e) {
            plugin.getLogger().severe("DB page error: " + e.getMessage());
            list = List.of();
            count = 0;
        }
        int pages = Math.max(1, (int) Math.ceil(count / (double) per));

        ShopGui gui = new ShopGui(plugin, page, pages, list);
        FoliaSchedulerUtil.runAtEntity(plugin, p, () -> {
            p.openInventory(gui.getInventory());
            plugin.msg().send(p, "open-shop", Map.of());
        });
    }

    public void handleBuyClick(Player p, ShopGui gui, int slot, boolean rightClick, boolean shift) {
        var entry = gui.entryAt(slot);
        if (entry == null) return;

        Material mat = entry.material();
        ItemsCatalog.Price pr = items.price(mat);
        if (pr == null || pr.buyPrice <= 0) return;

        long req = rightClick ? 64 : 1;
        if (shift) req = Math.min(64, req * 2);

        MatchKey key = new MatchKey(mat);

        // Async: check/consume/pay/log; then deliver in Region
        long finalReq = req;
        FoliaSchedulerUtil.runAsync(() -> {
            long consumed;
            try {
                // check current stock quickly
                consumed = plugin.getInventoryDao().tryConsume(key.keyHash(), finalReq);
            } catch (Exception e) {
                plugin.getLogger().severe("Consume error: " + e.getMessage());
                return;
            }
            if (consumed <= 0) {
                FoliaSchedulerUtil.runAtEntity(plugin, p, () -> {
                    plugin.msg().send(p, "buy-partial", Map.of("item", mat.name(), "amount", "0", "price", "0"));
                });
                return;
            }

            double buyFee = cfg.buyFeePercent();
            double unit = pr.buyPrice * (1.0 + buyFee / 100.0);
            BigDecimal pay = BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(consumed)).setScale(2, BigDecimal.ROUND_HALF_UP);

            if (!econ.has(p, pay.doubleValue())) {
                // refund stock
                try { invService.addStock(key, consumed); } catch (Exception ignored) {}
                FoliaSchedulerUtil.runAtEntity(plugin, p, () ->
                        plugin.msg().send(p, "buy-failed-balance", Map.of("need", pay.toPlainString()))
                );
                return;
            }

            boolean ok = econ.withdraw(p, pay.doubleValue());
            if (!ok) {
                try { invService.addStock(key, consumed); } catch (Exception ignored) {}
                return;
            }

            // Log & deliver
            try {
                Map<String, Object> itemsJson = Map.of(mat.name(), Map.of("qty", consumed, "unit", pr.buyPrice));
                txnDao.insert(p.getUniqueId().toString(), TxnDirection.BUY, pay.doubleValue(), 0.0, pay.doubleValue(), itemsJson, null);
            } catch (Exception e) {
                plugin.getLogger().warning("Txn log failed: " + e.getMessage());
            }

            FoliaSchedulerUtil.runAtEntity(plugin, p, () -> {
                long remain = consumed;
                while (remain > 0) {
                    int a = (int) Math.min(64, remain);
                    p.getInventory().addItem(new ItemStack(mat, a));
                    remain -= a;
                }
                plugin.msg().send(p, "buy-success", Map.of(
                        "item", mat.name(),
                        "amount", String.valueOf(consumed),
                        "price", pay.toPlainString()
                ));
            });
        });
    }
}