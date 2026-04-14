package com.yumkitchen.sync.data.repository;

import android.util.Log;

import com.couchbase.lite.Collection;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Ordering;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

import com.yumkitchen.sync.data.CouchbaseManager;
import com.yumkitchen.sync.data.model.Order;
import com.yumkitchen.sync.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderRepository {
    private static final String TAG = "OrderRepository";

    public void saveOrder(Order order) throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return;

        MutableDocument doc = new MutableDocument(order.getOrderId());
        Map<String, Object> map = order.toMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            doc.setValue(entry.getKey(), entry.getValue());
        }
        collection.save(doc);
        Log.i(TAG, "Order saved: " + order.getOrderId());
    }

    public void updateOrderStatus(String orderId, String newStatus) throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return;

        Document doc = collection.getDocument(orderId);
        if (doc == null) return;

        MutableDocument mutableDoc = doc.toMutable();
        mutableDoc.setString("status", newStatus);
        mutableDoc.setString("updatedAt", java.time.Instant.now().toString());
        collection.save(mutableDoc);
        Log.i(TAG, "Order status updated: " + orderId + " -> " + newStatus);
    }

    public List<Order> getOrders(String statusFilter) {
        List<Order> orders = new ArrayList<>();
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return orders;

        try {
            Expression where = Expression.property("type")
                    .equalTo(Expression.string(Constants.DOC_TYPE_ORDER));

            if (statusFilter != null && !statusFilter.equals("all")) {
                where = where.and(Expression.property("status")
                        .equalTo(Expression.string(statusFilter)));
            }

            Query query = QueryBuilder.select(SelectResult.all())
                    .from(DataSource.collection(collection))
                    .where(where)
                    .orderBy(Ordering.property("createdAt").descending());

            ResultSet rs = query.execute();
            for (Result result : rs) {
                com.couchbase.lite.Dictionary dict = result.getDictionary(0);
                if (dict != null) {
                    String orderId = dict.getString("orderId");
                    if (orderId != null) {
                        Document fullDoc = collection.getDocument(orderId);
                        if (fullDoc != null) {
                            Order order = Order.fromDocument(fullDoc);
                            if (order != null) {
                                orders.add(order);
                            }
                        }
                    }
                }
            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error querying orders", e);
        }
        return orders;
    }

    public List<Order> getActiveOrders() {
        List<Order> orders = new ArrayList<>();
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return orders;

        try {
            Expression where = Expression.property("type")
                    .equalTo(Expression.string(Constants.DOC_TYPE_ORDER))
                    .and(Expression.property("status")
                            .notEqualTo(Expression.string(Constants.ORDER_STATUS_PICKED_UP)));

            Query query = QueryBuilder.select(SelectResult.all())
                    .from(DataSource.collection(collection))
                    .where(where)
                    .orderBy(Ordering.property("createdAt").ascending());

            ResultSet rs = query.execute();
            for (Result result : rs) {
                com.couchbase.lite.Dictionary dict = result.getDictionary(0);
                if (dict != null) {
                    String orderId = dict.getString("orderId");
                    if (orderId != null) {
                        Document fullDoc = collection.getDocument(orderId);
                        if (fullDoc != null) {
                            Order order = Order.fromDocument(fullDoc);
                            if (order != null) {
                                orders.add(order);
                            }
                        }
                    }
                }
            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error querying active orders", e);
        }
        return orders;
    }

    public Query createOrdersLiveQuery() throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return null;

        return QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("type")
                        .equalTo(Expression.string(Constants.DOC_TYPE_ORDER)))
                .orderBy(Ordering.property("createdAt").descending());
    }

    public Query createActiveOrdersLiveQuery() throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return null;

        return QueryBuilder.select(SelectResult.all())
                .from(DataSource.collection(collection))
                .where(Expression.property("type")
                        .equalTo(Expression.string(Constants.DOC_TYPE_ORDER))
                        .and(Expression.property("status")
                                .notEqualTo(Expression.string(Constants.ORDER_STATUS_PICKED_UP))))
                .orderBy(Ordering.property("createdAt").ascending());
    }

    public void deleteAllOrders() throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return;

        List<Order> orders = getOrders(null);
        for (Order order : orders) {
            Document doc = collection.getDocument(order.getOrderId());
            if (doc != null) {
                collection.delete(doc);
            }
        }
        Log.i(TAG, "All orders deleted");
    }

    public int getMaxOrderNumber() {
        int max = 0;
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return max;

        try {
            Query query = QueryBuilder.select(SelectResult.property("orderNumber"))
                    .from(DataSource.collection(collection))
                    .where(Expression.property("type")
                            .equalTo(Expression.string(Constants.DOC_TYPE_ORDER)));

            ResultSet rs = query.execute();
            for (Result result : rs) {
                int num = result.getInt("orderNumber");
                if (num > max) max = num;
            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error querying max order number", e);
        }
        return max;
    }

    /**
     * Find an open (new or preparing) order for the given customer name (case-insensitive).
     */
    public Order findOpenOrderByCustomerName(String customerName) {
        if (customerName == null || customerName.isEmpty()) return null;
        String nameLower = customerName.toLowerCase();

        List<Order> activeOrders = getActiveOrders();
        for (Order order : activeOrders) {
            String status = order.getStatus();
            if (Constants.ORDER_STATUS_NEW.equals(status) || Constants.ORDER_STATUS_PREPARING.equals(status)) {
                String existingName = order.getCustomerName();
                if (existingName != null && existingName.toLowerCase().equals(nameLower)) {
                    return order;
                }
            }
        }
        return null;
    }

    /**
     * Append items to an existing order document and update total.
     */
    public void appendItemsToOrder(Order existingOrder, List<com.yumkitchen.sync.data.model.OrderItem> newItems) throws CouchbaseLiteException {
        Collection collection = CouchbaseManager.getInstance().getCollection();
        if (collection == null) return;

        existingOrder.addItems(newItems);
        existingOrder.setUpdatedAt(java.time.Instant.now().toString());

        // Re-save the full document
        Document doc = collection.getDocument(existingOrder.getOrderId());
        if (doc == null) return;

        MutableDocument mutableDoc = doc.toMutable();
        Map<String, Object> map = existingOrder.toMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            mutableDoc.setValue(entry.getKey(), entry.getValue());
        }
        collection.save(mutableDoc);
        Log.i(TAG, "Items appended to order: " + existingOrder.getOrderId());
    }
}
