package com.cnh.ies.entity.warehouse;

import java.time.Instant;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.auth.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_inbound_receipt_approvals")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInboundReceiptApprovalEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private WarehouseInboundReceiptEntity receipt;

    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel;

    @Column(name = "approval_role", nullable = false, length = 50)
    private String approvalRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private UserEntity approver;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
