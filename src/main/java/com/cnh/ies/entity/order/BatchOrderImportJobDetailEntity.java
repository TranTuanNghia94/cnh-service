package com.cnh.ies.entity.order;

import java.util.UUID;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "batch_order_import_job_details")
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchOrderImportJobDetailEntity extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "row_num")
    private Integer rowNum;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;
}
