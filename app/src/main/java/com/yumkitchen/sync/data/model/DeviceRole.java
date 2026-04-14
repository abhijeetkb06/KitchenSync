package com.yumkitchen.sync.data.model;

public enum DeviceRole {
    KIOSK("Kiosk", "Self-order station"),
    KITCHEN("Kitchen", "Receives and prepares orders"),
    STORE_MANAGER("Store Manager", "Monitor store operations");

    private final String displayName;
    private final String description;

    DeviceRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
