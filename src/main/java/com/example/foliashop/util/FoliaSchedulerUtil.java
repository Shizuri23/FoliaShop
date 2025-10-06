package com.example.foliashop.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class FoliaSchedulerUtil {
    private FoliaSchedulerUtil() {}

    /** 在位置所在 Region 线程执行 —— 你的 RegionScheduler#execute 第三参是 Runnable（0 参） */
    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, loc, task);
        // 若未来切到 Consumer<ScheduledTask> 版本，改为：
        // Bukkit.getRegionScheduler().execute(plugin, loc, st -> task.run());
    }

    /** 在实体当前所在 Region 执行 —— 用实体当前位置(Location)提交到 RegionScheduler */
    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        Location loc = entity.getLocation();
        Bukkit.getRegionScheduler().execute(plugin, loc, task);
        // 也可以使用实体调度器（如果你 API 提供无延迟 Runnable 重载）：
        // entity.getScheduler().execute(plugin, task);
        // 若该方法在你的版本需要 Consumer：entity.getScheduler().execute(plugin, st -> task.run());
    }

    /** 异步执行 —— 你的 AsyncScheduler#runNow 需要 Consumer<ScheduledTask>（1 参） */
    public static void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, st -> task.run());
    }

    /** 兼容旧调用（不建议）：自动取任意一个已加载的插件作为 owner */
    public static void runAsync(Runnable task) {
        Plugin owner = Bukkit.getPluginManager().getPlugins()[0];
        Bukkit.getAsyncScheduler().runNow(owner, st -> task.run());
    }
}