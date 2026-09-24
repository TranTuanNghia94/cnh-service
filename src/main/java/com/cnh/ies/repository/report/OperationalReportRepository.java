package com.cnh.ies.repository.report;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.cnh.ies.model.report.OperationalReportRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OperationalReportRepository {

    private static final String PAYMENT_ACTIVE_STATUSES =
            "'PAID','APPROVED','PARTIALLY_PAID','SUBMITTED','PENDING_ACC_APPROVAL','PENDING_HEAD_ACC_APR','PENDING_FINAL_APR'";

    private static final String SALES_STATUSES = PAYMENT_ACTIVE_STATUSES + ",'DRAFT'";

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> stockRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT CAST(p.id AS text),
                       COALESCE(c.name, ''),
                       p.code,
                       p.name,
                       COALESCE(p.unit_1, ''),
                       COALESCE(SUM(CASE
                           WHEN t.created_at < :start AND t.is_deleted = false AND t.direction = 'INBOUND' THEN t.quantity
                           WHEN t.created_at < :start AND t.is_deleted = false AND t.direction = 'OUTBOUND' THEN -t.quantity
                           ELSE 0 END), 0),
                       COALESCE(SUM(CASE
                           WHEN t.created_at >= :start AND t.created_at < :end AND t.is_deleted = false AND t.direction = 'INBOUND'
                           THEN t.quantity ELSE 0 END), 0),
                       COALESCE(SUM(CASE
                           WHEN t.created_at >= :start AND t.created_at < :end AND t.is_deleted = false AND t.direction = 'OUTBOUND'
                           THEN t.quantity ELSE 0 END), 0),
                       ROUND(COALESCE(p.cost_price, 0))
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN warehouse_stock_transactions t ON t.product_id = p.id
                WHERE p.is_deleted = false
                  AND (:productName = '' OR LOWER(COALESCE(p.name, '')) LIKE :productNameLike)
                  AND (:productCode = '' OR LOWER(COALESCE(p.code, '')) LIKE :productCodeLike)
                GROUP BY p.id, c.name, p.code, p.name, p.unit_1, p.cost_price
                ORDER BY p.code
                """;
        Query query = bindRange(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "productName", request.getProductName());
        bindContains(query, "productCode", request.getProductCode());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> outboundSummaryRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT COALESCE(u.full_name, o.created_by, ''),
                       o.outbound_date,
                       o.outbound_number,
                       o.contract_number,
                       COALESCE(cu.name, ''),
                       COALESCE(o.total_amount, 0) - COALESCE(o.tax_amount, 0),
                       COALESCE(o.tax_amount, 0),
                       COALESCE(o.total_amount, 0)
                FROM warehouse_outbounds o
                LEFT JOIN orders ord ON ord.id = o.order_id
                LEFT JOIN customers cu ON cu.id = ord.customer_id
                LEFT JOIN users u ON u.username = o.created_by OR CAST(u.id AS text) = o.created_by
                WHERE o.is_deleted = false
                  AND o.status = 'APPROVED'
                  AND o.outbound_date >= :startDate
                  AND o.outbound_date < :endDate
                  AND (:userName = '' OR LOWER(COALESCE(u.full_name, o.created_by, '')) LIKE :userNameLike)
                  AND (:contractNumber = '' OR LOWER(COALESCE(o.contract_number, '')) LIKE :contractNumberLike)
                  AND (:customer = '' OR LOWER(COALESCE(cu.name, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.code, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.misa_code, '')) LIKE :customerLike)
                  AND (:documentNumber = '' OR LOWER(COALESCE(o.outbound_number, '')) LIKE :documentNumberLike)
                ORDER BY o.outbound_date, o.outbound_number
                """;
        Query query = bindDates(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "userName", request.getUserName());
        bindContains(query, "contractNumber", request.getContractNumber());
        bindContains(query, "customer", request.getCustomer());
        bindContains(query, "documentNumber", request.getDocumentNumber());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> outboundDetailRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT o.outbound_date,
                       o.outbound_number,
                       COALESCE(cu.misa_code, cu.code, ''),
                       COALESCE(cu.name, ''),
                       COALESCE(p.code, ''),
                       COALESCE(p.name, ''),
                       COALESCE(d.quantity, 0),
                       COALESCE(d.unit_price, 0),
                       COALESCE(d.vat, 0),
                       COALESCE(d.tax_amount, 0)
                FROM warehouse_outbound_details d
                JOIN warehouse_outbounds o ON o.id = d.outbound_id
                LEFT JOIN orders ord ON ord.id = o.order_id
                LEFT JOIN customers cu ON cu.id = ord.customer_id
                LEFT JOIN products p ON p.id = d.product_id
                LEFT JOIN users u ON u.username = o.created_by OR CAST(u.id AS text) = o.created_by
                WHERE d.is_deleted = false
                  AND o.is_deleted = false
                  AND o.status = 'APPROVED'
                  AND o.outbound_date >= :startDate
                  AND o.outbound_date < :endDate
                  AND (:productName = '' OR LOWER(COALESCE(p.name, '')) LIKE :productNameLike)
                  AND (:customer = '' OR LOWER(COALESCE(cu.name, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.code, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.misa_code, '')) LIKE :customerLike)
                  AND (:documentNumber = '' OR LOWER(COALESCE(o.outbound_number, '')) LIKE :documentNumberLike)
                  AND (:contractNumber = '' OR LOWER(COALESCE(o.contract_number, '')) LIKE :contractNumberLike)
                  AND (:userName = '' OR LOWER(COALESCE(u.full_name, o.created_by, '')) LIKE :userNameLike)
                ORDER BY o.outbound_date, o.outbound_number, p.code
                """;
        Query query = bindDates(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "productName", request.getProductName());
        bindContains(query, "customer", request.getCustomer());
        bindContains(query, "documentNumber", request.getDocumentNumber());
        bindContains(query, "contractNumber", request.getContractNumber());
        bindContains(query, "userName", request.getUserName());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> inboundSummaryRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT COALESCE(u.full_name, r.created_by, ''),
                       r.received_date,
                       COALESCE(r.receipt_number, ''),
                       COALESCE((
                           SELECT ord.contract_number
                           FROM payment_request_purchase_order_lines link
                           JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                           JOIN purchase_orders po ON po.id = pol.purchase_order_id
                           JOIN orders ord ON ord.id = po.order_id
                           WHERE link.payment_request_id = r.payment_request_id
                             AND link.is_deleted = false
                           LIMIT 1
                       ), ''),
                       COALESCE(v.name, ''),
                       COALESCE(r.currency, v.currency, ''),
                       COALESCE(r.real_bill_amount, 0),
                       COALESCE((
                           SELECT SUM(l.quantity_received * COALESCE(pol.unit_price, 0) * COALESCE(l.tax_percent, 0) / 100)
                           FROM warehouse_inbound_receipt_lines l
                           LEFT JOIN purchase_order_lines pol ON pol.id = l.purchase_order_line_id
                           WHERE l.receipt_id = r.id AND l.is_deleted = false
                       ), 0),
                       COALESCE(r.fee_amount, 0),
                       COALESCE(r.exchange_rate, 1)
                FROM warehouse_inbound_receipts r
                LEFT JOIN payment_requests pr ON pr.id = r.payment_request_id
                LEFT JOIN vendors v ON v.id = pr.vendor_id
                LEFT JOIN users u ON u.username = r.created_by OR CAST(u.id AS text) = r.created_by
                WHERE r.is_deleted = false
                  AND r.status = 'APPROVED'
                  AND r.received_date >= :startDate
                  AND r.received_date < :endDate
                  AND (:userName = '' OR LOWER(COALESCE(u.full_name, r.created_by, '')) LIKE :userNameLike)
                  AND (:contractNumber = '' OR LOWER(COALESCE((
                           SELECT ord.contract_number
                           FROM payment_request_purchase_order_lines link
                           JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                           JOIN purchase_orders po ON po.id = pol.purchase_order_id
                           JOIN orders ord ON ord.id = po.order_id
                           WHERE link.payment_request_id = r.payment_request_id
                             AND link.is_deleted = false
                           LIMIT 1
                       ), '')) LIKE :contractNumberLike)
                  AND (:vendor = '' OR LOWER(COALESCE(v.name, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.code, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.misa_code, '')) LIKE :vendorLike)
                  AND (:documentNumber = '' OR LOWER(COALESCE(r.receipt_number, '')) LIKE :documentNumberLike)
                ORDER BY r.received_date, r.receipt_number
                """;
        Query query = bindDates(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "userName", request.getUserName());
        bindContains(query, "contractNumber", request.getContractNumber());
        bindContains(query, "vendor", request.getVendor());
        bindContains(query, "documentNumber", request.getDocumentNumber());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> inboundDetailRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT r.received_date,
                       COALESCE(r.receipt_number, ''),
                       COALESCE(pol.invoice, ''),
                       COALESCE(v.misa_code, v.code, ''),
                       COALESCE(p.code, ''),
                       COALESCE(p.name, ''),
                       COALESCE(l.quantity_received, 0),
                       COALESCE(pol.unit_price, 0),
                       COALESCE(r.currency, pol.currency, ''),
                       COALESCE(r.exchange_rate, pol.exchange_rate, 1),
                       COALESCE(l.tax_percent, 0),
                       COALESCE(l.bill_on_paper, ''),
                       COALESCE(l.line_note, '')
                FROM warehouse_inbound_receipt_lines l
                JOIN warehouse_inbound_receipts r ON r.id = l.receipt_id
                LEFT JOIN purchase_order_lines pol ON pol.id = l.purchase_order_line_id
                LEFT JOIN vendors v ON v.id = pol.vendor_id
                LEFT JOIN products p ON p.id = pol.product_id
                LEFT JOIN purchase_orders po ON po.id = pol.purchase_order_id
                LEFT JOIN orders ord ON ord.id = po.order_id
                LEFT JOIN users u ON u.username = r.created_by OR CAST(u.id AS text) = r.created_by
                WHERE l.is_deleted = false
                  AND r.is_deleted = false
                  AND r.status = 'APPROVED'
                  AND r.received_date >= :startDate
                  AND r.received_date < :endDate
                  AND (:productName = '' OR LOWER(COALESCE(p.name, '')) LIKE :productNameLike)
                  AND (:vendor = '' OR LOWER(COALESCE(v.name, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.code, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.misa_code, '')) LIKE :vendorLike)
                  AND (:documentNumber = '' OR LOWER(COALESCE(r.receipt_number, '')) LIKE :documentNumberLike)
                  AND (:contractNumber = '' OR LOWER(COALESCE(ord.contract_number, '')) LIKE :contractNumberLike)
                ORDER BY r.received_date, r.receipt_number, p.code
                """;
        Query query = bindDates(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "productName", request.getProductName());
        bindContains(query, "vendor", request.getVendor());
        bindContains(query, "documentNumber", request.getDocumentNumber());
        bindContains(query, "contractNumber", request.getContractNumber());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> paymentRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        String sql = """
                SELECT pr.request_number,
                       COALESCE(pr.notes, ''),
                       pr.status,
                       COALESCE(pr.total_amount, 0),
                       pr.paid_at,
                       pr.request_date,
                       COALESCE(pr.currency, ''),
                       COALESCE(v.code, ''),
                       COALESCE(v.misa_code, ''),
                       COALESCE(v.currency, ''),
                       COALESCE((
                           SELECT cu.name
                           FROM payment_request_purchase_order_lines link
                           JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                           JOIN purchase_orders po ON po.id = pol.purchase_order_id
                           JOIN orders ord ON ord.id = po.order_id
                           JOIN customers cu ON cu.id = ord.customer_id
                           WHERE link.payment_request_id = pr.id AND link.is_deleted = false
                           LIMIT 1
                       ), ''),
                       COALESCE(CAST(pr.papers AS text), '')
                FROM payment_requests pr
                LEFT JOIN vendors v ON v.id = pr.vendor_id
                WHERE pr.is_deleted = false
                  AND pr.status IN (%s)
                  AND pr.request_date >= :start
                  AND pr.request_date < :end
                  AND (:vendor = '' OR LOWER(COALESCE(v.name, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.code, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.misa_code, '')) LIKE :vendorLike)
                  AND (:customer = '' OR LOWER(COALESCE((
                           SELECT cu.name
                           FROM payment_request_purchase_order_lines link
                           JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                           JOIN purchase_orders po ON po.id = pol.purchase_order_id
                           JOIN orders ord ON ord.id = po.order_id
                           JOIN customers cu ON cu.id = ord.customer_id
                           WHERE link.payment_request_id = pr.id AND link.is_deleted = false
                           LIMIT 1
                       ), '')) LIKE :customerLike
                       OR LOWER(COALESCE((
                           SELECT COALESCE(cu.misa_code, cu.code, '')
                           FROM payment_request_purchase_order_lines link
                           JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                           JOIN purchase_orders po ON po.id = pol.purchase_order_id
                           JOIN orders ord ON ord.id = po.order_id
                           JOIN customers cu ON cu.id = ord.customer_id
                           WHERE link.payment_request_id = pr.id AND link.is_deleted = false
                           LIMIT 1
                       ), '')) LIKE :customerLike)
                ORDER BY pr.created_at ASC
                """.formatted(PAYMENT_ACTIVE_STATUSES);
        Query query = bindRange(entityManager.createNativeQuery(sql), start, end);
        bindContains(query, "vendor", request.getVendor());
        bindContains(query, "customer", request.getCustomer());
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> vendorInboundSums(LocalDate start, LocalDate end, boolean beforeStart, OperationalReportRequest request) {
        boolean bounded = start != null;
        String dateClause = !bounded
                ? ""
                : beforeStart
                ? "AND r.received_date < :startDate"
                : "AND r.received_date >= :startDate AND r.received_date < :endDate";
        String sql = """
                SELECT CAST(v.id AS text),
                       COALESCE(v.misa_code, v.code, ''),
                       COALESCE(v.name, ''),
                       COALESCE(v.currency, r.currency, ''),
                       COALESCE(SUM(COALESCE(r.real_bill_amount, 0)), 0)
                FROM warehouse_inbound_receipts r
                JOIN payment_requests pr ON pr.id = r.payment_request_id
                JOIN vendors v ON v.id = pr.vendor_id
                WHERE r.is_deleted = false
                  AND pr.is_deleted = false
                  AND r.status NOT IN ('DRAFT', 'REJECTED', 'CANCELLED')
                  AND (:vendor = '' OR LOWER(COALESCE(v.name, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.code, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.misa_code, '')) LIKE :vendorLike)
                  %s
                GROUP BY v.id, v.misa_code, v.code, v.name, v.currency, r.currency
                """.formatted(dateClause);
        Query query = entityManager.createNativeQuery(sql);
        bindContains(query, "vendor", request.getVendor());
        if (bounded) {
            query.setParameter("startDate", start);
            if (!beforeStart) {
                query.setParameter("endDate", end);
            }
        }
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> vendorPaidSums(LocalDate start, LocalDate end, boolean beforeStart, OperationalReportRequest request) {
        boolean bounded = start != null;
        String dateClause = !bounded
                ? ""
                : beforeStart
                ? "AND COALESCE(pr.paid_at, pr.updated_at) < :start"
                : "AND COALESCE(pr.paid_at, pr.updated_at) >= :start AND COALESCE(pr.paid_at, pr.updated_at) < :end";
        String sql = """
                SELECT CAST(v.id AS text),
                       COALESCE(v.misa_code, v.code, ''),
                       COALESCE(v.name, ''),
                       COALESCE(v.currency, pr.currency, ''),
                       COALESCE(SUM(COALESCE(pr.paid_amount, 0)), 0),
                       COALESCE(SUM(COALESCE(pr.total_amount, 0) + COALESCE(pr.fee_amount, 0)), 0)
                FROM payment_requests pr
                JOIN vendors v ON v.id = pr.vendor_id
                WHERE pr.is_deleted = false
                  AND pr.status NOT IN ('REJECTED', 'CANCELLED', 'DRAFT')
                  AND (:vendor = '' OR LOWER(COALESCE(v.name, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.code, '')) LIKE :vendorLike
                       OR LOWER(COALESCE(v.misa_code, '')) LIKE :vendorLike)
                  %s
                GROUP BY v.id, v.misa_code, v.code, v.name, v.currency, pr.currency
                """.formatted(dateClause);
        Query query = entityManager.createNativeQuery(sql);
        bindContains(query, "vendor", request.getVendor());
        if (bounded) {
            query.setParameter("start", Timestamp.valueOf(start.atStartOfDay()));
            if (!beforeStart) {
                query.setParameter("end", Timestamp.valueOf(end.atStartOfDay()));
            }
        }
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> salesDetailRows(LocalDate start, LocalDate end, OperationalReportRequest request) {
        boolean bounded = start != null && end != null;
        String dateClause = bounded
                ? "AND pr.request_date >= :start AND pr.request_date < :end"
                : "";
        String sql = """
                SELECT COALESCE(u.full_name, ol.created_by, ''),
                       COALESCE(cu.misa_code, cu.code, ''),
                       COALESCE(ord.contract_number, ''),
                       ord.order_date,
                       COALESCE(p.code, ''),
                       COALESCE(p.name, ''),
                       COALESCE(ol.uom, pol.uom_1, ''),
                       COALESCE(ol.quantity, 0),
                       COALESCE(ol.unit_price, 0),
                       COALESCE(ol.total_amount, 0),
                       COALESCE(ol.tax_rate, 0),
                       COALESCE(ol.vendor_code_suggest, ''),
                       COALESCE(ol.reference_note, ''),
                       COALESCE(ol.notes, ''),
                       COALESCE(v.misa_code, v.code, ''),
                       COALESCE(pr.notes, ''),
                       COALESCE(CAST(pr.papers AS text), ''),
                       COALESCE(pol.quantity, 0),
                       COALESCE(v.currency, pol.currency, ''),
                       COALESCE(pol.unit_price, 0),
                       COALESCE(pol.total_price, 0),
                       COALESCE(pol.quote, ''),
                       COALESCE(pol.invoice, ''),
                       COALESCE(pol.bill_of_ladding, ''),
                       COALESCE(pol.receipt_warehouse, ''),
                       COALESCE(pol.track_id, ''),
                       inbound.received_date,
                       COALESCE(inbound.receipt_number, ''),
                       COALESCE(inbound.quantity_received, 0),
                       COALESCE(inbound.inbound_price, 0),
                       COALESCE(inbound.inbound_amount, 0),
                       COALESCE(inbound.bill_on_paper, ''),
                       COALESCE(inbound.line_note, '')
                FROM payment_request_purchase_order_lines link
                JOIN payment_requests pr ON pr.id = link.payment_request_id
                JOIN purchase_order_lines pol ON pol.id = link.purchase_order_line_id
                LEFT JOIN products p ON p.id = pol.product_id
                LEFT JOIN vendors v ON v.id = pol.vendor_id
                LEFT JOIN order_lines ol ON ol.id = pol.sale_orderline_id
                LEFT JOIN orders ord ON ord.id = ol.order_id
                LEFT JOIN customers cu ON cu.id = ord.customer_id
                LEFT JOIN users u ON u.username = ol.created_by OR CAST(u.id AS text) = ol.created_by
                LEFT JOIN LATERAL (
                    SELECT r.received_date,
                           r.receipt_number,
                           l.quantity_received,
                           COALESCE(pol2.unit_price, 0) AS inbound_price,
                           l.quantity_received * COALESCE(pol2.unit_price, 0) AS inbound_amount,
                           l.bill_on_paper,
                           l.line_note
                    FROM warehouse_inbound_receipt_lines l
                    JOIN warehouse_inbound_receipts r ON r.id = l.receipt_id
                    LEFT JOIN purchase_order_lines pol2 ON pol2.id = l.purchase_order_line_id
                    WHERE l.is_deleted = false
                      AND r.is_deleted = false
                      AND (l.purchase_order_line_id = pol.id OR l.payment_request_purchase_order_line_id = link.id)
                    ORDER BY r.received_date NULLS LAST, l.created_at
                    LIMIT 1
                ) inbound ON true
                WHERE link.is_deleted = false
                  AND pr.is_deleted = false
                  AND pr.status IN (%s)
                  AND (:userName = '' OR LOWER(COALESCE(u.full_name, ol.created_by, '')) LIKE :userNameLike)
                  AND (:contractNumber = '' OR LOWER(COALESCE(ord.contract_number, '')) LIKE :contractNumberLike)
                  AND (:productName = '' OR LOWER(COALESCE(p.name, '')) LIKE :productNameLike)
                  AND (:customer = '' OR LOWER(COALESCE(cu.name, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.code, '')) LIKE :customerLike
                       OR LOWER(COALESCE(cu.misa_code, '')) LIKE :customerLike)
                  %s
                ORDER BY pr.created_at DESC, p.code
                """.formatted(SALES_STATUSES, dateClause);
        Query query = entityManager.createNativeQuery(sql);
        bindContains(query, "userName", request.getUserName());
        bindContains(query, "contractNumber", request.getContractNumber());
        bindContains(query, "productName", request.getProductName());
        bindContains(query, "customer", request.getCustomer());
        if (bounded) {
            bindRange(query, start, end);
        }
        return query.getResultList();
    }

    private Query bindRange(Query query, LocalDate start, LocalDate end) {
        query.setParameter("start", Timestamp.valueOf(start.atStartOfDay()));
        query.setParameter("end", Timestamp.valueOf(end.atStartOfDay()));
        return query;
    }

    private void bindContains(Query query, String name, String value) {
        String text = value == null ? "" : value.trim();
        query.setParameter(name, text);
        query.setParameter(name + "Like", "%" + text.toLowerCase(java.util.Locale.ROOT) + "%");
    }

    private Query bindDates(Query query, LocalDate start, LocalDate end) {
        query.setParameter("startDate", start);
        query.setParameter("endDate", end);
        return query;
    }

    public static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    public static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    public static LocalDate date(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    public static Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
