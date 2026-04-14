package com.yumkitchen.sync.ui.statusboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.model.Order;

import java.util.ArrayList;
import java.util.List;

public class StatusBoardAdapter extends RecyclerView.Adapter<StatusBoardAdapter.ViewHolder> {

    private List<Order> orders = new ArrayList<>();

    public void setOrders(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_status_board_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.textOrderLabel.setText(order.getDisplayLabel());
        holder.textItemCount.setText(order.getItemCount() + " items");
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textOrderLabel, textItemCount;

        ViewHolder(View itemView) {
            super(itemView);
            textOrderLabel = itemView.findViewById(R.id.text_order_label);
            textItemCount = itemView.findViewById(R.id.text_item_count);
        }
    }
}
