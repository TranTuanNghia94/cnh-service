package com.cnh.ies.service.vendor;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import com.cnh.ies.repository.vendors.VendorBanksRepo;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.mapper.vendors.VendorBanksMapper;
import com.cnh.ies.model.vendors.VendorBanksInfo;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.vendors.CreateVendorBanksRequest;
import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.model.general.PaginationModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VendorBanksService {
    private final VendorBanksRepo vendorBanksRepo;
    private final VendorBanksMapper vendorBanksMapper;
    
    public ListDataModel<VendorBanksInfo> getAllVendorBanks(String requestId, Integer page, Integer limit) {
        try {
            log.info("Getting all vendor banks | RequestId: {} page: {} limit: {}", requestId, page, limit);
            Page<VendorBanksEntity> vendorBanks = vendorBanksRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));
           
            List<VendorBanksInfo> vendorBanksInfo = vendorBanksMapper.toVendorBanksInfoList(vendorBanks.getContent());

            PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(limit)
                .total(vendorBanks.getTotalElements())
                .totalPage(vendorBanks.getTotalPages())
                .build();

            log.info("Getting all vendor banks success | RequestId: {} page: {} limit: {} total: {} totalPage: {}", requestId, page, limit, vendorBanks.getTotalElements(), vendorBanks.getTotalPages());

            return ListDataModel.<VendorBanksInfo>builder()
                .data(vendorBanksInfo)
                .pagination(pagination)
                .build();
        } catch (Exception e) {
            log.error("Error getting all vendor banks", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public VendorBanksInfo createVendorBank(CreateVendorBanksRequest request, String requestId, VendorsEntity vendor) {
        try {
            log.info("Creating vendor bank | RequestId: {} request: {} vendor: {}", requestId, request, vendor);

            VendorBanksEntity vendorBanks = vendorBanksMapper.toVendorBanksEntity(request, vendor);
            vendorBanksRepo.save(vendorBanks);

            log.info("Vendor bank created successfully | RequestId: {} request: {} vendor: {}", requestId, request, vendor);

            return vendorBanksMapper.toVendorBanksInfo(vendorBanks);
        } catch (Exception e) {
            log.error("Error creating vendor bank", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public List<VendorBanksInfo> createVendorBanks(List<CreateVendorBanksRequest> request, String requestId, VendorsEntity vendor) {
        try {
            log.info("Creating vendor banks | RequestId: {} request: {} vendor: {}", requestId, request, vendor);

            List<VendorBanksEntity> vendorBanks = request.stream().map(bank -> vendorBanksMapper.toVendorBanksEntity(bank, vendor)).collect(Collectors.toList());
            vendorBanksRepo.saveAll(vendorBanks);

            log.info("Vendor banks created successfully | RequestId: {} request: {} vendor: {}", requestId, request, vendor);

            return vendorBanksMapper.toVendorBanksInfoList(vendorBanks);
        } catch (Exception e) {
            log.error("Error creating vendor banks", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String deleteVendorBank(String requestId, String vendorBankId) {
        try {
            log.info("Deleting vendor bank | RequestId: {} vendorBankId: {}", requestId, vendorBankId);
            Optional<VendorBanksEntity> vendorBank = vendorBanksRepo.findById(UUID.fromString(vendorBankId));
            if (vendorBank.isEmpty()) {
                log.error("Vendor bank not found | RequestId: {} vendorBankId: {}", requestId, vendorBankId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Vendor bank not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            vendorBank.get().setIsDeleted(true);
            vendorBank.get().setUpdatedBy(RequestContext.getCurrentUsername());
            vendorBanksRepo.save(vendorBank.get());

            log.info("Vendor bank deleted successfully | RequestId: {} vendorBankId: {}", requestId, vendorBankId);

            return "Vendor bank deleted successfully";

        } catch (Exception e) {
            log.error("Error deleting vendor bank | RequestId: {} vendorBankId: {}", requestId, vendorBankId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
