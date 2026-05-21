package com.cnh.ies.entity.export;

import java.time.Instant;
import java.util.UUID;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "export_jobs")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportJobEntity extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "file_info_id")
    private UUID fileInfoId;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
