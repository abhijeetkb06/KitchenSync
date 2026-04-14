package com.yumkitchen.sync.util;

public final class Constants {
    private Constants() {}

    public static final String DATABASE_NAME = "yumkitchen";
    public static final String PEER_GROUP_ID = "com.yumkitchen.sync";
    public static final String IDENTITY_LABEL = "com.yumkitchen.identity";
    public static final String COLLECTION_NAME = "_default";

    public static final String DOC_TYPE_ORDER = "order";
    public static final String DOC_TYPE_MENU_ITEM = "menu_item";
    public static final String DOC_TYPE_DEVICE = "device";

    public static final String ORDER_STATUS_NEW = "new";
    public static final String ORDER_STATUS_PREPARING = "preparing";
    public static final String ORDER_STATUS_READY = "ready";
    public static final String ORDER_STATUS_PICKED_UP = "picked_up";

    public static final String EXTRA_ROLE = "extra_role";
}
