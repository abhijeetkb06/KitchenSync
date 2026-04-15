package com.kitchensync.ui.waiter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kitchensync.R;
import com.kitchensync.data.model.MenuItem;
import com.kitchensync.util.MenuImageMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onAddToCart(MenuItem item);
    }

    private List<MenuItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public MenuItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<MenuItem> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        int resId = MenuImageMapper.getDrawableRes(item.getImageKey());
        if (resId != 0) {
            holder.foodImage.setImageResource(resId);
        }
        holder.name.setText(item.getName());
        holder.price.setText(String.format(Locale.US, "$%.2f", item.getPrice()));
        holder.btnAdd.setOnClickListener(v -> listener.onAddToCart(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImage;
        TextView name, price;
        MaterialButton btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            foodImage = itemView.findViewById(R.id.image_food);
            name = itemView.findViewById(R.id.text_name);
            price = itemView.findViewById(R.id.text_price);
            btnAdd = itemView.findViewById(R.id.btn_add);
        }
    }
}
