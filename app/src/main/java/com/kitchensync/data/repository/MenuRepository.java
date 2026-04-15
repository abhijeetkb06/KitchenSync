package com.kitchensync.data.repository;

import com.couchbase.lite.Collection;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.MutableDocument;

import com.kitchensync.data.CouchbaseManager;
import com.kitchensync.data.model.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MenuRepository {

    private static final MenuItem[] SEED_ITEMS = {
        // Tacos
        new MenuItem("menu::crunchy_taco", "Crunchy Taco", "Tacos", 1.89, "crunchy_taco"),
        new MenuItem("menu::doritos_taco", "Doritos Locos Taco", "Tacos", 2.49, "doritos_taco"),
        new MenuItem("menu::soft_taco", "Soft Taco", "Tacos", 1.89, "soft_taco"),
        new MenuItem("menu::chalupa", "Chalupa Supreme", "Tacos", 3.79, "chalupa"),
        // Burritos
        new MenuItem("menu::bean_burrito", "Bean Burrito", "Burritos", 1.69, "bean_burrito"),
        new MenuItem("menu::beefy_5layer", "Beefy 5-Layer", "Burritos", 3.49, "beefy_5layer"),
        new MenuItem("menu::crunchwrap", "Crunchwrap Supreme", "Burritos", 4.89, "crunchwrap"),
        new MenuItem("menu::quesarito", "Quesarito", "Burritos", 3.99, "quesarito"),
        // Sides
        new MenuItem("menu::chips_nacho", "Chips & Nacho Cheese", "Sides", 1.89, "chips_nacho"),
        new MenuItem("menu::cinnamon_twists", "Cinnamon Twists", "Sides", 1.49, "cinnamon_twists"),
        new MenuItem("menu::fiesta_potatoes", "Cheesy Fiesta Potatoes", "Sides", 2.49, "fiesta_potatoes"),
        // Drinks
        new MenuItem("menu::baja_blast", "Baja Blast", "Drinks", 2.49, "baja_blast"),
        new MenuItem("menu::pepsi", "Pepsi", "Drinks", 1.99, "pepsi"),
        new MenuItem("menu::tb_iced_coffee", "Iced Coffee", "Drinks", 2.79, "tb_iced_coffee"),
    };

    public void seedMenuIfNeeded() throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return;

        // Check if menu items already exist
        if (collection.getDocument("menu::crunchy_taco") != null) return;

        for (MenuItem item : SEED_ITEMS) {
            MutableDocument doc = new MutableDocument(item.getMenuItemId());
            for (java.util.Map.Entry<String, Object> entry : item.toMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    doc.setString(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Double) {
                    doc.setDouble(entry.getKey(), (Double) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    doc.setBoolean(entry.getKey(), (Boolean) entry.getValue());
                }
            }
            collection.save(doc);
        }
    }

    public List<MenuItem> getAllMenuItems() throws CouchbaseLiteException {
        List<MenuItem> items = new ArrayList<>();
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return items;

        for (MenuItem seed : SEED_ITEMS) {
            com.couchbase.lite.Document doc = collection.getDocument(seed.getMenuItemId());
            if (doc != null) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("menuItemId", doc.getString("menuItemId"));
                map.put("name", doc.getString("name"));
                map.put("category", doc.getString("category"));
                map.put("price", doc.getDouble("price"));
                map.put("imageKey", doc.getString("imageKey"));
                map.put("available", doc.getBoolean("available"));
                items.add(MenuItem.fromMap(map));
            }
        }
        return items;
    }

    public List<MenuItem> getMenuItemsByCategory(String category) throws CouchbaseLiteException {
        List<MenuItem> all = getAllMenuItems();
        if (category == null || category.equals("All")) return all;
        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem item : all) {
            if (category.equals(item.getCategory())) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
