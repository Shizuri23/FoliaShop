package com.example.foliashop.db;

import com.example.foliashop.model.ShopPoint;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShopPointDao {
    private final Database db;

    public ShopPointDao(Database db) {
        this.db = db;
    }

    public long insert(ShopPoint p) throws SQLException {
        String sql = "INSERT INTO shop_points(world,x,y,z,type,yaw,pitch,creator_uuid) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.world());
            ps.setInt(2, p.x());
            ps.setInt(3, p.y());
            ps.setInt(4, p.z());
            ps.setString(5, p.type());
            ps.setFloat(6, p.yaw());
            ps.setFloat(7, p.pitch());
            ps.setString(8, p.creatorUuid());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    public Optional<ShopPoint> findByLocation(String world, int x, int y, int z) throws SQLException {
        String sql = "SELECT * FROM shop_points WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x); ps.setInt(3, y); ps.setInt(4, z);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(from(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean deleteByLocation(String world, int x, int y, int z) throws SQLException {
        String sql = "DELETE FROM shop_points WHERE world=? AND x=? AND y=? AND z=?";
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x); ps.setInt(3, y); ps.setInt(4, z);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ShopPoint> list(int offset, int limit) throws SQLException {
        String sql = "SELECT * FROM shop_points ORDER BY id LIMIT ? OFFSET ?";
        List<ShopPoint> out = new ArrayList<>();
        try (Connection c = db.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(from(rs));
            }
        }
        return out;
    }

    private ShopPoint from(ResultSet rs) throws SQLException {
        return new ShopPoint(
                rs.getLong("id"),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getString("type"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                rs.getString("creator_uuid")
        );
    }
}