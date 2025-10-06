package com.example.foliashop.command;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.db.ShopPointDao;
import com.example.foliashop.model.ShopPoint;
import com.example.foliashop.util.FoliaSchedulerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map;

public class SetShopCommand implements CommandExecutor {
    private final FoliaShopPlugin plugin;
    private final ShopPointDao dao;

    public SetShopCommand(FoliaShopPlugin plugin, ShopPointDao dao) {
        this.plugin = plugin; this.dao = dao;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!p.hasPermission("foliashop.admin.set")) {
            plugin.msg().send(p, "no-permission", Map.of());
            return true;
        }
        Block target = p.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.AIR) {
            plugin.msg().send(p, "not-looking-at-air", Map.of());
            return true;
        }
        Location place = target.getLocation();

        FoliaSchedulerUtil.runAtLocation(plugin, place, () -> {
            place.getBlock().setType(Material.CHEST);
            ShopPoint sp = new ShopPoint(0, place.getWorld().getName(), place.getBlockX(), place.getBlockY(), place.getBlockZ(),
                    "SHOP", p.getLocation().getYaw(), p.getLocation().getPitch(), p.getUniqueId().toString());
            try {
                dao.insert(sp);
            } catch (SQLException e) {
                plugin.getLogger().severe("DB insert error: " + e.getMessage());
            }
            plugin.msg().send(p, "shop-placed", Map.of(
                    "world", place.getWorld().getName(),
                    "x", String.valueOf(place.getBlockX()),
                    "y", String.valueOf(place.getBlockY()),
                    "z", String.valueOf(place.getBlockZ())
            ));
        });
        return true;
    }
}