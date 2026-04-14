package com.yumkitchen.sync.ui.discovery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yumkitchen.sync.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.ViewHolder> {

    public static class PeerInfo {
        public String peerId;
        public String name;
        public String role;
        public String status;
        public boolean isOnline;

        public PeerInfo(String peerId, String name, String role, boolean isOnline) {
            this.peerId = peerId;
            this.name = name;
            this.role = role;
            this.status = "connected";
            this.isOnline = isOnline;
        }
    }

    private final List<PeerInfo> peers = new ArrayList<>();
    private final Map<String, PeerInfo> peerMap = new HashMap<>();

    public void addOrUpdatePeer(PeerInfo info) {
        PeerInfo existing = peerMap.get(info.peerId);
        if (existing != null) {
            int index = peers.indexOf(existing);
            if (index >= 0) {
                peers.set(index, info);
                peerMap.put(info.peerId, info);
                notifyItemChanged(index);
                return;
            }
        }
        peers.add(info);
        peerMap.put(info.peerId, info);
        notifyItemInserted(peers.size() - 1);
    }

    public void removePeer(String peerId) {
        PeerInfo info = peerMap.remove(peerId);
        if (info != null) {
            int index = peers.indexOf(info);
            if (index >= 0) {
                peers.remove(index);
                notifyItemRemoved(index);
            }
        }
    }

    public void updatePeerStatus(String peerId, String status) {
        PeerInfo info = peerMap.get(peerId);
        if (info != null) {
            info.status = status;
            int index = peers.indexOf(info);
            if (index >= 0) {
                notifyItemChanged(index);
            }
        }
    }

    public void clear() {
        int size = peers.size();
        peers.clear();
        peerMap.clear();
        notifyItemRangeRemoved(0, size);
    }

    public int getPeerCount() {
        return peers.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_peer_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PeerInfo peer = peers.get(position);
        holder.name.setText(peer.name != null ? peer.name : peer.peerId.substring(0, Math.min(8, peer.peerId.length())));
        holder.role.setText(peer.role != null ? peer.role : "Unknown role");
        holder.status.setText(peer.status != null ? peer.status : "connected");

        int statusColor = peer.isOnline
                ? ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green)
                : ContextCompat.getColor(holder.itemView.getContext(), R.color.status_picked_up);
        holder.status.setTextColor(statusColor);

        // Indicator color
        android.graphics.drawable.GradientDrawable indicator =
                new android.graphics.drawable.GradientDrawable();
        indicator.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        indicator.setColor(statusColor);
        holder.indicator.setBackground(indicator);
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View indicator;
        TextView name, role, status;

        ViewHolder(View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.indicator);
            name = itemView.findViewById(R.id.text_peer_name);
            role = itemView.findViewById(R.id.text_peer_role);
            status = itemView.findViewById(R.id.text_peer_status);
        }
    }
}
