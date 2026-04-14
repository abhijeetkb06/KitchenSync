package com.yumkitchen.sync.ui.kitchen;

import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.Query;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.model.Order;
import com.yumkitchen.sync.data.repository.OrderRepository;
import com.yumkitchen.sync.util.Constants;

import java.util.List;

public class KitchenDisplayFragment extends Fragment implements OrderCardAdapter.OnOrderActionListener {
    private static final String TAG = "KitchenDisplay";

    private RecyclerView recyclerOrders;
    private LinearLayout emptyState;
    private OrderCardAdapter adapter;
    private final OrderRepository orderRepo = new OrderRepository();
    private Query liveQuery;
    private ListenerToken queryToken;
    private int lastOrderCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kitchen_display, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerOrders = view.findViewById(R.id.recycler_orders);
        emptyState = view.findViewById(R.id.empty_state);

        adapter = new OrderCardAdapter(this);

        int columns = getResources().getConfiguration().screenWidthDp > 600 ? 3 : 2;
        recyclerOrders.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        recyclerOrders.setAdapter(adapter);

        startLiveQuery();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapter.stopTimer();
        if (queryToken != null) {
            queryToken.remove();
            queryToken = null;
        }
    }

    private void startLiveQuery() {
        try {
            liveQuery = orderRepo.createActiveOrdersLiveQuery();
            if (liveQuery == null) return;

            queryToken = liveQuery.addChangeListener(change -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    List<Order> orders = orderRepo.getActiveOrders();
                    adapter.setOrders(orders);

                    // Haptic feedback for new orders
                    if (orders.size() > lastOrderCount && lastOrderCount > 0) {
                        triggerHaptic();
                    }
                    lastOrderCount = orders.size();

                    emptyState.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerOrders.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
                });
            });

            // Initial load
            List<Order> orders = orderRepo.getActiveOrders();
            adapter.setOrders(orders);
            lastOrderCount = orders.size();
            emptyState.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerOrders.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up live query", e);
        }
    }

    private void triggerHaptic() {
        try {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(200,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception e) {
            // Ignore vibration errors
        }
    }

    // OrderCardAdapter.OnOrderActionListener
    @Override
    public void onStatusChange(Order order, String newStatus) {
        try {
            orderRepo.updateOrderStatus(order.getOrderId(), newStatus);
            String msg = "";
            switch (newStatus) {
                case Constants.ORDER_STATUS_PREPARING:
                    msg = order.getDisplayLabel() + " being prepared";
                    break;
                case Constants.ORDER_STATUS_READY:
                    msg = order.getDisplayLabel() + " is READY!";
                    break;
                case Constants.ORDER_STATUS_PICKED_UP:
                    msg = order.getDisplayLabel() + " picked up";
                    break;
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
