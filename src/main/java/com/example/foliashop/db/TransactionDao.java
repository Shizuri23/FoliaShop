package com.example.foliashop.db;

import com.example.foliashop.model.TxnDirection;
import com.google.gson.Gson;

import java.sql.*;
import java.util.Map;

public class TransactionDao {
    private final Database db;
    private final Gson gson = new Gson();

    public TransactionDao(Database db) { this.db = db; }

    public void insert(String playerUuid, TxnDirection direction, double gross, double fee, double net, Map<String, Object> itemsJson, Long pointId) throws SQLException {
        String sql = "INSERT INTO shop_transactions(player_uuid, direction, gross_amount, fee_amount, net_amount, items_json, point_id) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, direction.name());
            ps.setDouble(3, gross);
            ps.setDouble(4, fee);
            ps.setDouble(5, net);
            ps.setString(6, gson.toJson(itemsJson));
            if (pointId == null) ps.setNull(7, Types.BIGINT);
            else ps.setLong(7, pointId);
            ps.executeUpdate();
        }
    }
}