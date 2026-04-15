package com.kitchensync.data.model;

import java.util.HashMap;
import java.util.Map;

public class MenuItem {
    private String menuItemId;
    private String name;
    private String category;
    private double price;
    private String imageKey;
    private boolean available;

    public MenuItem() {}

    public MenuItem(String menuItemId, String name, String category, double price, String imageKey) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageKey = imageKey;
        this.available = true;
    }

    public static MenuItem fromMap(Map<String, Object> map) {
        MenuItem item = new MenuItem();
        item.menuItemId = (String) map.get("menuItemId");
        item.name = (String) map.get("name");
        item.category = (String) map.get("category");
        item.price = ((Number) map.get("price")).doubleValue();
        item.imageKey = (String) map.get("imageKey");
        Object avail = map.get("available");
        item.available = avail == null || (Boolean) avail;
        return item;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "menu_item");
        map.put("menuItemId", menuItemId);
        map.put("name", name);
        map.put("category", category);
        map.put("price", price);
        map.put("imageKey", imageKey);
        map.put("available", available);
        return map;
    }

    public String getMenuItemId() { return menuItemId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getImageKey() { return imageKey; }
    public boolean isAvailable() { return available; }
}
