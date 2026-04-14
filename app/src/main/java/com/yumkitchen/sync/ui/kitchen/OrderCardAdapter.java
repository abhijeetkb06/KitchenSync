package com.yumkitchen.sync.ui.kitchen;

import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.model.Order;
import com.yumkitchen.sync.data.model.OrderItem;
import com.yumkitchen.sync.util.Constants;
import com.yumkitchen.sync.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class OrderCardAdapter extends RecyclerView.Adapter<OrderCardAdapter.ViewHolder> {

    public interface OnOrderActionListener {
        void onStatusChange(Order order, String newStatus);
    }

    private List<Order> orders = new ArrayList<>();
    private final OnOrderActionListener listener;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    public OrderCardAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
        notifyDataSetChanged();
        startTimer();
    }

    public void startTimer() {
        stopTimer();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                timerHandler.postDelayed(this, 10000); // Update every 10 seconds
            }
        };
        timerHandler.postDelayed(timerRunnable, 10000);
    }

    public void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout header;
        TextView textTable, textOrderId, textElapsed;
        LinearLayout itemsContainer;
        MaterialButton btnAction;

        ViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            textTable = itemView.findViewById(R.id.text_table);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            textElapsed = itemView.findViewById(R.id.text_elapsed);
            itemsContainer = itemView.findViewById(R.id.items_container);
            btnAction = itemView.findViewById(R.id.btn_action);
        }

        void bind(Order order) {
            textTable.setText(order.getDisplayLabel());
            textOrderId.setText(order.getShortId());
            textElapsed.setText(TimeUtils.getElapsedMinutes(order.getCreatedAt()));

            // Set header color based on status
            int bgRes;
            switch (order.getStatus()) {
                case Constants.ORDER_STATUS_PREPARING:
                    bgRes = R.drawable.bg_status_preparing;
                    break;
                case Constants.ORDER_STATUS_READY:
                    bgRes = R.drawable.bg_status_ready;
                    break;
                default:
                    bgRes = R.drawable.bg_status_new;
                    break;
            }
            header.setBackgroundResource(bgRes);

            // Populate items
            itemsContainer.removeAllViews();
            for (OrderItem item : order.getItems()) {
                LinearLayout row = new LinearLayout(itemView.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 4, 0, 4);

                // Emoji
                String emoji = item.getEmoji();
                if (emoji != null && !emoji.isEmpty()) {
                    TextView emojiTv = new TextView(itemView.getContext());
                    emojiTv.setText(emoji);
                    emojiTv.setTextSize(20);
                    emojiTv.setPadding(0, 0, 12, 0);
                    row.addView(emojiTv);
                }

                // Item text
                TextView tv = new TextView(itemView.getContext());
                tv.setText(item.getQuantity() + "x  " + item.getName()
                        + (item.getNotes() != null && !item.getNotes().isEmpty()
                        ? "  (" + item.getNotes() + ")" : ""));
                tv.setTextSize(14);
                tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.on_surface));

                row.addView(tv);
                itemsContainer.addView(row);
            }

            // Action button
            switch (order.getStatus()) {
                case Constants.ORDER_STATUS_NEW:
                    btnAction.setText(R.string.start_preparing);
                    btnAction.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.yum_amber));
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setOnClickListener(v ->
                            listener.onStatusChange(order, Constants.ORDER_STATUS_PREPARING));
                    break;
                case Constants.ORDER_STATUS_PREPARING:
                    btnAction.setText(R.string.mark_ready);
                    btnAction.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.success_green));
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setOnClickListener(v ->
                            listener.onStatusChange(order, Constants.ORDER_STATUS_READY));
                    break;
                case Constants.ORDER_STATUS_READY:
                    btnAction.setText(R.string.mark_picked_up);
                    btnAction.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.status_picked_up));
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setOnClickListener(v ->
                            listener.onStatusChange(order, Constants.ORDER_STATUS_PICKED_UP));
                    break;
                default:
                    btnAction.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
