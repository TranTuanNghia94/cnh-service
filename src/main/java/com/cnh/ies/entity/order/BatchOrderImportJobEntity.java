package com.cnh.ies.entity.order;

import java.time.Instant;
import java.util.UUID;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "batch_order_import_jobs")
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchOrderImportJobEntity extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "file_info_id", nullable = false)
    private UUID fileInfoId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "original_file_name", length = 512)
    private String originalFileName;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "success_rows", nullable = false)
    private Integer successRows = 0;

    @Column(name = "error_rows", nullable = false)
    private Integer errorRows = 0;

    @Column(name = "warning_rows", nullable = false)
    private Integer warningRows = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "result_summary_json", columnDefinition = "TEXT")
    private String resultSummaryJson;
}
