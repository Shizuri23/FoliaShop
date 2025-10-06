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
import java.util.*;

public class ShopAdminCommand implements CommandExecutor {
    private final FoliaShopPlugin plugin;
    private final ShopPointDao dao;

    public ShopAdminCommand(FoliaShopPlugin plugin, ShopPointDao dao) { this.plugin = plugin; this.dao = dao; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!p.hasPermission("foliashop.admin.manage")) {
            plugin.msg().send(p, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("/shop reload | remove | list [page] | tp <id>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getItemsCatalog().reload(plugin);
                p.sendMessage(plugin.msg().format("reload-ok", Map.of()));
                return true;
            }
            case "remove" -> {
                Block looking = p.getTargetBlockExact(5);
                if (looking == null) return true;
                var loc = looking.getLocation();
                try {
                    boolean ok = dao.deleteByLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    if (ok) {
                        FoliaSchedulerUtil.runAtLocation(plugin, loc, () -> looking.setType(Material.AIR));
                        p.sendMessage(plugin.msg().format("removed", Map.of()));
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("DB delete error: " + e.getMessage());
                }
                return true;
            }
            case "list" -> {
                int page = 1;
                if (args.length >= 2) try { page = Integer.parseInt(args[1]); } catch (Exception ignored){}
                int per = 8;
                int offset = (page - 1) * per;
                try {
                    List<ShopPoint> list = dao.list(offset, per);
                    int pages = page + (list.size() == per ? 1 : 0);
                    p.sendMessage(plugin.msg().format("list-header", Map.of("page", String.valueOf(page), "pages", String.valueOf(pages))));
                    for (ShopPoint sp : list) {
                        p.sendMessage(plugin.msg().format("list-row", Map.of(
                                "id", String.valueOf(sp.id()),
                                "type", sp.type(),
                                "world", sp.world(),
                                "x", String.valueOf(sp.x()),
                                "y", String.valueOf(sp.y()),
                                "z", String.valueOf(sp.z())
                        )));
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("DB list error: " + e.getMessage());
                }
                return true;
            }
            case "tp" -> {
                if (args.length < 2) return true;
                long id = Long.parseLong(args[1]);
                // 简化：重新 list + 选取（演示目的）；生产上应做 findById
                try {
                    List<ShopPoint> list = dao.list(0, 1000);
                    for (ShopPoint sp : list) {
                        if (sp.id() == id) {
                            var w = plugin.getServer().getWorld(sp.world());
                            if (w == null) return true;
                            Location loc = new Location(w, sp.x()+0.5, sp.y()+1.0, sp.z()+0.5);
                            p.teleport(loc);
                            p.sendMessage(plugin.msg().format("tp-ok", Map.of("id", String.valueOf(id))));
                            break;
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("DB tp error: " + e.getMessage());
                }
                return true;
            }
        }
        return true;
    }
}