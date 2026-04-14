package com.yumkitchen.sync.ui.main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.CouchbaseManager;
import com.yumkitchen.sync.data.PeerEventBus;
import com.yumkitchen.sync.data.model.DeviceRole;
import com.yumkitchen.sync.data.repository.OrderRepository;
import com.yumkitchen.sync.ui.discovery.PeerDiscoveryFragment;
import com.yumkitchen.sync.ui.kitchen.KitchenDisplayFragment;
import com.yumkitchen.sync.ui.manager.ManagerDashboardFragment;
import com.yumkitchen.sync.ui.statusboard.OrderStatusBoardFragment;
import com.yumkitchen.sync.ui.waiter.WaiterMenuFragment;
import com.yumkitchen.sync.util.Constants;

public class MainActivity extends AppCompatActivity implements PeerEventBus.PeerEventListener {

    private DeviceRole currentRole;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private WaiterMenuFragment kioskFragment;
    private OrderStatusBoardFragment statusBoardFragment;
    private KitchenDisplayFragment kitchenFragment;
    private ManagerDashboardFragment storeManagerFragment;
    private PeerDiscoveryFragment peerFragment;
    private int peerCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String roleName = getIntent().getStringExtra(Constants.EXTRA_ROLE);
        currentRole = roleName != null ? DeviceRole.valueOf(roleName) : DeviceRole.KIOSK;

        setupToolbar();
        setupBottomNav();

        PeerEventBus.getInstance().register(this);
    }

    @Override
    protected void onDestroy() {
        PeerEventBus.getInstance().unregister(this);
        CouchbaseManager.getInstance().close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reset) {
            showResetDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        updateSubtitle();
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_kiosk) {
                showFragment(getKioskFragment());
                return true;
            } else if (id == R.id.nav_status_board) {
                showFragment(getStatusBoardFragment());
                return true;
            } else if (id == R.id.nav_kitchen) {
                showFragment(getKitchenFragment());
                return true;
            } else if (id == R.id.nav_store_mgr) {
                showFragment(getStoreManagerFragment());
                return true;
            } else if (id == R.id.nav_peers) {
                showFragment(getPeerFragment());
                return true;
            }
            return false;
        });

        // Select the tab matching the initial role
        switch (currentRole) {
            case KIOSK:
                bottomNav.setSelectedItemId(R.id.nav_kiosk);
                break;
            case KITCHEN:
                bottomNav.setSelectedItemId(R.id.nav_kitchen);
                break;
            case STORE_MANAGER:
                bottomNav.setSelectedItemId(R.id.nav_store_mgr);
                break;
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private WaiterMenuFragment getKioskFragment() {
        if (kioskFragment == null) kioskFragment = new WaiterMenuFragment();
        return kioskFragment;
    }

    private OrderStatusBoardFragment getStatusBoardFragment() {
        if (statusBoardFragment == null) statusBoardFragment = new OrderStatusBoardFragment();
        return statusBoardFragment;
    }

    private KitchenDisplayFragment getKitchenFragment() {
        if (kitchenFragment == null) kitchenFragment = new KitchenDisplayFragment();
        return kitchenFragment;
    }

    private ManagerDashboardFragment getStoreManagerFragment() {
        if (storeManagerFragment == null) storeManagerFragment = new ManagerDashboardFragment();
        return storeManagerFragment;
    }

    private PeerDiscoveryFragment getPeerFragment() {
        if (peerFragment == null) peerFragment = new PeerDiscoveryFragment();
        return peerFragment;
    }

    private void updateSubtitle() {
        String subtitle = currentRole.getDisplayName();
        if (peerCount > 0) {
            subtitle += " \u00B7 " + peerCount + " peer" + (peerCount != 1 ? "s" : "") + " connected";
        }
        toolbar.setSubtitle(subtitle);
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_demo)
                .setMessage("This will delete all orders and restart. Continue?")
                .setPositiveButton("Reset", (d, w) -> {
                    try {
                        new OrderRepository().deleteAllOrders();
                        Toast.makeText(this, "Demo reset complete", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // PeerEventBus.PeerEventListener
    @Override
    public void onPeerDiscovered(String peerId, boolean isOnline) {
        peerCount = CouchbaseManager.getInstance().getNeighborPeers().size();
        updateSubtitle();
    }
}
