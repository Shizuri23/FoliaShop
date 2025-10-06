package com.example.foliashop.service;

import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.config.ItemsCatalog;
import com.example.foliashop.db.InventoryDao;
import com.example.foliashop.model.MatchKey;
import org.bukkit.Material;

import java.sql.SQLException;

public class InventoryService {
    private final InventoryDao dao;
    private final ItemsCatalog catalog;
    private final MatchService matchService;
    private final ConfigManager cfg;

    public InventoryService(InventoryDao dao, ItemsCatalog catalog, MatchService matchService, ConfigManager cfg) {
        this.dao = dao;
        this.catalog = catalog;
        this.matchService = matchService;
        this.cfg = cfg;
    }

    public void addStock(MatchKey key, long qty) throws SQLException {
        if (qty <= 0) return;
        dao.upsertAdd(key.keyHash(), key.material().name(), qty);
    }

    /** return actually consumed (<= req) */
    public long consume(MatchKey key, long req) throws SQLException {
        if (req <= 0) return 0;
        return dao.tryConsume(key.keyHash(), req);
    }
}