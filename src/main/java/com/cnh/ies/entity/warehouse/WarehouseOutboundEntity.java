package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cnh.ies.constant.Constant;
import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.order.OrderEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_outbounds")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseOutboundEntity extends BaseEntity {

    @Column(name = "outbound_number", nullable = false, unique = true, length = 50)
    private String outboundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "contract_number", nullable = false, length = 200)
    private String contractNumber;

    @Column(name = "outbound_reason", nullable = false, length = 200)
    private String outboundReason;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "outbound_date", nullable = false)
    private LocalDate outboundDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status = Constant.WAREHOUSE_OUTBOUND_STATUS_DRAFT;

    @Column(name = "approval_levels", nullable = false)
    private Integer approvalLevels = 0;

    @Column(name = "current_approval_level", nullable = false)
    private Integer currentApprovalLevel = 0;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
