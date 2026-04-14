package com.yumkitchen.sync.data.model;

import com.couchbase.lite.Document;
import com.couchbase.lite.Array;
import com.couchbase.lite.Dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Order {
    private String orderId;
    private int orderNumber;
    private String customerName;
    private String status;
    private List<OrderItem> items;
    private double totalAmount;
    private String createdBy;
    private String createdByName;
    private String createdAt;
    private String updatedAt;

    public Order() {
        this.items = new ArrayList<>();
    }

    public static Order create(int orderNumber, String customerName, List<OrderItem> items,
                               String createdBy, String createdByName) {
        Order order = new Order();
        order.orderId = "order::" + UUID.randomUUID().toString().substring(0, 8);
        order.orderNumber = orderNumber;
        order.customerName = customerName;
        order.status = "new";
        order.items = new ArrayList<>(items);
        order.totalAmount = 0;
        for (OrderItem item : items) {
            order.totalAmount += item.getLineTotal();
        }
        order.createdBy = createdBy;
        order.createdByName = createdByName;
        String now = java.time.Instant.now().toString();
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public static Order fromDocument(Document doc) {
        if (doc == null) return null;
        Order order = new Order();
        order.orderId = doc.getString("orderId");
        order.orderNumber = doc.getInt("orderNumber");
        order.customerName = doc.getString("customerName");
        order.status = doc.getString("status");
        order.totalAmount = doc.getDouble("totalAmount");
        order.createdBy = doc.getString("createdBy");
        order.createdByName = doc.getString("createdByName");
        order.createdAt = doc.getString("createdAt");
        order.updatedAt = doc.getString("updatedAt");

        order.items = new ArrayList<>();
        Array itemsArray = doc.getArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.count(); i++) {
                Dictionary dict = itemsArray.getDictionary(i);
                if (dict != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("menuItemId", dict.getString("menuItemId"));
                    map.put("name", dict.getString("name"));
                    map.put("quantity", dict.getInt("quantity"));
                    map.put("notes", dict.getString("notes"));
                    map.put("price", dict.getDouble("price"));
                    map.put("emoji", dict.getString("emoji"));
                    order.items.add(OrderItem.fromMap(map));
                }
            }
        }
        return order;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "order");
        map.put("orderId", orderId);
        map.put("orderNumber", orderNumber);
        map.put("customerName", customerName);
        map.put("status", status);
        map.put("totalAmount", totalAmount);
        map.put("createdBy", createdBy);
        map.put("createdByName", createdByName);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (OrderItem item : items) {
            itemMaps.add(item.toMap());
        }
        map.put("items", itemMaps);
        return map;
    }

    public String getOrderId() { return orderId; }
    public int getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getItemCount() {
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public String getShortId() {
        if (orderId != null && orderId.contains("::")) {
            return "#" + orderId.substring(orderId.indexOf("::") + 2).toUpperCase();
        }
        return orderId;
    }

    public String getDisplayLabel() {
        String name = customerName != null ? customerName : "";
        return "#" + orderNumber + " " + name;
    }

    public void addItems(List<OrderItem> newItems) {
        this.items.addAll(newItems);
        for (OrderItem item : newItems) {
            this.totalAmount += item.getLineTotal();
        }
    }
}
