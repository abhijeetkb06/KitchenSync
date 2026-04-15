package com.kitchensync.data.model;

import java.util.HashMap;
import java.util.Map;

public class OrderItem {
    private String menuItemId;
    private String name;
    private int quantity;
    private String notes;
    private double price;
    private String imageKey;

    public OrderItem() {}

    public OrderItem(String menuItemId, String name, int quantity, String notes, double price, String imageKey) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.quantity = quantity;
        this.notes = notes;
        this.price = price;
        this.imageKey = imageKey;
    }

    @SuppressWarnings("unchecked")
    public static OrderItem fromMap(Map<String, Object> map) {
        OrderItem item = new OrderItem();
        item.menuItemId = (String) map.get("menuItemId");
        item.name = (String) map.get("name");
        item.quantity = ((Number) map.get("quantity")).intValue();
        item.notes = (String) map.get("notes");
        item.price = ((Number) map.get("price")).doubleValue();
        item.imageKey = (String) map.get("imageKey");
        return item;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("menuItemId", menuItemId);
        map.put("name", name);
        map.put("quantity", quantity);
        map.put("notes", notes != null ? notes : "");
        map.put("price", price);
        map.put("imageKey", imageKey != null ? imageKey : "");
        return map;
    }

    public String getMenuItemId() { return menuItemId; }
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getLineTotal() { return price * quantity; }
    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
}
