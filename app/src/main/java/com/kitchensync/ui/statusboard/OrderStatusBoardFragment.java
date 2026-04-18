package com.kitchensync.ui.statusboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.Query;
import com.kitchensync.R;
import com.kitchensync.data.model.Order;
import com.kitchensync.data.repository.OrderRepository;
import com.kitchensync.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class OrderStatusBoardFragment extends Fragment {
    private static final String TAG = "OrderStatusBoard";

    private StatusBoardAdapter newAdapter;
    private StatusBoardAdapter preparingAdapter;
    private StatusBoardAdapter readyAdapter;
    private final OrderRepository orderRepo = new OrderRepository();
    private Query liveQuery;
    private ListenerToken queryToken;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_status_board, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerNew = view.findViewById(R.id.recycler_new);
        newAdapter = new StatusBoardAdapter();
        recyclerNew.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerNew.setAdapter(newAdapter);

        RecyclerView recyclerPreparing = view.findViewById(R.id.recycler_preparing);
        preparingAdapter = new StatusBoardAdapter();
        recyclerPreparing.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPreparing.setAdapter(preparingAdapter);

        RecyclerView recyclerReady = view.findViewById(R.id.recycler_ready);
        readyAdapter = new StatusBoardAdapter();
        recyclerReady.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerReady.setAdapter(readyAdapter);

        startLiveQuery();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
                requireActivity().runOnUiThread(this::refreshBoard);
            });

            // Initial load
            refreshBoard();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up live query", e);
        }
    }

    private void refreshBoard() {
        List<Order> activeOrders = orderRepo.getActiveOrders();

        List<Order> newOrders = new ArrayList<>();
        List<Order> preparing = new ArrayList<>();
        List<Order> ready = new ArrayList<>();

        for (Order order : activeOrders) {
            switch (order.getStatus()) {
                case Constants.ORDER_STATUS_NEW:
                    newOrders.add(order);
                    break;
                case Constants.ORDER_STATUS_PREPARING:
                    preparing.add(order);
                    break;
                case Constants.ORDER_STATUS_READY:
                    ready.add(order);
                    break;
            }
        }

        newAdapter.setOrders(newOrders);
        preparingAdapter.setOrders(preparing);
        readyAdapter.setOrders(ready);
    }
}
