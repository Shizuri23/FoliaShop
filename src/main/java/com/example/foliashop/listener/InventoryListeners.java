package com.example.foliashop.listener;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.gui.SellGui;
import com.example.foliashop.gui.ShopGui;
import com.example.foliashop.service.SellService;
import com.example.foliashop.service.ShopService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class InventoryListeners implements Listener {
    private final FoliaShopPlugin plugin;
    private final SellService sellService;
    private final ShopService shopService;

    public InventoryListeners(FoliaShopPlugin plugin, SellService sellService, ShopService shopService) {
        this.plugin = plugin; this.sellService = sellService; this.shopService = shopService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        if (e.getView().getTopInventory().getHolder() instanceof ShopGui gui) {
            // prevent taking from top inventory
            if (e.getClickedInventory().equals(e.getView().getTopInventory())) {
                e.setCancelled(true);
                int slot = e.getSlot();
                int prev = plugin.getConfigManager().slotPrev();
                int next = plugin.getConfigManager().slotNext();
                int close = plugin.getConfigManager().slotClose();
                if (slot == close) {
                    e.getWhoClicked().closeInventory();
                    return;
                }
                if (slot == prev && gui.page() > 1) {
                    shopService.openShop((org.bukkit.entity.Player) e.getWhoClicked(), gui.page() - 1);
                    return;
                }
                if (slot == next && gui.page() < gui.pages()) {
                    shopService.openShop((org.bukkit.entity.Player) e.getWhoClicked(), gui.page() + 1);
                    return;
                }
                // item click
                boolean right = e.getClick().isRightClick();
                boolean shift = e.getClick().isShiftClick();
                shopService.handleBuyClick((org.bukkit.entity.Player) e.getWhoClicked(), gui, slot, right, shift);
            }
        } else if (e.getView().getTopInventory().getHolder() instanceof SellGui) {
            // allow placing items into top except bottom row buttons;
            int rows = plugin.getConfigManager().rows();
            int bottomStart = (rows - 1) * 9;

            if (e.getClickedInventory().equals(e.getView().getTopInventory())) {
                if (e.getSlot() >= bottomStart) {
                    e.setCancelled(true);
                    // confirm or close
                    var it = e.getCurrentItem();
                    if (it != null && it.getType() == Material.LIME_CONCRETE) {
                        sellService.confirmSell((org.bukkit.entity.Player) e.getWhoClicked(), e.getView().getTopInventory());
                    } else if (it != null && it.getType() == Material.BARRIER) {
                        e.getWhoClicked().closeInventory();
                    }
                } else {
                    // allow
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopGui) {
            e.setCancelled(true);
        }
        // SellGui: allow drag into top except bottom row
        if (e.getView().getTopInventory().getHolder() instanceof SellGui) {
            int rows = plugin.getConfigManager().rows();
            int bottomStart = (rows - 1) * 9;
            for (int slot : e.getRawSlots()) {
                if (slot < e.getView().getTopInventory().getSize() && slot >= bottomStart) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}