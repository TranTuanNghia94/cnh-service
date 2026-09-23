package com.cnh.ies.repository.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.cnh.ies.repository.report.projection.OrderDetailProjection;
import com.cnh.ies.repository.report.projection.PaymentRequestDetailProjection;
import com.cnh.ies.repository.report.projection.StatusCountProjection;
import com.cnh.ies.repository.report.projection.WarehouseInboundDetailProjection;
import com.cnh.ies.repository.report.projection.WarehouseInventoryDetailProjection;
import com.cnh.ies.repository.report.projection.WarehouseOutboundDetailProjection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
@Repository
public class ServiceReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // --- Warehouse Inbound ---

    public Long countInboundReceipts(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        Query q = entityManager.createQuery(inboundReceiptFilterJpql("SELECT COUNT(DISTINCT r.id)"));
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<StatusCountProjection> inboundStatusBreakdown(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        String jpql = inboundReceiptFilterJpql(
                "SELECT r.status AS status, COUNT(DISTINCT r.id) AS count ")
                + "GROUP BY r.status ORDER BY r.status";
        Query q = entityManager.createQuery(jpql);
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        return q.getResultList().stream()
                .map(row -> {
                    Object[] arr = (Object[]) row;
                    return statusCount((String) arr[0], (Long) arr[1]);
                })
                .toList();
    }

    public BigDecimal[] inboundLineQuantities(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        String jpql = "SELECT COALESCE(SUM(l.quantityExpected), 0), COALESCE(SUM(l.quantityReceived), 0) "
                + "FROM WarehouseInboundReceiptLineEntity l "
                + "JOIN l.receipt r "
                + "LEFT JOIN r.paymentRequest pr "
                + "LEFT JOIN PaymentRequestPurchaseOrderLineEntity prpol ON prpol.paymentRequest.id = pr.id "
                + "LEFT JOIN prpol.purchaseOrderLine pol "
                + "LEFT JOIN pol.purchaseOrder po "
                + "LEFT JOIN po.order o "
                + "LEFT JOIN o.customer c "
                + "WHERE r.isDeleted = false AND l.isDeleted = false "
                + "AND r.receivedDate >= :fromDate AND r.receivedDate <= :toDate "
                + "AND (:createdBy = '' OR LOWER(COALESCE(r.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:inboundNumber = '' OR LOWER(COALESCE(r.receiptNumber, '')) LIKE LOWER(CONCAT('%', :inboundNumber, '%'))) "
                + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
                + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                + "AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(r.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))";
        Query q = entityManager.createQuery(jpql);
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] { (BigDecimal) row[0], (BigDecimal) row[1] };
    }

    public BigDecimal[] inboundHeaderMoneyTotals(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        String jpql = inboundReceiptFilterJpql(
                "SELECT COALESCE(SUM(r.feeAmount), 0), COALESCE(SUM(r.realBillAmount), 0), "
                        + "COALESCE(SUM(r.billOnPaperAmount), 0) ");
        Query q = entityManager.createQuery(jpql);
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] { (BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2] };
    }

    @SuppressWarnings("unchecked")
    public List<WarehouseInboundDetailProjection> findInboundDetail(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status,
            Pageable pageable) {
        String select = """
                SELECT r.id, r.receiptNumber, r.receivedDate, r.status,
                       pr.requestNumber, v.code, v.name,
                       COALESCE(polProd.code, prPolProd.code), COALESCE(polProd.name, prPolProd.name),
                       l.quantityExpected, l.quantityReceived,
                       r.feeAmount, r.realBillAmount, r.billOnPaperAmount, r.createdBy
                FROM WarehouseInboundReceiptLineEntity l
                JOIN l.receipt r
                LEFT JOIN r.paymentRequest pr
                LEFT JOIN pr.vendor v
                LEFT JOIN l.purchaseOrderLine pol
                LEFT JOIN pol.product polProd
                LEFT JOIN l.paymentRequestPurchaseOrderLine prpol
                LEFT JOIN prpol.purchaseOrderLine prPol
                LEFT JOIN prPol.product prPolProd
                LEFT JOIN pol.purchaseOrder po
                LEFT JOIN po.order o
                LEFT JOIN o.customer c
                WHERE r.isDeleted = false AND l.isDeleted = false
                AND r.receivedDate >= :fromDate AND r.receivedDate <= :toDate
                AND (:createdBy = '' OR LOWER(COALESCE(r.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:inboundNumber = '' OR LOWER(COALESCE(r.receiptNumber, '')) LIKE LOWER(CONCAT('%', :inboundNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(r.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                ORDER BY r.receivedDate DESC, r.receiptNumber DESC
                """;
        Query q = entityManager.createQuery(select);
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return mapInboundDetail(q.getResultList());
    }

    public Long countInboundDetail(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        String jpql = """
                SELECT COUNT(l.id)
                FROM WarehouseInboundReceiptLineEntity l
                JOIN l.receipt r
                LEFT JOIN r.paymentRequest pr
                LEFT JOIN l.paymentRequestPurchaseOrderLine prpol
                LEFT JOIN prpol.purchaseOrderLine pol
                LEFT JOIN pol.purchaseOrder po
                LEFT JOIN po.order o
                LEFT JOIN o.customer c
                WHERE r.isDeleted = false AND l.isDeleted = false
                AND r.receivedDate >= :fromDate AND r.receivedDate <= :toDate
                AND (:createdBy = '' OR LOWER(COALESCE(r.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:inboundNumber = '' OR LOWER(COALESCE(r.receiptNumber, '')) LIKE LOWER(CONCAT('%', :inboundNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(r.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindInboundReceiptFilters(q, fromDate, toDate, createdBy, inboundNumber, contractNumber,
                customerName, orderNumber, status);
        return (Long) q.getSingleResult();
    }

    // --- Warehouse Outbound ---

    public Long countOutbound(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        Query q = entityManager.createQuery(outboundFilterJpql("SELECT COUNT(DISTINCT o.id)"));
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<StatusCountProjection> outboundStatusBreakdown(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        String jpql = outboundFilterJpql("SELECT o.status, COUNT(DISTINCT o.id) ")
                + "GROUP BY o.status ORDER BY o.status";
        Query q = entityManager.createQuery(jpql);
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        return q.getResultList().stream()
                .map(row -> {
                    Object[] arr = (Object[]) row;
                    return statusCount((String) arr[0], (Long) arr[1]);
                })
                .toList();
    }

    public BigDecimal[] outboundHeaderTotals(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        String jpql = outboundFilterJpql(
                "SELECT COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.taxAmount), 0) ");
        Query q = entityManager.createQuery(jpql);
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] { (BigDecimal) row[0], (BigDecimal) row[1] };
    }

    public BigDecimal outboundDetailQuantityTotal(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        String jpql = """
                SELECT COALESCE(SUM(d.quantity), 0)
                FROM WarehouseOutboundDetailEntity d
                JOIN d.outbound o
                WHERE o.isDeleted = false AND d.isDeleted = false
                AND o.outboundDate >= :fromDate AND o.outboundDate <= :toDate
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:outboundNumber = '' OR LOWER(COALESCE(o.outboundNumber, '')) LIKE LOWER(CONCAT('%', :outboundNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        return (BigDecimal) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<WarehouseOutboundDetailProjection> findOutboundDetail(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status,
            Pageable pageable) {
        String jpql = """
                SELECT o.id, o.outboundNumber, o.outboundDate, o.status, o.contractNumber,
                       CONCAT(COALESCE(ord.orderPrefix, ''), '.', CONCAT('', ord.orderNumber)),
                       p.code, p.name, d.quantity, d.unitPrice, d.totalAmount, d.taxAmount, o.createdBy
                FROM WarehouseOutboundDetailEntity d
                JOIN d.outbound o
                JOIN d.product p
                LEFT JOIN o.order ord
                WHERE o.isDeleted = false AND d.isDeleted = false
                AND o.outboundDate >= :fromDate AND o.outboundDate <= :toDate
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:outboundNumber = '' OR LOWER(COALESCE(o.outboundNumber, '')) LIKE LOWER(CONCAT('%', :outboundNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                ORDER BY o.outboundDate DESC, o.outboundNumber DESC
                """;
        Query q = entityManager.createQuery(jpql);
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return mapOutboundDetail(q.getResultList());
    }

    public Long countOutboundDetail(
            LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        String jpql = """
                SELECT COUNT(d.id)
                FROM WarehouseOutboundDetailEntity d
                JOIN d.outbound o
                WHERE o.isDeleted = false AND d.isDeleted = false
                AND o.outboundDate >= :fromDate AND o.outboundDate <= :toDate
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:outboundNumber = '' OR LOWER(COALESCE(o.outboundNumber, '')) LIKE LOWER(CONCAT('%', :outboundNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindOutboundFilters(q, fromDate, toDate, createdBy, outboundNumber, contractNumber, status);
        return (Long) q.getSingleResult();
    }

    // --- Warehouse Inventory ---

    public Long countInventoryProducts(String productCode, String productName, String productCategory) {
        String jpql = """
                SELECT COUNT(DISTINCT i.product.id)
                FROM WarehouseInventoryEntity i
                JOIN i.product p
                LEFT JOIN p.category c
                WHERE i.isDeleted = false AND p.isDeleted = false
                AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%')))
                AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
                AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        q.setParameter("productCode", nz(productCode));
        q.setParameter("productName", nz(productName));
        q.setParameter("productCategory", nz(productCategory));
        return (Long) q.getSingleResult();
    }

    public BigDecimal sumQuantityOnHand(String productCode, String productName, String productCategory) {
        String jpql = """
                SELECT COALESCE(SUM(i.quantityOnHand), 0)
                FROM WarehouseInventoryEntity i
                JOIN i.product p
                LEFT JOIN p.category c
                WHERE i.isDeleted = false AND p.isDeleted = false
                AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%')))
                AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
                AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        q.setParameter("productCode", nz(productCode));
        q.setParameter("productName", nz(productName));
        q.setParameter("productCategory", nz(productCategory));
        return (BigDecimal) q.getSingleResult();
    }

    public StockMovementTotals stockMovementTotals(
            Instant fromInstant, Instant toExclusive,
            String productCode, String productName, String productCategory, String direction) {
        String jpql = """
                SELECT COALESCE(SUM(CASE WHEN t.direction = 'INBOUND' THEN t.quantity ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN t.direction = 'OUTBOUND' THEN t.quantity ELSE 0 END), 0),
                       COUNT(t.id)
                FROM WarehouseStockTransactionEntity t
                JOIN t.product p
                LEFT JOIN p.category c
                WHERE t.isDeleted = false AND p.isDeleted = false
                AND t.createdAt >= :fromInstant AND t.createdAt < :toExclusive
                AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%')))
                AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
                AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))
                AND (:direction = '' OR LOWER(t.direction) = LOWER(:direction))
                """;
        Query q = entityManager.createQuery(jpql);
        q.setParameter("fromInstant", fromInstant);
        q.setParameter("toExclusive", toExclusive);
        q.setParameter("productCode", nz(productCode));
        q.setParameter("productName", nz(productName));
        q.setParameter("productCategory", nz(productCategory));
        q.setParameter("direction", nz(direction));
        Object[] row = (Object[]) q.getSingleResult();
        return new StockMovementTotals((BigDecimal) row[0], (BigDecimal) row[1], (Long) row[2]);
    }

    public record StockMovementTotals(BigDecimal inboundQty, BigDecimal outboundQty, Long movementCount) {}

    @SuppressWarnings("unchecked")
    public List<WarehouseInventoryDetailProjection> findInventoryDetail(
            Instant fromInstant, Instant toExclusive,
            String productCode, String productName, String productCategory, String direction,
            Pageable pageable) {
        String jpql = """
                SELECT t.id, p.code, p.name, t.direction, t.quantity, t.referenceType, t.referenceId,
                       t.createdAt, t.note, t.createdBy
                FROM WarehouseStockTransactionEntity t
                JOIN t.product p
                LEFT JOIN p.category c
                WHERE t.isDeleted = false AND p.isDeleted = false
                AND t.createdAt >= :fromInstant AND t.createdAt < :toExclusive
                AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%')))
                AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
                AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))
                AND (:direction = '' OR LOWER(t.direction) = LOWER(:direction))
                ORDER BY t.createdAt DESC
                """;
        Query q = entityManager.createQuery(jpql);
        q.setParameter("fromInstant", fromInstant);
        q.setParameter("toExclusive", toExclusive);
        q.setParameter("productCode", nz(productCode));
        q.setParameter("productName", nz(productName));
        q.setParameter("productCategory", nz(productCategory));
        q.setParameter("direction", nz(direction));
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return mapInventoryDetail(q.getResultList());
    }

    public Long countInventoryDetail(
            Instant fromInstant, Instant toExclusive,
            String productCode, String productName, String productCategory, String direction) {
        String jpql = """
                SELECT COUNT(t.id)
                FROM WarehouseStockTransactionEntity t
                JOIN t.product p
                LEFT JOIN p.category c
                WHERE t.isDeleted = false AND p.isDeleted = false
                AND t.createdAt >= :fromInstant AND t.createdAt < :toExclusive
                AND (:productCode = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :productCode, '%')))
                AND (:productName = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
                AND (:productCategory = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :productCategory, '%')))
                AND (:direction = '' OR LOWER(t.direction) = LOWER(:direction))
                """;
        Query q = entityManager.createQuery(jpql);
        q.setParameter("fromInstant", fromInstant);
        q.setParameter("toExclusive", toExclusive);
        q.setParameter("productCode", nz(productCode));
        q.setParameter("productName", nz(productName));
        q.setParameter("productCategory", nz(productCategory));
        q.setParameter("direction", nz(direction));
        return (Long) q.getSingleResult();
    }

    // --- Order ---

    public Long countOrders(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        Query q = entityManager.createQuery(orderFilterJpql("SELECT COUNT(DISTINCT o.id)"));
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<StatusCountProjection> orderStatusBreakdown(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        String jpql = orderFilterJpql("SELECT o.status, COUNT(DISTINCT o.id) ")
                + "GROUP BY o.status ORDER BY o.status";
        Query q = entityManager.createQuery(jpql);
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        return q.getResultList().stream()
                .map(row -> {
                    Object[] arr = (Object[]) row;
                    return statusCount((String) arr[0], (Long) arr[1]);
                })
                .toList();
    }

    public BigDecimal[] orderHeaderTotals(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        String jpql = orderFilterJpql(
                "SELECT COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.discountAmount), 0), "
                        + "COALESCE(SUM(o.taxAmount), 0), COALESCE(SUM(o.finalAmount), 0) ");
        Query q = entityManager.createQuery(jpql);
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] {
                (BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3]
        };
    }

    public BigDecimal orderLineQuantityTotal(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        String jpql = """
                SELECT COALESCE(SUM(ol.quantity), 0)
                FROM OrderLineEntity ol
                JOIN ol.order o
                LEFT JOIN o.customer c
                WHERE o.isDeleted = false AND ol.isDeleted = false
                AND (
                    (o.orderDate >= :fromDate AND o.orderDate <= :toDate)
                    OR (o.createdAt >= :fromInstant AND o.createdAt < :toExclusive)
                )
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        return (BigDecimal) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<OrderDetailProjection> findOrderDetail(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName,
            Pageable pageable) {
        String jpql = """
                SELECT o.id, CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber)),
                       o.orderDate, o.status, c.name, o.contractNumber,
                       COALESCE(p.code, ol.productCodeSuggest), COALESCE(p.name, ol.productNameSuggest),
                       COALESCE(v.code, ol.vendorCodeSuggest), ol.quantity, ol.unitPrice,
                       ol.discountAmount, ol.taxAmount, ol.totalAmount, o.createdBy
                FROM OrderEntity o
                LEFT JOIN o.customer c
                LEFT JOIN o.orderLines ol ON ol.isDeleted = false
                LEFT JOIN ol.product p
                LEFT JOIN ol.vendor v
                WHERE o.isDeleted = false
                AND (ol IS NULL OR ol.isDeleted = false)
                AND (
                    (o.orderDate >= :fromDate AND o.orderDate <= :toDate)
                    OR (o.createdAt >= :fromInstant AND o.createdAt < :toExclusive)
                )
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                ORDER BY o.orderDate DESC, o.orderNumber DESC, ol.id ASC
                """;
        Query q = entityManager.createQuery(jpql);
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return mapOrderDetail(q.getResultList());
    }

    public Long countOrderDetail(
            LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        String jpql = """
                SELECT COUNT(*)
                FROM OrderEntity o
                LEFT JOIN o.customer c
                LEFT JOIN o.orderLines ol ON ol.isDeleted = false
                WHERE o.isDeleted = false
                AND (ol IS NULL OR ol.isDeleted = false)
                AND (
                    (o.orderDate >= :fromDate AND o.orderDate <= :toDate)
                    OR (o.createdAt >= :fromInstant AND o.createdAt < :toExclusive)
                )
                AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%')))
                AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindOrderFilters(q, fromDate, toDate, fromInstant, toExclusive,
                createdBy, contractNumber, orderNumber, status, customerName);
        return (Long) q.getSingleResult();
    }

    // --- Purchase ---

    public Long countPurchaseOrders(
            LocalDate fromDate, LocalDate toDate,
            String purchaseOrderNumber, String contractNumber,
            String createdBy, String customerName, String status) {
        Query q = entityManager.createQuery(purchaseFilterJpql("SELECT COUNT(DISTINCT po.id)"));
        bindPurchaseFilters(q, fromDate, toDate, purchaseOrderNumber, contractNumber,
                createdBy, customerName, status);
        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<StatusCountProjection> purchaseStatusBreakdown(
            LocalDate fromDate, LocalDate toDate,
            String purchaseOrderNumber, String contractNumber,
            String createdBy, String customerName, String status) {
        String jpql = purchaseFilterJpql("SELECT po.status, COUNT(DISTINCT po.id) ")
                + "GROUP BY po.status ORDER BY po.status";
        Query q = entityManager.createQuery(jpql);
        bindPurchaseFilters(q, fromDate, toDate, purchaseOrderNumber, contractNumber,
                createdBy, customerName, status);
        return q.getResultList().stream()
                .map(row -> {
                    Object[] arr = (Object[]) row;
                    return statusCount((String) arr[0], (Long) arr[1]);
                })
                .toList();
    }

    public BigDecimal[] purchaseLineTotals(
            LocalDate fromDate, LocalDate toDate,
            String purchaseOrderNumber, String contractNumber,
            String createdBy, String customerName, String status) {
        String jpql = """
                SELECT COALESCE(SUM(pol.quantity), 0),
                       COALESCE(SUM(pol.totalBeforeTax), 0),
                       COALESCE(SUM(pol.totalPrice), 0),
                       COALESCE(SUM(pol.totalPriceVnd), 0),
                       COUNT(DISTINCT pol.vendor.id),
                       COUNT(DISTINCT pol.product.id)
                FROM PurchaseOrderLineEntity pol
                JOIN pol.purchaseOrder po
                LEFT JOIN po.order o
                LEFT JOIN o.customer c
                WHERE po.isDeleted = false AND pol.isDeleted = false
                AND po.orderDate >= :fromDate AND po.orderDate <= :toDate
                AND (:purchaseOrderNumber = '' OR LOWER(CONCAT(COALESCE(po.poPrefix, ''), '.', CONCAT('', po.poNumber))) LIKE LOWER(CONCAT('%', :purchaseOrderNumber, '%')))
                AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%')))
                AND (:createdBy = '' OR LOWER(COALESCE(po.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%')))
                AND (:status = '' OR LOWER(COALESCE(po.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindPurchaseFilters(q, fromDate, toDate, purchaseOrderNumber, contractNumber,
                createdBy, customerName, status);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] {
                (BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3],
                BigDecimal.valueOf((Long) row[4]), BigDecimal.valueOf((Long) row[5])
        };
    }

    // --- Payment Request ---

    public Long countPaymentRequests(
            Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status) {
        String jpql = paymentFilterJpql("SELECT COUNT(DISTINCT pr.id)");
        Query q = entityManager.createQuery(jpql);
        bindPaymentFilters(q, fromInstant, toExclusive, createdBy, paymentRequestNumber, vendorCode, status);
        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<StatusCountProjection> paymentStatusBreakdown(
            Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status) {
        String jpql = paymentFilterJpql("SELECT pr.status, COUNT(DISTINCT pr.id) ")
                + "GROUP BY pr.status ORDER BY pr.status";
        Query q = entityManager.createQuery(jpql);
        bindPaymentFilters(q, fromInstant, toExclusive, createdBy, paymentRequestNumber, vendorCode, status);
        return q.getResultList().stream()
                .map(row -> {
                    Object[] arr = (Object[]) row;
                    return statusCount((String) arr[0], (Long) arr[1]);
                })
                .toList();
    }

    public BigDecimal[] paymentHeaderTotals(
            Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status) {
        String jpql = paymentFilterJpql(
                "SELECT COALESCE(SUM(pr.requestedAmount), 0), COALESCE(SUM(pr.requestedAmountVnd), 0), "
                        + "COALESCE(SUM(pr.feeAmount), 0), COALESCE(SUM(pr.feeAmountVnd), 0), "
                        + "COALESCE(SUM(pr.totalAmount), 0), COALESCE(SUM(pr.totalAmountVnd), 0), "
                        + "COALESCE(SUM(pr.paidAmount), 0), COALESCE(SUM(pr.paidAmountVnd), 0) ");
        Query q = entityManager.createQuery(jpql);
        bindPaymentFilters(q, fromInstant, toExclusive, createdBy, paymentRequestNumber, vendorCode, status);
        Object[] row = (Object[]) q.getSingleResult();
        return new BigDecimal[] {
                (BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3],
                (BigDecimal) row[4], (BigDecimal) row[5], (BigDecimal) row[6], (BigDecimal) row[7]
        };
    }

    @SuppressWarnings("unchecked")
    public List<PaymentRequestDetailProjection> findPaymentDetail(
            Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status,
            Pageable pageable) {
        String jpql = """
                SELECT pr.id, pr.requestNumber, pr.requestDate, pr.status, v.code, v.name,
                       p.code,
                       CONCAT(COALESCE(po.poPrefix, ''), '.', CONCAT('', po.poNumber)),
                       prpol.requestedAmount, prpol.paidAmount, pr.feeAmount, pr.totalAmount,
                       pr.currentApprovalLevel, pr.approvalLevels, pr.createdBy
                FROM PaymentRequestPurchaseOrderLineEntity prpol
                JOIN prpol.paymentRequest pr
                LEFT JOIN pr.vendor v
                JOIN prpol.purchaseOrderLine pol
                LEFT JOIN pol.product p
                LEFT JOIN pol.purchaseOrder po
                WHERE pr.isDeleted = false AND prpol.isDeleted = false
                AND ((pr.requestDate >= :fromInstant AND pr.requestDate < :toExclusive)
                     OR (pr.createdAt >= :fromInstant AND pr.createdAt < :toExclusive))
                AND (:createdBy = '' OR LOWER(COALESCE(pr.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:paymentRequestNumber = '' OR LOWER(COALESCE(pr.requestNumber, '')) LIKE LOWER(CONCAT('%', :paymentRequestNumber, '%')))
                AND (:vendorCode = '' OR LOWER(COALESCE(v.code, '')) LIKE LOWER(CONCAT('%', :vendorCode, '%')))
                AND (:status = '' OR LOWER(COALESCE(pr.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                ORDER BY pr.requestDate DESC, pr.requestNumber DESC
                """;
        Query q = entityManager.createQuery(jpql);
        bindPaymentFilters(q, fromInstant, toExclusive, createdBy, paymentRequestNumber, vendorCode, status);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return mapPaymentDetail(q.getResultList());
    }

    public Long countPaymentDetail(
            Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status) {
        String jpql = """
                SELECT COUNT(prpol.id)
                FROM PaymentRequestPurchaseOrderLineEntity prpol
                JOIN prpol.paymentRequest pr
                LEFT JOIN pr.vendor v
                WHERE pr.isDeleted = false AND prpol.isDeleted = false
                AND ((pr.requestDate >= :fromInstant AND pr.requestDate < :toExclusive)
                     OR (pr.createdAt >= :fromInstant AND pr.createdAt < :toExclusive))
                AND (:createdBy = '' OR LOWER(COALESCE(pr.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%')))
                AND (:paymentRequestNumber = '' OR LOWER(COALESCE(pr.requestNumber, '')) LIKE LOWER(CONCAT('%', :paymentRequestNumber, '%')))
                AND (:vendorCode = '' OR LOWER(COALESCE(v.code, '')) LIKE LOWER(CONCAT('%', :vendorCode, '%')))
                AND (:status = '' OR LOWER(COALESCE(pr.status, '')) LIKE LOWER(CONCAT('%', :status, '%')))
                """;
        Query q = entityManager.createQuery(jpql);
        bindPaymentFilters(q, fromInstant, toExclusive, createdBy, paymentRequestNumber, vendorCode, status);
        return (Long) q.getSingleResult();
    }

    // --- helpers ---

    private String inboundReceiptFilterJpql(String selectPrefix) {
        return selectPrefix
                + " FROM WarehouseInboundReceiptEntity r "
                + "LEFT JOIN r.paymentRequest pr "
                + "LEFT JOIN PaymentRequestPurchaseOrderLineEntity prpol ON prpol.paymentRequest.id = pr.id "
                + "LEFT JOIN prpol.purchaseOrderLine pol "
                + "LEFT JOIN pol.purchaseOrder po "
                + "LEFT JOIN po.order o "
                + "LEFT JOIN o.customer c "
                + "WHERE r.isDeleted = false "
                + "AND r.receivedDate >= :fromDate AND r.receivedDate <= :toDate "
                + "AND (:createdBy = '' OR LOWER(COALESCE(r.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:inboundNumber = '' OR LOWER(COALESCE(r.receiptNumber, '')) LIKE LOWER(CONCAT('%', :inboundNumber, '%'))) "
                + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
                + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                + "AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(r.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) ";
    }

    private void bindInboundReceiptFilters(
            Query q, LocalDate fromDate, LocalDate toDate,
            String createdBy, String inboundNumber, String contractNumber,
            String customerName, String orderNumber, String status) {
        q.setParameter("fromDate", fromDate);
        q.setParameter("toDate", toDate);
        q.setParameter("createdBy", nz(createdBy));
        q.setParameter("inboundNumber", nz(inboundNumber));
        q.setParameter("contractNumber", nz(contractNumber));
        q.setParameter("customerName", nz(customerName));
        q.setParameter("orderNumber", nz(orderNumber));
        q.setParameter("status", nz(status));
    }

    private String outboundFilterJpql(String selectPrefix) {
        return selectPrefix
                + " FROM WarehouseOutboundEntity o "
                + "WHERE o.isDeleted = false "
                + "AND o.outboundDate >= :fromDate AND o.outboundDate <= :toDate "
                + "AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:outboundNumber = '' OR LOWER(COALESCE(o.outboundNumber, '')) LIKE LOWER(CONCAT('%', :outboundNumber, '%'))) "
                + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) ";
    }

    private void bindOutboundFilters(
            Query q, LocalDate fromDate, LocalDate toDate,
            String createdBy, String outboundNumber, String contractNumber, String status) {
        q.setParameter("fromDate", fromDate);
        q.setParameter("toDate", toDate);
        q.setParameter("createdBy", nz(createdBy));
        q.setParameter("outboundNumber", nz(outboundNumber));
        q.setParameter("contractNumber", nz(contractNumber));
        q.setParameter("status", nz(status));
    }

    private String orderFilterJpql(String selectPrefix) {
        return selectPrefix
                + " FROM OrderEntity o LEFT JOIN o.customer c "
                + "WHERE o.isDeleted = false "
                + "AND ((o.orderDate >= :fromDate AND o.orderDate <= :toDate) "
                + "OR (o.createdAt >= :fromInstant AND o.createdAt < :toExclusive)) "
                + "AND (:createdBy = '' OR LOWER(COALESCE(o.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
                + "AND (:orderNumber = '' OR LOWER(CONCAT(COALESCE(o.orderPrefix, ''), '.', CONCAT('', o.orderNumber))) LIKE LOWER(CONCAT('%', :orderNumber, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(o.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) "
                + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%'))) ";
    }

    private void bindOrderFilters(
            Query q, LocalDate fromDate, LocalDate toDate,
            Instant fromInstant, Instant toExclusive,
            String createdBy, String contractNumber, String orderNumber,
            String status, String customerName) {
        q.setParameter("fromDate", fromDate);
        q.setParameter("toDate", toDate);
        q.setParameter("fromInstant", fromInstant);
        q.setParameter("toExclusive", toExclusive);
        q.setParameter("createdBy", nz(createdBy));
        q.setParameter("contractNumber", nz(contractNumber));
        q.setParameter("orderNumber", nz(orderNumber));
        q.setParameter("status", nz(status));
        q.setParameter("customerName", nz(customerName));
    }

    private String purchaseFilterJpql(String selectPrefix) {
        return selectPrefix
                + " FROM PurchaseOrderEntity po "
                + "LEFT JOIN po.order o LEFT JOIN o.customer c "
                + "WHERE po.isDeleted = false "
                + "AND po.orderDate >= :fromDate AND po.orderDate <= :toDate "
                + "AND (:purchaseOrderNumber = '' OR LOWER(CONCAT(COALESCE(po.poPrefix, ''), '.', CONCAT('', po.poNumber))) LIKE LOWER(CONCAT('%', :purchaseOrderNumber, '%'))) "
                + "AND (:contractNumber = '' OR LOWER(COALESCE(o.contractNumber, '')) LIKE LOWER(CONCAT('%', :contractNumber, '%'))) "
                + "AND (:createdBy = '' OR LOWER(COALESCE(po.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:customerName = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :customerName, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(po.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) ";
    }

    private void bindPurchaseFilters(
            Query q, LocalDate fromDate, LocalDate toDate,
            String purchaseOrderNumber, String contractNumber,
            String createdBy, String customerName, String status) {
        q.setParameter("fromDate", fromDate);
        q.setParameter("toDate", toDate);
        q.setParameter("purchaseOrderNumber", nz(purchaseOrderNumber));
        q.setParameter("contractNumber", nz(contractNumber));
        q.setParameter("createdBy", nz(createdBy));
        q.setParameter("customerName", nz(customerName));
        q.setParameter("status", nz(status));
    }

    private String paymentFilterJpql(String selectPrefix) {
        return selectPrefix
                + " FROM PaymentRequestEntity pr LEFT JOIN pr.vendor v "
                + "WHERE pr.isDeleted = false "
                + "AND ((pr.requestDate >= :fromInstant AND pr.requestDate < :toExclusive) "
                + "OR (pr.createdAt >= :fromInstant AND pr.createdAt < :toExclusive)) "
                + "AND (:createdBy = '' OR LOWER(COALESCE(pr.createdBy, '')) LIKE LOWER(CONCAT('%', :createdBy, '%'))) "
                + "AND (:paymentRequestNumber = '' OR LOWER(COALESCE(pr.requestNumber, '')) LIKE LOWER(CONCAT('%', :paymentRequestNumber, '%'))) "
                + "AND (:vendorCode = '' OR LOWER(COALESCE(v.code, '')) LIKE LOWER(CONCAT('%', :vendorCode, '%'))) "
                + "AND (:status = '' OR LOWER(COALESCE(pr.status, '')) LIKE LOWER(CONCAT('%', :status, '%'))) ";
    }

    private void bindPaymentFilters(
            Query q, Instant fromInstant, Instant toExclusive,
            String createdBy, String paymentRequestNumber, String vendorCode, String status) {
        q.setParameter("fromInstant", fromInstant);
        q.setParameter("toExclusive", toExclusive);
        q.setParameter("createdBy", nz(createdBy));
        q.setParameter("paymentRequestNumber", nz(paymentRequestNumber));
        q.setParameter("vendorCode", nz(vendorCode));
        q.setParameter("status", nz(status));
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }

    private static StatusCountProjection statusCount(String status, Long count) {
        return new StatusCountProjection() {
            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private List<WarehouseInboundDetailProjection> mapInboundDetail(List<?> rows) {
        return rows.stream().map(r -> {
            Object[] a = (Object[]) r;
            return inboundDetail(
                    a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]);
        }).toList();
    }

    private static WarehouseInboundDetailProjection inboundDetail(
            Object receiptId, Object receiptNumber, Object receivedDate, Object status,
            Object prNumber, Object vendorCode, Object vendorName, Object productCode, Object productName,
            Object qtyExpected, Object qtyReceived, Object fee, Object realBill, Object billOnPaper, Object createdBy) {
        return new WarehouseInboundDetailProjection() {
            @Override
            public java.util.UUID getReceiptId() {
                return (java.util.UUID) receiptId;
            }

            @Override
            public String getReceiptNumber() {
                return (String) receiptNumber;
            }

            @Override
            public LocalDate getReceivedDate() {
                return (LocalDate) receivedDate;
            }

            @Override
            public String getStatus() {
                return (String) status;
            }

            @Override
            public String getPaymentRequestNumber() {
                return (String) prNumber;
            }

            @Override
            public String getVendorCode() {
                return (String) vendorCode;
            }

            @Override
            public String getVendorName() {
                return (String) vendorName;
            }

            @Override
            public String getProductCode() {
                return (String) productCode;
            }

            @Override
            public String getProductName() {
                return (String) productName;
            }

            @Override
            public BigDecimal getQuantityExpected() {
                return (BigDecimal) qtyExpected;
            }

            @Override
            public BigDecimal getQuantityReceived() {
                return (BigDecimal) qtyReceived;
            }

            @Override
            public BigDecimal getFeeAmount() {
                return (BigDecimal) fee;
            }

            @Override
            public BigDecimal getRealBillAmount() {
                return (BigDecimal) realBill;
            }

            @Override
            public BigDecimal getBillOnPaperAmount() {
                return (BigDecimal) billOnPaper;
            }

            @Override
            public String getCreatedBy() {
                return (String) createdBy;
            }
        };
    }

    private List<WarehouseOutboundDetailProjection> mapOutboundDetail(List<?> rows) {
        return rows.stream()
                .map(r -> {
                    Object[] a = (Object[]) r;
                    return outboundDetail(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12]);
                })
                .toList();
    }

    private static WarehouseOutboundDetailProjection outboundDetail(
            Object id, Object number, Object date, Object status, Object contract, Object orderNum,
            Object productCode, Object productName, Object qty, Object unitPrice, Object total, Object tax, Object createdBy) {
        return new WarehouseOutboundDetailProjection() {
            @Override
            public java.util.UUID getOutboundId() {
                return (java.util.UUID) id;
            }

            @Override
            public String getOutboundNumber() {
                return (String) number;
            }

            @Override
            public LocalDate getOutboundDate() {
                return (LocalDate) date;
            }

            @Override
            public String getStatus() {
                return (String) status;
            }

            @Override
            public String getContractNumber() {
                return (String) contract;
            }

            @Override
            public String getOrderNumber() {
                return (String) orderNum;
            }

            @Override
            public String getProductCode() {
                return (String) productCode;
            }

            @Override
            public String getProductName() {
                return (String) productName;
            }

            @Override
            public BigDecimal getQuantity() {
                return (BigDecimal) qty;
            }

            @Override
            public BigDecimal getUnitPrice() {
                return (BigDecimal) unitPrice;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return (BigDecimal) total;
            }

            @Override
            public BigDecimal getTaxAmount() {
                return (BigDecimal) tax;
            }

            @Override
            public String getCreatedBy() {
                return (String) createdBy;
            }
        };
    }

    private List<WarehouseInventoryDetailProjection> mapInventoryDetail(List<?> rows) {
        return rows.stream()
                .map(r -> {
                    Object[] a = (Object[]) r;
                    return inventoryDetail(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9]);
                })
                .toList();
    }

    private static WarehouseInventoryDetailProjection inventoryDetail(
            Object id, Object code, Object name, Object direction, Object qty,
            Object refType, Object refId, Object createdAt, Object note, Object createdBy) {
        return new WarehouseInventoryDetailProjection() {
            @Override
            public java.util.UUID getTransactionId() {
                return (java.util.UUID) id;
            }

            @Override
            public String getProductCode() {
                return (String) code;
            }

            @Override
            public String getProductName() {
                return (String) name;
            }

            @Override
            public String getDirection() {
                return (String) direction;
            }

            @Override
            public BigDecimal getQuantity() {
                return (BigDecimal) qty;
            }

            @Override
            public String getReferenceType() {
                return (String) refType;
            }

            @Override
            public java.util.UUID getReferenceId() {
                return (java.util.UUID) refId;
            }

            @Override
            public Instant getCreatedAt() {
                return (Instant) createdAt;
            }

            @Override
            public String getNote() {
                return (String) note;
            }

            @Override
            public String getCreatedBy() {
                return (String) createdBy;
            }
        };
    }

    private List<OrderDetailProjection> mapOrderDetail(List<?> rows) {
        return rows.stream()
                .map(r -> {
                    Object[] a = (Object[]) r;
                    return orderDetail(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]);
                })
                .toList();
    }

    private static OrderDetailProjection orderDetail(
            Object orderId, Object orderNumber, Object orderDate, Object status, Object customer,
            Object contract, Object productCode, Object productName, Object vendorCode,
            Object qty, Object unitPrice, Object discount, Object tax, Object total, Object createdBy) {
        return new OrderDetailProjection() {
            @Override
            public java.util.UUID getOrderId() {
                return (java.util.UUID) orderId;
            }

            @Override
            public String getOrderNumber() {
                return (String) orderNumber;
            }

            @Override
            public LocalDate getOrderDate() {
                return (LocalDate) orderDate;
            }

            @Override
            public String getStatus() {
                return (String) status;
            }

            @Override
            public String getCustomerName() {
                return (String) customer;
            }

            @Override
            public String getContractNumber() {
                return (String) contract;
            }

            @Override
            public String getProductCode() {
                return (String) productCode;
            }

            @Override
            public String getProductName() {
                return (String) productName;
            }

            @Override
            public String getVendorCode() {
                return (String) vendorCode;
            }

            @Override
            public BigDecimal getQuantity() {
                return (BigDecimal) qty;
            }

            @Override
            public BigDecimal getUnitPrice() {
                return (BigDecimal) unitPrice;
            }

            @Override
            public BigDecimal getDiscountAmount() {
                return (BigDecimal) discount;
            }

            @Override
            public BigDecimal getTaxAmount() {
                return (BigDecimal) tax;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return (BigDecimal) total;
            }

            @Override
            public String getCreatedBy() {
                return (String) createdBy;
            }
        };
    }

    private List<PaymentRequestDetailProjection> mapPaymentDetail(List<?> rows) {
        return rows.stream()
                .map(r -> {
                    Object[] a = (Object[]) r;
                    return paymentDetail(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14]);
                })
                .toList();
    }

    private static PaymentRequestDetailProjection paymentDetail(
            Object id, Object number, Object date, Object status, Object vendorCode, Object vendorName,
            Object productCode, Object poNumber, Object requested, Object paid, Object fee, Object total,
            Object currentLevel, Object levels, Object createdBy) {
        return new PaymentRequestDetailProjection() {
            @Override
            public java.util.UUID getPaymentRequestId() {
                return (java.util.UUID) id;
            }

            @Override
            public String getRequestNumber() {
                return (String) number;
            }

            @Override
            public Instant getRequestDate() {
                return (Instant) date;
            }

            @Override
            public String getStatus() {
                return (String) status;
            }

            @Override
            public String getVendorCode() {
                return (String) vendorCode;
            }

            @Override
            public String getVendorName() {
                return (String) vendorName;
            }

            @Override
            public String getProductCode() {
                return (String) productCode;
            }

            @Override
            public String getPurchaseOrderNumber() {
                return (String) poNumber;
            }

            @Override
            public BigDecimal getRequestedAmount() {
                return (BigDecimal) requested;
            }

            @Override
            public BigDecimal getPaidAmount() {
                return (BigDecimal) paid;
            }

            @Override
            public BigDecimal getFeeAmount() {
                return (BigDecimal) fee;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return (BigDecimal) total;
            }

            @Override
            public Integer getCurrentApprovalLevel() {
                return (Integer) currentLevel;
            }

            @Override
            public Integer getApprovalLevels() {
                return (Integer) levels;
            }

            @Override
            public String getCreatedBy() {
                return (String) createdBy;
            }
        };
    }
}
