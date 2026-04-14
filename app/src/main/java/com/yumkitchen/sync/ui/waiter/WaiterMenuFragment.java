package com.yumkitchen.sync.ui.waiter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.CouchbaseManager;
import com.yumkitchen.sync.data.model.MenuItem;
import com.yumkitchen.sync.data.model.Order;
import com.yumkitchen.sync.data.model.OrderItem;
import com.yumkitchen.sync.data.repository.MenuRepository;
import com.yumkitchen.sync.data.repository.OrderRepository;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class WaiterMenuFragment extends Fragment implements MenuItemAdapter.OnItemClickListener,
        CartAdapter.OnCartChangeListener {

    private static final AtomicInteger orderCounter = new AtomicInteger(0);
    private static boolean counterInitialized = false;

    private MenuItemAdapter menuAdapter;
    private CartAdapter cartAdapter;
    private MaterialCardView cardCart;
    private TextView textTotal;
    private TextInputEditText editCustomerName;
    private final MenuRepository menuRepo = new MenuRepository();
    private final OrderRepository orderRepo = new OrderRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiter_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initOrderCounter();
        setupMenuGrid(view);
        setupCart(view);
        setupCategoryFilter(view);
        loadMenu(null);
    }

    private void initOrderCounter() {
        if (!counterInitialized) {
            int max = orderRepo.getMaxOrderNumber();
            orderCounter.set(max + 1);
            counterInitialized = true;
        }
    }

    private void setupMenuGrid(View view) {
        RecyclerView recyclerMenu = view.findViewById(R.id.recycler_menu);
        menuAdapter = new MenuItemAdapter(this);
        recyclerMenu.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerMenu.setAdapter(menuAdapter);
    }

    private void setupCart(View view) {
        cardCart = view.findViewById(R.id.card_cart);
        textTotal = view.findViewById(R.id.text_total);
        editCustomerName = view.findViewById(R.id.edit_customer_name);

        RecyclerView recyclerCart = view.findViewById(R.id.recycler_cart);
        cartAdapter = new CartAdapter(this);
        recyclerCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCart.setAdapter(cartAdapter);

        MaterialButton btnSend = view.findViewById(R.id.btn_send_order);
        btnSend.setOnClickListener(v -> submitOrder());
    }

    private void setupCategoryFilter(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String category = null;
            if (checkedIds.contains(R.id.chip_tacos)) category = "Tacos";
            else if (checkedIds.contains(R.id.chip_burritos)) category = "Burritos";
            else if (checkedIds.contains(R.id.chip_sides)) category = "Sides";
            else if (checkedIds.contains(R.id.chip_drinks)) category = "Drinks";
            loadMenu(category);
        });
    }

    private void loadMenu(String category) {
        try {
            List<MenuItem> items = menuRepo.getMenuItemsByCategory(category);
            menuAdapter.setItems(items);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error loading menu", Toast.LENGTH_SHORT).show();
        }
    }

    // MenuItemAdapter.OnItemClickListener
    @Override
    public void onAddToCart(MenuItem item) {
        OrderItem orderItem = new OrderItem(
                item.getMenuItemId(), item.getName(), 1, "", item.getPrice(), item.getEmoji());
        cartAdapter.addItem(orderItem);
        updateCartVisibility();
    }

    // CartAdapter.OnCartChangeListener
    @Override
    public void onItemRemoved(int position) {
        updateCartVisibility();
    }

    private void updateCartVisibility() {
        if (cartAdapter.isEmpty()) {
            cardCart.setVisibility(View.GONE);
        } else {
            cardCart.setVisibility(View.VISIBLE);
            textTotal.setText(String.format(Locale.US, "$%.2f", cartAdapter.getTotal()));
        }
    }

    private void submitOrder() {
        if (cartAdapter.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = editCustomerName.getText() != null
                ? editCustomerName.getText().toString().trim() : "";
        if (customerName.isEmpty()) {
            editCustomerName.setError("Required");
            return;
        }

        // Check if there's already an open order for this customer
        Order existingOrder = orderRepo.findOpenOrderByCustomerName(customerName);
        if (existingOrder != null) {
            showAppendConfirmDialog(existingOrder, customerName);
        } else {
            // Refresh counter from DB to account for orders synced from other kiosks
            int currentMax = orderRepo.getMaxOrderNumber();
            if (currentMax >= orderCounter.get()) {
                orderCounter.set(currentMax + 1);
            }
            int orderNumber = orderCounter.getAndIncrement();
            showConfirmDialog(orderNumber, customerName);
        }
    }

    private void showAppendConfirmDialog(Order existingOrder, String customerName) {
        List<OrderItem> items = cartAdapter.getItems();
        StringBuilder sb = new StringBuilder();
        sb.append("Add to existing ").append(existingOrder.getDisplayLabel()).append("?\n\n");
        sb.append("New items:\n");
        for (OrderItem item : items) {
            sb.append(item.getQuantity()).append("x ").append(item.getName())
                    .append(" - $").append(String.format(Locale.US, "%.2f", item.getLineTotal()))
                    .append("\n");
        }
        sb.append("\nNew items total: $").append(String.format(Locale.US, "%.2f", cartAdapter.getTotal()));

        new AlertDialog.Builder(requireContext())
                .setTitle("Add to Order " + existingOrder.getDisplayLabel() + "?")
                .setMessage(sb.toString())
                .setPositiveButton("Add to Order", (d, w) -> {
                    appendToOrder(existingOrder, items);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showConfirmDialog(int orderNumber, String customerName) {
        List<OrderItem> items = cartAdapter.getItems();
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(orderNumber).append(" - ").append(customerName).append("\n\n");
        for (OrderItem item : items) {
            sb.append(item.getQuantity()).append("x ").append(item.getName())
                    .append(" - $").append(String.format(Locale.US, "%.2f", item.getLineTotal()))
                    .append("\n");
        }
        sb.append("\nTotal: $").append(String.format(Locale.US, "%.2f", cartAdapter.getTotal()));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_order)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.send_to_kitchen, (d, w) -> {
                    placeOrder(orderNumber, customerName, items);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void placeOrder(int orderNumber, String customerName, List<OrderItem> items) {
        try {
            String peerId = CouchbaseManager.getInstance().getCurrentPeerId();
            String peerName = android.os.Build.MODEL + " - Kiosk";
            Order order = Order.create(orderNumber, customerName, items, peerId, peerName);
            orderRepo.saveOrder(order);

            // Clear cart
            cartAdapter.clear();
            editCustomerName.setText("");
            updateCartVisibility();

            Toast.makeText(requireContext(),
                    "Order #" + orderNumber + " for " + customerName + " placed!",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void appendToOrder(Order existingOrder, List<OrderItem> items) {
        try {
            orderRepo.appendItemsToOrder(existingOrder, items);

            // Clear cart
            cartAdapter.clear();
            editCustomerName.setText("");
            updateCartVisibility();

            Toast.makeText(requireContext(),
                    "Items added to " + existingOrder.getDisplayLabel() + "!",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
