package com.example.foliashop;

import com.example.foliashop.command.SetSellCommand;
import com.example.foliashop.command.SetShopCommand;
import com.example.foliashop.command.ShopAdminCommand;
import com.example.foliashop.config.ConfigManager;
import com.example.foliashop.config.ItemsCatalog;
import com.example.foliashop.db.Database;
import com.example.foliashop.db.InventoryDao;
import com.example.foliashop.db.ShopPointDao;
import com.example.foliashop.db.TransactionDao;
import com.example.foliashop.listener.BlockInteractListener;
import com.example.foliashop.listener.InventoryListeners;
import com.example.foliashop.service.*;
import com.example.foliashop.util.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FoliaShopPlugin extends JavaPlugin {

    private static FoliaShopPlugin instance;

    private ConfigManager configManager;
    private ItemsCatalog itemsCatalog;

    private Database database;
    private ShopPointDao shopPointDao;
    private InventoryDao inventoryDao;
    private TransactionDao transactionDao;

    private EconomyService economyService;
    private MatchService matchService;
    private InventoryService inventoryService;
    private SellService sellService;
    private ShopService shopService;

    public static FoliaShopPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig(); // ensure config.yml exists
        saveResource("items.yml", false);
        saveResource("messages.yml", false);

        // Load configs
        this.configManager = new ConfigManager(this);
        this.itemsCatalog = new ItemsCatalog(this);

        // Vault hook
        setupEconomy();

        // Database
        this.database = new Database(this, configManager);
        database.init();
        database.runInitSql(); // create tables if not exists

        // DAOs
        this.shopPointDao = new ShopPointDao(database);
        this.inventoryDao = new InventoryDao(database);
        this.transactionDao = new TransactionDao(database);

        // Services
        this.matchService = new MatchService(configManager);
        this.economyService = new EconomyService(this);
        this.inventoryService = new InventoryService(inventoryDao, itemsCatalog, matchService, configManager);
        this.sellService = new SellService(this, configManager, itemsCatalog, economyService, transactionDao, inventoryService);
        this.shopService = new ShopService(this, configManager, itemsCatalog, economyService, transactionDao, inventoryService, matchService);

        // Commands
        getCommand("setshop").setExecutor(new SetShopCommand(this, shopPointDao));
        getCommand("setsell").setExecutor(new SetSellCommand(this, shopPointDao));
        getCommand("shop").setExecutor(new ShopAdminCommand(this, shopPointDao));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new BlockInteractListener(this, shopPointDao, shopService, sellService), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListeners(this, sellService, shopService), this);

        getLogger().info("FoliaShop enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.shutdown();
        getLogger().info("FoliaShop disabled.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault not found! Economy features disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No Economy provider found! Please install an economy plugin.");
            return;
        }
        Economy econ = rsp.getProvider();
        this.economyService = new EconomyService(this, econ);
        getLogger().info("Hooked into Vault economy: " + econ.getName());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemsCatalog getItemsCatalog() {
        return itemsCatalog;
    }

    public Database getDatabase() {
        return database;
    }

    public ShopPointDao getShopPointDao() {
        return shopPointDao;
    }

    public InventoryDao getInventoryDao() {
        return inventoryDao;
    }

    public TransactionDao getTransactionDao() {
        return transactionDao;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public SellService getSellService() {
        return sellService;
    }

    public ShopService getShopService() {
        return shopService;
    }

    public Message msg() { return configManager.getMessage(); }
}