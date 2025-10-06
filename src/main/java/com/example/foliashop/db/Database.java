package com.example.foliashop.db;

import com.example.foliashop.FoliaShopPlugin;
import com.example.foliashop.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class Database {
    private final FoliaShopPlugin plugin;
    private final ConfigManager cfg;
    private HikariDataSource ds;

    public Database(FoliaShopPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void init() {
        HikariConfig hc = new HikariConfig();
        String jdbc = "jdbc:mysql://" + cfg.config().getString("database.host") + ":" +
                cfg.config().getInt("database.port") + "/" +
                cfg.config().getString("database.name") +
                "?useUnicode=true&characterEncoding=utf8&useSSL=" +
                cfg.config().getBoolean("database.useSSL", false);
        hc.setJdbcUrl(jdbc);
        hc.setUsername(cfg.config().getString("database.user"));
        hc.setPassword(cfg.config().getString("database.password"));

        // Hikari pool options
        hc.setMaximumPoolSize(cfg.config().getInt("database.pool.maximumPoolSize", 10));
        hc.setMinimumIdle(cfg.config().getInt("database.pool.minimumIdle", 2));
        hc.setConnectionTimeout(cfg.config().getLong("database.pool.connectionTimeoutMs", 10000L));

        this.ds = new HikariDataSource(hc);
        plugin.getLogger().info("Connected to MySQL.");
    }

    public Connection conn() throws java.sql.SQLException {
        return ds.getConnection();
    }

    public void runInitSql() {
        // 先尝试通过 JavaPlugin 提供的公共 API 读取 jar 内资源
        InputStream in = plugin.getResource("sql/init.sql");

        // 兜底：使用当前类的类加载器（不要用 plugin.getClassLoader()，它是 protected）
        if (in == null) {
            in = Database.class.getClassLoader().getResourceAsStream("sql/init.sql");
        }

        if (in == null) {
            plugin.getLogger().warning("init.sql not found in resources; make sure tables exist or place file at src/main/resources/sql/init.sql");
            return;
        }

        try (InputStream is = in;
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             Connection c = conn();
             Statement st = c.createStatement()) {

            String sql = br.lines().collect(Collectors.joining("\n"));

            // 简单按分号拆分执行（确保你的 init.sql 没有分号内嵌到字符串等复杂情况）
            for (String part : sql.split(";")) {
                String s = part.trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
            plugin.getLogger().info("Executed init.sql successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to run init.sql: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (ds != null) ds.close();
    }
}