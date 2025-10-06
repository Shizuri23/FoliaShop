package com.example.foliashop.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private final Map<String, String> map;

    public Message(Map<String, String> map) {
        this.map = map;
    }

    public String raw(String key) { return map.getOrDefault(key, key); }

    public String format(String key, Map<String, String> params) {
        String s = raw(key);
        for (Map.Entry<String, String> e : params.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        s = s.replace("{prefix}", raw("prefix"));
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void send(CommandSender sender, String key, Map<String, String> params) {
        sender.sendMessage(format(key, params));
    }
}