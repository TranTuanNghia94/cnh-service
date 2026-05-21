package com.cnh.ies.service.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.cnh.ies.entity.export.ExportJobEntity;
import com.cnh.ies.entity.file.FileInfoEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.export.ExportJobInfo;
import com.cnh.ies.repository.export.ExportJobRepo;
import com.cnh.ies.repository.file.FileInfoRepo;
import com.cnh.ies.service.file.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ExportJobServiceTest {

    @Mock
    private ExportJobRepo exportJobRepo;
    @Mock
    private FileInfoRepo fileInfoRepo;
    @Mock
    private FileService fileService;
    @Mock
    private ExportJobWorkerService workerService;

    @InjectMocks
    private ExportJobService exportJobService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getOwnedJob_forbiddenWhenOwnerMismatch() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ExportJobEntity job = new ExportJobEntity();
        job.setId(jobId);
        job.setOwnerUserId(owner);
        job.setType(ExportJobType.PRODUCTS);
        job.setStatus(ExportJobStatus.PENDING);

        when(exportJobRepo.findByIdAndIsDeletedFalse(jobId)).thenReturn(Optional.of(job));

        ExportJobService service = new ExportJobService(
                exportJobRepo, fileInfoRepo, fileService, objectMapper, workerService);

        assertThrows(ApiException.class, () -> service.getOwnedJob(jobId.toString(), other, "rid"));
    }

    @Test
    void getOwnedJob_includesViewUrlOnSuccess() {
        UUID owner = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID fileInfoId = UUID.randomUUID();

        ExportJobEntity job = new ExportJobEntity();
        job.setId(jobId);
        job.setOwnerUserId(owner);
        job.setType(ExportJobType.PRODUCTS);
        job.setStatus(ExportJobStatus.SUCCESS);
        job.setFileInfoId(fileInfoId);
        job.setFileName("products_export.xlsx");

        FileInfoEntity file = new FileInfoEntity();
        file.setId(fileInfoId);
        file.setFilePath("exports/products/2026/5/21/uuid_products.xlsx");
        file.setIsDeleted(false);

        when(exportJobRepo.findByIdAndIsDeletedFalse(jobId)).thenReturn(Optional.of(job));
        when(fileInfoRepo.findById(fileInfoId)).thenReturn(Optional.of(file));
        when(fileService.presignGetUrlForKey(file.getFilePath())).thenReturn("https://signed-url");

        ExportJobService service = new ExportJobService(
                exportJobRepo, fileInfoRepo, fileService, objectMapper, workerService);

        ExportJobInfo info = service.getOwnedJob(jobId.toString(), owner, "rid");

        assertEquals(ExportJobStatus.SUCCESS, info.getStatus());
        assertEquals("https://signed-url", info.getViewUrl());
    }

    @Test
    void markSuccess_updatesJobFields() {
        UUID jobId = UUID.randomUUID();
        UUID fileInfoId = UUID.randomUUID();

        ExportJobEntity job = new ExportJobEntity();
        job.setId(jobId);
        job.setStatus(ExportJobStatus.RUNNING);

        when(exportJobRepo.findByIdAndIsDeletedFalse(jobId)).thenReturn(Optional.of(job));
        when(exportJobRepo.save(any(ExportJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ExportJobService service = new ExportJobService(
                exportJobRepo, fileInfoRepo, fileService, objectMapper, workerService);

        service.markSuccess(jobId, fileInfoId, "export.xlsx");

        verify(exportJobRepo).save(job);
        assertEquals(ExportJobStatus.SUCCESS, job.getStatus());
        assertEquals(fileInfoId, job.getFileInfoId());
    }

    @Test
    void listOwnedJobs_returnsPagedResults() {
        UUID owner = UUID.randomUUID();
        ExportJobEntity job = new ExportJobEntity();
        job.setId(UUID.randomUUID());
        job.setOwnerUserId(owner);
        job.setType(ExportJobType.VENDORS);
        job.setStatus(ExportJobStatus.PENDING);

        Page<ExportJobEntity> page = new PageImpl<>(java.util.List.of(job));
        when(exportJobRepo.findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(page);

        ExportJobService service = new ExportJobService(
                exportJobRepo, fileInfoRepo, fileService, objectMapper, workerService);

        var result = service.listOwnedJobs(owner, 0, 20, "rid");

        assertEquals(1, result.getData().size());
        assertEquals(ExportJobType.VENDORS, result.getData().get(0).getType());
    }
}
