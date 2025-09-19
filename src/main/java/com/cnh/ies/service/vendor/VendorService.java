package com.cnh.ies.service.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.repository.vendors.VendorsRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.vendors.VendorsMapper;
import com.cnh.ies.model.vendors.VendorInfo;
import com.cnh.ies.model.vendors.CreateVendorRequest;
import com.cnh.ies.model.vendors.UpdateVendorRequest;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VendorService {
    private final VendorsRepo vendorsRepo;
    private final VendorsMapper vendorsMapper;
    private final VendorBanksService vendorBanksService;

    public ListDataModel<VendorInfo> getAllVendors(String requestId, Integer page, Integer limit) {
        try {
            log.info("Getting all vendors | RequestId: {} page: {} limit: {}", requestId, page, limit);
            Page<VendorsEntity> vendors = vendorsRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
            
            List<VendorInfo> vendorInfos = vendorsMapper.toVendorInfoList(vendors.getContent());
           

            PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(limit)
                .total(vendors.getTotalElements())
                .totalPage(vendors.getTotalPages())
                .build();

            log.info("Getting all vendors success | RequestId: {} page: {} limit: {} total: {} totalPage: {}", requestId, page, limit, vendors.getTotalElements(), vendors.getTotalPages());
            
            return ListDataModel.<VendorInfo>builder()
                .data(vendorInfos)
                .pagination(pagination)
                .build();
        } catch (Exception e) {
            log.error("Error getting all vendors", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public VendorInfo createVendor(CreateVendorRequest request, String requestId) {
        try {
            log.info("Creating vendor with 0/3 steps: {} | RequestId: {}", request, requestId);

            if (vendorsRepo.findByCode(request.getCode()).isPresent()) {
                log.error("Vendor code already exists with code: {} RequestId: {}", request.getCode(), requestId);
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Vendor code already exists", HttpStatus.BAD_REQUEST.value(), requestId);
            }

            VendorsEntity vendor = vendorsMapper.toVendorsEntity(request);
            vendorsRepo.save(vendor);
            log.info("Vendor created successfully with request 1/3: {}", requestId);

            if (request.getBanks().isPresent()) {
                vendorBanksService.createVendorBanks( request.getBanks().get(), requestId, vendor);

                log.info("Vendor banks created successfully with request 2/3: {}", requestId);
            }

            log.info("Vendor created successfully with request 3/3: {}", requestId);

            return vendorsMapper.toVendorInfo(vendor);
        } catch (Exception e) {
            log.error("Error creating vendor", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public VendorInfo getVendorById(String id, String requestId) {
        try {
            log.info("Getting vendor by id: {} | RequestId: {}", id, requestId);
            Optional<VendorsEntity> vendor = vendorsRepo.findById(UUID.fromString(id));
            if (vendor.isEmpty()) {
                log.error("Vendor not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (vendor.get().getIsDeleted()) {
                log.error("Vendor is deleted with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor is deleted", HttpStatus.NOT_FOUND.value(), requestId);
            }
            log.info("Vendor fetched successfully with id: {}", id);

            return vendorsMapper.toVendorInfo(vendor.get());
        } catch (Exception e) {
            log.error("Error getting vendor by id", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public VendorInfo updateVendor(UpdateVendorRequest request, String requestId) {
        try {
            log.info("Updating vendor with id: {} | RequestId: {}", request.getId(), requestId);

            Optional<VendorsEntity> vendor = vendorsRepo.findById(UUID.fromString(request.getId()));
            if (vendor.isEmpty()) {
                log.error("Vendor not found with id: {} | RequestId: {}", request.getId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (vendor.get().getIsDeleted()) {
                log.error("Vendor is deleted with id: {} | RequestId: {}", request.getId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor is deleted", HttpStatus.NOT_FOUND.value(), requestId);
            }
            
            VendorsEntity vendorEntity = vendorsMapper.toVendorsEntity(request);
            vendorsRepo.save(vendorEntity);

            log.info("Vendor updated successfully with id: {}", request.getId());
            return vendorsMapper.toVendorInfo(vendorEntity);
        } catch (Exception e) {
            log.error("Error updating vendor", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


    public String deleteVendor(String id, String requestId) {
        try {
            log.info("Deleting vendor with id: {} | RequestId: {}", id, requestId);
            

            Optional<VendorsEntity> vendor = vendorsRepo.findById(UUID.fromString(id));
            if (vendor.isEmpty()) {
                log.error("Vendor not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (vendor.get().getIsDeleted()) {
                log.error("Vendor is deleted with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor already deleted", HttpStatus.NOT_FOUND.value(), requestId);
            }

            vendor.get().setIsDeleted(true);
            vendor.get().setCode(vendor.get().getCode() + "_" + "DELETED" + "_" + requestId);
            vendor.get().setUpdatedAt(Instant.now());
            vendor.get().setUpdatedBy(RequestContext.getCurrentUsername());
            vendorsRepo.save(vendor.get());

            log.info("Vendor deleted successfully with id: {}", id);

            return "Vendor deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting vendor", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
    

}
