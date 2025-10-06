package com.example.foliashop.listener;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.db.ShopPointDao;
import com.example.foliashop.gui.SellGui;
import com.example.foliashop.model.ShopPoint;
import com.example.foliashop.service.SellService;
import com.example.foliashop.service.ShopService;
import com.example.foliashop.util.FoliaSchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class BlockInteractListener implements Listener {
    private final FoliaShopPlugin plugin;
    private final ShopPointDao pointDao;
    private final ShopService shopService;
    private final SellService sellService;

    public BlockInteractListener(FoliaShopPlugin plugin, ShopPointDao pointDao, ShopService shopService, SellService sellService) {
        this.plugin = plugin;
        this.pointDao = pointDao;
        this.shopService = shopService;
        this.sellService = sellService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.CHEST) return;

        var loc = b.getLocation();
        Optional<ShopPoint> point;
        try {
            point = pointDao.findByLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } catch (SQLException ex) {
            plugin.getLogger().severe("DB error: " + ex.getMessage());
            return;
        }
        if (point.isEmpty()) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("foliashop.use")) return;

        if ("SHOP".equalsIgnoreCase(point.get().type())) {
            shopService.openShop(p, 1);
        } else {
            // open sell gui
            SellGui gui = new SellGui(plugin);
            FoliaSchedulerUtil.runAtEntity(plugin, p, () -> {
                p.openInventory(gui.getInventory());
                plugin.msg().send(p, "open-sell", Map.of());
            });
        }
    }
}