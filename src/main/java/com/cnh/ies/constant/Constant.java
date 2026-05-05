package com.cnh.ies.constant;

import java.util.Map;

public class Constant {
    public static final String SUCCESS = "Success";
    public static final String ERROR = "Error";
    public static final String WARNING = "Warning";
    public static final String PENDING = "Pending";



    public static final String ORDER_STATUS_DRAFT = "DRAFT"; // Bản nháp
    public static final String ORDER_STATUS_PENDING = "PENDING"; // Chờ thanh toán
    public static final String ORDER_STATUS_PAID = "PAID"; // Đã thanh toán
    public static final String ORDER_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID"; // Đã thanh toán một phần
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED"; // Đã giao hàng
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED"; // Đã hoàn thành
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED"; // Đã hủy
    public static final String ORDER_STATUS_REJECTED = "REJECTED"; // Đã từ chối

    public static final Map<String, String> ORDER_STATUS_MAP = Map.of(
        ORDER_STATUS_DRAFT, ORDER_STATUS_DRAFT,
        ORDER_STATUS_PENDING, ORDER_STATUS_PENDING,
        ORDER_STATUS_PAID, ORDER_STATUS_PAID,
        ORDER_STATUS_PARTIALLY_PAID, ORDER_STATUS_PARTIALLY_PAID,
        ORDER_STATUS_SHIPPED, ORDER_STATUS_SHIPPED,
        ORDER_STATUS_COMPLETED, ORDER_STATUS_COMPLETED,
        ORDER_STATUS_CANCELLED, ORDER_STATUS_CANCELLED,
        ORDER_STATUS_REJECTED, ORDER_STATUS_REJECTED
    );

    // Purchase Order statuses
    public static final String PO_STATUS_DRAFT = "DRAFT"; // Bản nháp
    public static final String PO_STATUS_PENDING = "PENDING"; // Chờ duyệt
    public static final String PO_STATUS_APPROVED = "APPROVED"; // Đã duyệt
    public static final String PO_STATUS_ORDERED = "ORDERED"; // Đã đặt hàng
    public static final String PO_STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED"; // Nhận một phần
    public static final String PO_STATUS_RECEIVED = "RECEIVED"; // Đã nhận hàng
    public static final String PO_STATUS_COMPLETED = "COMPLETED"; // Đã hoàn thành
    public static final String PO_STATUS_CANCELLED = "CANCELLED"; // Đã hủy

    public static final Map<String, String> PO_STATUS_MAP = Map.of(
        PO_STATUS_DRAFT, PO_STATUS_DRAFT,
        PO_STATUS_PENDING, PO_STATUS_PENDING,
        PO_STATUS_APPROVED, PO_STATUS_APPROVED,
        PO_STATUS_ORDERED, PO_STATUS_ORDERED,
        PO_STATUS_PARTIALLY_RECEIVED, PO_STATUS_PARTIALLY_RECEIVED,
        PO_STATUS_RECEIVED, PO_STATUS_RECEIVED,
        PO_STATUS_COMPLETED, PO_STATUS_COMPLETED,
        PO_STATUS_CANCELLED, PO_STATUS_CANCELLED
    );

    // Payment Request statuses
    public static final String PAYMENT_REQUEST_STATUS_DRAFT = "DRAFT";
    public static final String PAYMENT_REQUEST_STATUS_SUBMITTED = "SUBMITTED";
    public static final String PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL = "PENDING_ACC_APPROVAL";
    public static final String PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL = "PENDING_HEAD_ACC_APR";
    public static final String PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL = "PENDING_FINAL_APR";
    public static final String PAYMENT_REQUEST_STATUS_APPROVED = "APPROVED";
    public static final String PAYMENT_REQUEST_STATUS_PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String PAYMENT_REQUEST_STATUS_PAID = "PAID";
    public static final String PAYMENT_REQUEST_STATUS_REJECTED = "REJECTED";
    public static final String PAYMENT_REQUEST_STATUS_CANCELLED = "CANCELLED";

    public static final Map<String, String> PAYMENT_REQUEST_STATUS_MAP = Map.of(
        PAYMENT_REQUEST_STATUS_DRAFT, PAYMENT_REQUEST_STATUS_DRAFT,
        PAYMENT_REQUEST_STATUS_SUBMITTED, PAYMENT_REQUEST_STATUS_SUBMITTED,
        PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL, PAYMENT_REQUEST_STATUS_PENDING_ACCOUNTANT_APPROVAL,
        PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL, PAYMENT_REQUEST_STATUS_PENDING_HEAD_ACCOUNTANT_APPROVAL,
        PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL, PAYMENT_REQUEST_STATUS_PENDING_FINAL_APPROVAL,
        PAYMENT_REQUEST_STATUS_APPROVED, PAYMENT_REQUEST_STATUS_APPROVED,
        PAYMENT_REQUEST_STATUS_PARTIALLY_PAID, PAYMENT_REQUEST_STATUS_PARTIALLY_PAID,
        PAYMENT_REQUEST_STATUS_PAID, PAYMENT_REQUEST_STATUS_PAID,
        PAYMENT_REQUEST_STATUS_REJECTED, PAYMENT_REQUEST_STATUS_REJECTED,
        PAYMENT_REQUEST_STATUS_CANCELLED, PAYMENT_REQUEST_STATUS_CANCELLED
    );

    // Payment request approval statuses
    public static final String PAYMENT_APPROVAL_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_APPROVAL_STATUS_APPROVED = "APPROVED";
    public static final String PAYMENT_APPROVAL_STATUS_REJECTED = "REJECTED";

    // Role codes (from database)
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_ACCOUNTANT = "ACCOUNTANT";
    public static final String ROLE_ACCOUNTANT_MANAGER = "ACCOUNTANT_MANAGER";
    public static final String ROLE_CS = "CS";
    public static final String ROLE_CS_MANAGER = "CS_MANAGER";
    public static final String ROLE_WAREHOUSE_KEEPER = "WAREHOUSE_KEEPER";

    /** Warehouse inbound receipt lifecycle (N-level approvals, same mechanics as payment request). */
    public static final String WAREHOUSE_INBOUND_STATUS_DRAFT = "DRAFT";
    public static final String WAREHOUSE_INBOUND_STATUS_SUBMITTED = "SUBMITTED";
    public static final String WAREHOUSE_INBOUND_STATUS_APPROVED = "APPROVED";
    public static final String WAREHOUSE_INBOUND_STATUS_REJECTED = "REJECTED";
    public static final String WAREHOUSE_INBOUND_STATUS_CANCELLED = "CANCELLED";

    /** Stock ledger directions (warehouse_stock_transactions.direction). */
    public static final String WAREHOUSE_STOCK_DIRECTION_INBOUND = "INBOUND";
    public static final String WAREHOUSE_STOCK_DIRECTION_OUTBOUND = "OUTBOUND";

    public static final String WAREHOUSE_STOCK_REF_INBOUND_RECEIPT_LINE = "WAREHOUSE_INBOUND_RECEIPT_LINE";
    public static final String WAREHOUSE_STOCK_REF_OUTBOUND_DETAIL = "WAREHOUSE_OUTBOUND_DETAIL";

    public static final String WAREHOUSE_OUTBOUND_STATUS_DRAFT = "DRAFT";
    public static final String WAREHOUSE_OUTBOUND_STATUS_SUBMITTED = "SUBMITTED";
    public static final String WAREHOUSE_OUTBOUND_STATUS_APPROVED = "APPROVED";
    public static final String WAREHOUSE_OUTBOUND_STATUS_REJECTED = "REJECTED";
    public static final String WAREHOUSE_OUTBOUND_STATUS_CANCELLED = "CANCELLED";

    public static final String PRODUCT_TAX_SOURCE_WAREHOUSE_INBOUND_LINE = "WAREHOUSE_INBOUND_RECEIPT_LINE";
}