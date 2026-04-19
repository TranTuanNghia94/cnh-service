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
}