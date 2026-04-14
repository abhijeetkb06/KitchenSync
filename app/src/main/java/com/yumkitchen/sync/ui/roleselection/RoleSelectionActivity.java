package com.yumkitchen.sync.ui.roleselection;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.yumkitchen.sync.R;
import com.yumkitchen.sync.data.CouchbaseManager;
import com.yumkitchen.sync.data.model.DeviceRole;
import com.yumkitchen.sync.data.repository.MenuRepository;
import com.yumkitchen.sync.ui.main.MainActivity;
import com.yumkitchen.sync.util.Constants;
import com.yumkitchen.sync.util.PermissionHelper;

public class RoleSelectionActivity extends AppCompatActivity {
    private static final String TAG = "RoleSelection";

    private DeviceRole pendingRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        initDatabase();
        setupRoleCards();
    }

    private void initDatabase() {
        try {
            CouchbaseManager manager = CouchbaseManager.getInstance();
            manager.init(this);
            manager.openDatabase();
            new MenuRepository().seedMenuIfNeeded();
            Log.i(TAG, "Database initialized and menu seeded");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupRoleCards() {
        MaterialCardView kioskCard = findViewById(R.id.card_kiosk);
        MaterialCardView kitchenCard = findViewById(R.id.card_kitchen);
        MaterialCardView storeManagerCard = findViewById(R.id.card_store_manager);

        kioskCard.setOnClickListener(v -> onRoleSelected(DeviceRole.KIOSK));
        kitchenCard.setOnClickListener(v -> onRoleSelected(DeviceRole.KITCHEN));
        storeManagerCard.setOnClickListener(v -> onRoleSelected(DeviceRole.STORE_MANAGER));
    }

    private void onRoleSelected(DeviceRole role) {
        this.pendingRole = role;
        if (PermissionHelper.hasAllPermissions(this)) {
            launchWithRole(role);
        } else {
            PermissionHelper.requestPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_PERMISSIONS) {
            if (PermissionHelper.allGranted(grantResults)) {
                if (pendingRole != null) {
                    launchWithRole(pendingRole);
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permissions_title)
                        .setMessage(R.string.permissions_message)
                        .setPositiveButton(R.string.grant_permissions, (d, w) -> {
                            PermissionHelper.requestPermissions(this);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }

    private void launchWithRole(DeviceRole role) {
        // Start P2P sync
        try {
            String deviceName = Build.MODEL + " - " + role.getDisplayName();
            CouchbaseManager.getInstance().startPeerSync(deviceName, role);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start peer sync", e);
            Toast.makeText(this, "Sync error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.EXTRA_ROLE, role.name());
        startActivity(intent);
        finish();
    }
}
