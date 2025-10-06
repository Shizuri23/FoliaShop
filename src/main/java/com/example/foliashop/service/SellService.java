package com.example.foliashop.service;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.config.ItemsCatalog;
import com.example.foliashop.model.MatchKey;
import com.example.foliashop.model.TxnDirection;
import com.example.foliashop.db.TransactionDao;
import com.example.foliashop.util.FoliaSchedulerUtil;
import com.example.foliashop.util.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

public class SellService {
    private final FoliaShopPlugin plugin;
    private final ConfigManager cfg;
    private final ItemsCatalog items;
    private final EconomyService econ;
    private final TransactionDao txnDao;
    private final InventoryService invService;

    private final Map<UUID, Long> cooldown = new HashMap<>();

    public SellService(FoliaShopPlugin plugin, ConfigManager cfg, ItemsCatalog items, EconomyService econ, TransactionDao txnDao, InventoryService invService) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.items = items;
        this.econ = econ;
        this.txnDao = txnDao;
        this.invService = invService;
    }

    public void confirmSell(Player p, Inventory gui) {
        long now = System.currentTimeMillis();
        Long last = cooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < cfg.clickCooldownMs()) return;
        cooldown.put(p.getUniqueId(), now);

        // Collect valid items from top rows except bottom row (buttons)
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        List<Integer> validSlots = new ArrayList<>();

        int rows = cfg.rows();
        int bottomStart = (rows - 1) * 9;

        for (int i = 0; i < bottomStart; i++) {
            ItemStack it = gui.getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            Material m = it.getType();

            // whitelist/blacklist
            if (!isSellable(m)) continue;

            ItemsCatalog.Price price = items.price(m);
            if (price == null || price.sellPrice <= 0) continue;

            counts.merge(m, it.getAmount(), Integer::sum);
            validSlots.add(i);
        }

        if (counts.isEmpty()) {
            FoliaSchedulerUtil.runAtEntity(plugin, p, () ->
                    plugin.msg().send(p, "invalid-item", Map.of())
            );
            return;
        }

        // Compute money
        BigDecimal grossCalc = BigDecimal.ZERO;
        Map<String, Object> itemsJson = new HashMap<>();
        for (Map.Entry<Material, Integer> e : counts.entrySet()) {
            ItemsCatalog.Price pr = items.price(e.getKey());
            BigDecimal line = BigDecimal.valueOf(pr.sellPrice).multiply(BigDecimal.valueOf(e.getValue()));
            grossCalc = grossCalc.add(line);
            itemsJson.put(e.getKey().name(), Map.of("qty", e.getValue(), "unit", pr.sellPrice));
        }
        final BigDecimal gross = grossCalc;

        BigDecimal feeTmp = BigDecimal.ZERO;
        if (cfg.config().getBoolean("economy.enable-fee-on-sell", true)) {
            feeTmp = roundMoney(gross.multiply(BigDecimal.valueOf(cfg.feePercent() / 100.0)));
        }
        final BigDecimal fee = feeTmp;
        final BigDecimal net = roundMoney(gross.subtract(fee));

        if (net.compareTo(BigDecimal.ZERO) <= 0) {
            // ⚠️ 使用快照字符串，避免 lambda 捕获到“非有效 final”的 BigDecimal
            final String grossStr0 = gross.toPlainString();
            final String feeStr0   = fee.toPlainString();
            final String netStr0   = net.toPlainString();

            FoliaSchedulerUtil.runAtEntity(plugin, p, () ->
                    plugin.msg().send(p, "sell-summary", Map.of(
                            "gross", grossStr0,
                            "fee",  feeStr0,
                            "net",  netStr0
                    ))
            );
            return;
        }

        // ===== 在进入任何 lambda 之前，创建不可变快照（final）=====
        final Player playerRef = p;
        final Inventory guiRef = gui;
        final List<Integer> validSlotsRef = List.copyOf(validSlots);
        final Map<Material, Integer> countsRef = new EnumMap<>(counts);
        final Map<String, Object> itemsJsonRef = new HashMap<>(itemsJson);

        final String grossStr = gross.toPlainString();
        final String feeStr   = fee.toPlainString();
        final String netStr   = net.toPlainString();

        final double grossVal = gross.doubleValue();
        final double feeVal   = fee.doubleValue();
        final double netVal   = net.doubleValue();

        final String playerIdRef = playerRef.getUniqueId().toString();
        // =======================================================

        // Remove items from GUI (Region thread)
        FoliaSchedulerUtil.runAtEntity(plugin, playerRef, () -> {
            for (int slot : validSlotsRef) guiRef.setItem(slot, null);
        });

        // Async: deposit & add stock & txn log
        FoliaSchedulerUtil.runAsync(() -> {
            boolean ok = econ.deposit(playerRef, netVal);
            if (!ok) {
                // deposit failed -> return items to player (Region)
                FoliaSchedulerUtil.runAtEntity(plugin, playerRef, () -> {
                    for (Map.Entry<Material, Integer> e : countsRef.entrySet()) {
                        int remain = e.getValue();
                        while (remain > 0) {
                            int a = Math.min(64, remain);
                            playerRef.getInventory().addItem(new ItemStack(e.getKey(), a));
                            remain -= a;
                        }
                    }
                });
                return;
            }

            // add stock
            try {
                for (Map.Entry<Material, Integer> e : countsRef.entrySet()) {
                    MatchKey key = new MatchKey(e.getKey());
                    invService.addStock(key, e.getValue());
                }
                txnDao.insert(
                        playerIdRef,
                        TxnDirection.SELL,
                        grossVal, feeVal, netVal,
                        itemsJsonRef,
                        null
                );
            } catch (SQLException ex) {
                plugin.getLogger().severe("Sell DB error: " + ex.getMessage());
            }

            FoliaSchedulerUtil.runAtEntity(plugin, playerRef, () -> {
                Message m = plugin.msg();
                m.send(playerRef, "sell-summary", Map.of(
                        "gross", grossStr,
                        "fee",  feeStr,
                        "net",  netStr
                ));
                m.send(playerRef, "sell-success", Map.of("net", netStr));
            });
        });
    }

    private boolean isSellable(Material m) {
        var wl = cfg.whitelist();
        var bl = cfg.blacklist();
        if (!wl.isEmpty()) return wl.contains(m.name());
        if (cfg.allowBlacklist()) return !bl.contains(m.name());
        return true;
    }

    private BigDecimal roundMoney(BigDecimal x) {
        String mode = cfg.feeRounding();
        RoundingMode rm = switch (mode) {
            case "ROUND_DOWN" -> RoundingMode.DOWN;
            case "CEIL" -> RoundingMode.CEILING;
            default -> RoundingMode.HALF_UP;
        };
        return x.setScale(2, rm);
    }
}