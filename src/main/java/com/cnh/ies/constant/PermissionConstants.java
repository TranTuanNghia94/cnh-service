package com.cnh.ies.constant;

/**
 * Permission codes stored in {@code permissions.code} and assigned via {@code role_permissions}.
 */
public final class PermissionConstants {

    private PermissionConstants() {}

    // User management (admin)
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_READ = "USER_READ";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_RESTORE = "USER_RESTORE";
    public static final String USER_ACTIVATE = "USER_ACTIVATE";
    public static final String USER_RESET_PASSWORD = "USER_RESET_PASSWORD";

    // User self-service
    public static final String USER_SELF_READ = "USER_SELF_READ";
    public static final String USER_SELF_UPDATE_PROFILE = "USER_SELF_UPDATE_PROFILE";
    public static final String USER_SELF_CHANGE_PASSWORD = "USER_SELF_CHANGE_PASSWORD";

    // Payment workflow
    public static final String PAYMENT_READ = "PAYMENT_READ";
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String PAYMENT_APPROVE = "PAYMENT_APPROVE";
    public static final String PAYMENT_REJECT = "PAYMENT_REJECT";
    public static final String PAYMENT_APPROVE_LEVEL_1 = "PAYMENT_APPROVE_LEVEL_1";
    public static final String PAYMENT_APPROVE_LEVEL_2 = "PAYMENT_APPROVE_LEVEL_2";
    public static final String PAYMENT_APPROVE_FINAL = "PAYMENT_APPROVE_FINAL";
    public static final String PAYMENT_UPLOAD_BANK_NOTE = "PAYMENT_UPLOAD_BANK_NOTE";

    // Warehouse inbound
    public static final String WAREHOUSE_INBOUND_READ = "WAREHOUSE_INBOUND_READ";
    public static final String WAREHOUSE_INBOUND_CREATE = "WAREHOUSE_INBOUND_CREATE";
    public static final String WAREHOUSE_INBOUND_UPDATE = "WAREHOUSE_INBOUND_UPDATE";
    public static final String WAREHOUSE_INBOUND_APPROVE_LEVEL_1 = "WAREHOUSE_INBOUND_APPROVE_LEVEL_1";
    public static final String WAREHOUSE_INBOUND_APPROVE_LEVEL_2 = "WAREHOUSE_INBOUND_APPROVE_LEVEL_2";
    public static final String WAREHOUSE_INBOUND_APPROVE_FINAL = "WAREHOUSE_INBOUND_APPROVE_FINAL";

    // Warehouse outbound
    public static final String WAREHOUSE_OUTBOUND_READ = "WAREHOUSE_OUTBOUND_READ";
    public static final String WAREHOUSE_OUTBOUND_CREATE = "WAREHOUSE_OUTBOUND_CREATE";
    public static final String WAREHOUSE_OUTBOUND_UPDATE = "WAREHOUSE_OUTBOUND_UPDATE";
    public static final String WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1 = "WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1";
    public static final String WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2 = "WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2";
    public static final String WAREHOUSE_OUTBOUND_APPROVE_FINAL = "WAREHOUSE_OUTBOUND_APPROVE_FINAL";

    // Warehouse inventory
    public static final String WAREHOUSE_INVENTORY_READ = "WAREHOUSE_INVENTORY_READ";

    /** Workflow stage labels persisted on approval rows (not authorization codes). */
    public static final String APPROVAL_STAGE_ACCOUNTANT = "ACCOUNTANT";
    public static final String APPROVAL_STAGE_HEAD_ACCOUNTANT = "HEAD_ACCOUNTANT";
    public static final String APPROVAL_STAGE_FINAL_APPROVER = "FINAL_APPROVER";
}
