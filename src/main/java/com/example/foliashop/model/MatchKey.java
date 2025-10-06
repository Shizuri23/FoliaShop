package com.example.foliashop.model;

import org.bukkit.Material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class MatchKey {
    private final Material material;

    public MatchKey(Material material) { this.material = material; }

    public Material material() { return material; }

    /** For MATERIAL_ONLY we can just hash material name */
    public String keyHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(material.name().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return material.name().toLowerCase(Locale.ROOT);
        }
    }
}