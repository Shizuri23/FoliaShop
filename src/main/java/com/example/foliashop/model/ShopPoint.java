package com.example.foliashop.model;

public record ShopPoint(
        long id,
        String world,
        int x, int y, int z,
        String type, // "SHOP" or "SELL"
        float yaw, float pitch,
        String creatorUuid
) {}