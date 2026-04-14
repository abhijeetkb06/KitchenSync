package com.yumkitchen.sync.ui.manager;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.Query;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.CouchbaseManager;
import com.yumkitchen.sync.data.PeerEventBus;
import com.yumkitchen.sync.data.model.Order;
import com.yumkitchen.sync.data.repository.OrderRepository;
import com.yumkitchen.sync.util.Constants;

import java.util.List;
import java.util.Locale;

public class ManagerDashboardFragment extends Fragment implements PeerEventBus.PeerEventListener {
    private static final String TAG = "ManagerDashboard";

    private TextView textSyncStatus, textPeerSummary;
    private View indicatorSyncStatus;
    private TextView textCountNew, textCountPreparing, textCountReady, textCountPickedUp, textCountTotal;
    private TextView textRevenue;
    private OrderSummaryAdapter orderAdapter;
    private final OrderRepository orderRepo = new OrderRepository();
    private Query liveQuery;
    private ListenerToken queryToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manager_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Network status
        indicatorSyncStatus = view.findViewById(R.id.indicator_sync_status);
        textSyncStatus = view.findViewById(R.id.text_sync_status);
        textPeerSummary = view.findViewById(R.id.text_peer_summary);

        // Order stats
        textCountNew = view.findViewById(R.id.text_count_new);
        textCountPreparing = view.findViewById(R.id.text_count_preparing);
        textCountReady = view.findViewById(R.id.text_count_ready);
        textCountPickedUp = view.findViewById(R.id.text_count_picked_up);
        textCountTotal = view.findViewById(R.id.text_count_total);
        textRevenue = view.findViewById(R.id.text_revenue);

        // Recent orders
        RecyclerView recyclerOrders = view.findViewById(R.id.recycler_recent_orders);
        orderAdapter = new OrderSummaryAdapter();
        recyclerOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerOrders.setAdapter(orderAdapter);

        updateNetworkStatus();
        startLiveQuery();
    }

    @Override
    public void onResume() {
        super.onResume();
        PeerEventBus.getInstance().register(this);
        updateNetworkStatus();
    }

    @Override
    public void onPause() {
        PeerEventBus.getInstance().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (queryToken != null) {
            queryToken.remove();
            queryToken = null;
        }
    }

    private void updateNetworkStatus() {
        int peerCount = CouchbaseManager.getInstance().getNeighborPeers().size();
        if (peerCount > 0) {
            textPeerSummary.setText(getString(R.string.peers_connected, peerCount));
            textSyncStatus.setText(getString(R.string.replicator_active));
            setIndicatorColor(R.color.success_green);
        } else {
            textPeerSummary.setText(getString(R.string.no_peers));
            textSyncStatus.setText(getString(R.string.replicator_active));
            setIndicatorColor(R.color.yum_amber);
        }
    }

    private void setIndicatorColor(int colorRes) {
        GradientDrawable indicator = new GradientDrawable();
        indicator.setShape(GradientDrawable.OVAL);
        indicator.setColor(ContextCompat.getColor(requireContext(), colorRes));
        indicatorSyncStatus.setBackground(indicator);
    }

    private void startLiveQuery() {
        try {
            liveQuery = orderRepo.createOrdersLiveQuery();
            if (liveQuery == null) return;

            queryToken = liveQuery.addChangeListener(change -> {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(this::refreshOrderData);
            });

            // Initial load
            refreshOrderData();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up live query", e);
        }
    }

    private void refreshOrderData() {
        List<Order> allOrders = orderRepo.getOrders(null);
        orderAdapter.setOrders(allOrders);

        int newCount = 0, preparingCount = 0, readyCount = 0, pickedUpCount = 0;
        double totalRevenue = 0;
        for (Order order : allOrders) {
            totalRevenue += order.getTotalAmount();
            switch (order.getStatus()) {
                case Constants.ORDER_STATUS_NEW:
                    newCount++;
                    break;
                case Constants.ORDER_STATUS_PREPARING:
                    preparingCount++;
                    break;
                case Constants.ORDER_STATUS_READY:
                    readyCount++;
                    break;
                case Constants.ORDER_STATUS_PICKED_UP:
                    pickedUpCount++;
                    break;
            }
        }

        textCountNew.setText(String.valueOf(newCount));
        textCountPreparing.setText(String.valueOf(preparingCount));
        textCountReady.setText(String.valueOf(readyCount));
        textCountPickedUp.setText(String.valueOf(pickedUpCount));
        textCountTotal.setText(String.valueOf(allOrders.size()));
        textRevenue.setText(String.format(Locale.US, "$%.2f", totalRevenue));
    }

    // PeerEventBus.PeerEventListener
    @Override
    public void onPeerDiscovered(String peerId, boolean isOnline) {
        if (!isAdded()) return;
        updateNetworkStatus();
    }

    @Override
    public void onReplicatorStatusChanged(boolean isActive, String error) {
        if (!isAdded()) return;
        if (isActive) {
            textSyncStatus.setText(getString(R.string.replicator_active));
            setIndicatorColor(R.color.success_green);
        } else {
            textSyncStatus.setText(getString(R.string.replicator_inactive));
            setIndicatorColor(R.color.status_picked_up);
        }
    }
}
