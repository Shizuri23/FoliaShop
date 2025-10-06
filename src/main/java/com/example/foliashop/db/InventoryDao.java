package com.example.foliashop.db;

import com.example.foliashop.model.InventoryEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryDao {
    private final Database db;

    public InventoryDao(Database db) { this.db = db; }

    public void upsertAdd(String keyHash, String material, long qty) throws SQLException {
        String sql = "INSERT INTO shop_inventory(key_hash, material, quantity) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyHash);
            ps.setString(2, material);
            ps.setLong(3, qty);
            ps.executeUpdate();
        }
    }

    /** Try consume qty; return actually consumed (0..qty) */
    public long tryConsume(String keyHash, long qty) throws SQLException {
        String update = "UPDATE shop_inventory SET quantity = quantity - ? WHERE key_hash=? AND quantity >= ?";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(update)) {
            ps.setLong(1, qty);
            ps.setString(2, keyHash);
            ps.setLong(3, qty);
            int affected = ps.executeUpdate();
            if (affected > 0) return qty;
        }
        // Partial: read current
        Optional<Long> cur = getQuantity(keyHash);
        if (cur.isPresent() && cur.get() > 0) {
            long can = cur.get();
            String upd2 = "UPDATE shop_inventory SET quantity = quantity - ? WHERE key_hash=? AND quantity >= ?";
            try (Connection c2 = db.conn(); PreparedStatement ps2 = c2.prepareStatement(upd2)) {
                ps2.setLong(1, can);
                ps2.setString(2, keyHash);
                ps2.setLong(3, can);
                int a2 = ps2.executeUpdate();
                if (a2 > 0) return can;
            }
        }
        return 0L;
    }

    public Optional<Long> getQuantity(String keyHash) throws SQLException {
        String sql = "SELECT quantity FROM shop_inventory WHERE key_hash=?";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, keyHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getLong(1));
            }
        }
        return Optional.empty();
    }

    public List<InventoryEntry> page(int offset, int limit, boolean includeZero) throws SQLException {
        String sql = includeZero
                ? "SELECT key_hash, material, quantity FROM shop_inventory ORDER BY material LIMIT ? OFFSET ?"
                : "SELECT key_hash, material, quantity FROM shop_inventory WHERE quantity > 0 ORDER BY material LIMIT ? OFFSET ?";
        List<InventoryEntry> list = new ArrayList<>();
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InventoryEntry(rs.getString(1), rs.getString(2), rs.getLong(3)));
                }
            }
        }
        return list;
    }

    public int countAll(boolean includeZero) throws SQLException {
        String sql = includeZero ? "SELECT COUNT(*) FROM shop_inventory" : "SELECT COUNT(*) FROM shop_inventory WHERE quantity > 0";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}