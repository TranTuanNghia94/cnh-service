package com.cnh.ies.mapper.vendors;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.model.vendors.CreateVendorRequest;
import com.cnh.ies.model.vendors.UpdateVendorRequest;
import com.cnh.ies.model.vendors.VendorInfo;
import com.cnh.ies.util.RequestContext;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VendorsMapper {
    private final VendorBanksMapper vendorBanksMapper;

    public List<VendorInfo> toVendorInfoList(List<VendorsEntity> vendorsEntity) {
        return vendorsEntity.stream().map(this::toVendorInfo).collect(Collectors.toList());
    }
    
    public VendorInfo toVendorInfo(VendorsEntity vendorsEntity) { 
        VendorInfo vendorInfo = new VendorInfo();
        vendorInfo.setId(vendorsEntity.getId().toString());
        vendorInfo.setCode(vendorsEntity.getCode());
        vendorInfo.setName(vendorsEntity.getName());
        vendorInfo.setEmail(vendorsEntity.getEmail());
        vendorInfo.setCountry(vendorsEntity.getCountry());
        vendorInfo.setCurrency(vendorsEntity.getCurrency());
        vendorInfo.setPhone(vendorsEntity.getPhone());
        vendorInfo.setMisaCode(vendorsEntity.getMisaCode());
        vendorInfo.setAddress(vendorsEntity.getAddress());
        vendorInfo.setTaxCode(vendorsEntity.getTaxCode());
        vendorInfo.setContactPerson(vendorsEntity.getContactPerson());
        vendorInfo.setIsActive(vendorsEntity.getIsActive());
        vendorInfo.setCreatedAt(vendorsEntity.getCreatedAt());
        vendorInfo.setUpdatedAt(vendorsEntity.getUpdatedAt());
        vendorInfo.setCreatedBy(vendorsEntity.getCreatedBy());
        vendorInfo.setUpdatedBy(vendorsEntity.getUpdatedBy());
        vendorInfo.setIsDeleted(vendorsEntity.getIsDeleted());
        vendorInfo.setBanks(vendorsEntity.getBanks() != null ? 
            vendorsEntity.getBanks().stream()
                .filter(bank -> !bank.getIsDeleted()) // Filter out deleted banks
                .map(vendorBanksMapper::toVendorBanksInfo)
                .collect(Collectors.toList()) : 
            List.of());

        return vendorInfo;
    }

    public VendorInfo toVendorInfoWithBanks(VendorsEntity vendorsEntity, List<VendorBanksEntity> banks) { 
        VendorInfo vendorInfo = new VendorInfo();
        vendorInfo.setId(vendorsEntity.getId().toString());
        vendorInfo.setCode(vendorsEntity.getCode());
        vendorInfo.setName(vendorsEntity.getName());
        vendorInfo.setEmail(vendorsEntity.getEmail());
        vendorInfo.setCountry(vendorsEntity.getCountry());
        vendorInfo.setCurrency(vendorsEntity.getCurrency());
        vendorInfo.setPhone(vendorsEntity.getPhone());
        vendorInfo.setMisaCode(vendorsEntity.getMisaCode());
        vendorInfo.setAddress(vendorsEntity.getAddress());
        vendorInfo.setTaxCode(vendorsEntity.getTaxCode());
        vendorInfo.setContactPerson(vendorsEntity.getContactPerson());
        vendorInfo.setIsActive(vendorsEntity.getIsActive());
        vendorInfo.setCreatedAt(vendorsEntity.getCreatedAt());
        vendorInfo.setUpdatedAt(vendorsEntity.getUpdatedAt());
        vendorInfo.setCreatedBy(vendorsEntity.getCreatedBy());
        vendorInfo.setUpdatedBy(vendorsEntity.getUpdatedBy());
        vendorInfo.setIsDeleted(vendorsEntity.getIsDeleted());
        
        // Use the provided banks list instead of accessing the entity's banks collection
        vendorInfo.setBanks(banks != null ? 
            banks.stream()
                .filter(bank -> !bank.getIsDeleted()) // Filter out deleted banks
                .map(vendorBanksMapper::toVendorBanksInfo)
                .collect(Collectors.toList()) : 
            List.of());

        return vendorInfo;
    }

    public VendorsEntity toVendorsEntity(CreateVendorRequest request) {
        VendorsEntity vendorsEntity = new VendorsEntity();
        vendorsEntity.setCode(request.getCode());
        vendorsEntity.setName(request.getName());
        vendorsEntity.setEmail(request.getEmail());
        vendorsEntity.setCountry(request.getCountry());
        vendorsEntity.setCurrency(request.getCurrency());
        vendorsEntity.setPhone(request.getPhone());
        vendorsEntity.setMisaCode(request.getMisaCode());
        vendorsEntity.setAddress(request.getAddress());
        vendorsEntity.setTaxCode(request.getTaxCode());
        vendorsEntity.setContactPerson(request.getContactPerson());
        vendorsEntity.setIsActive(true);
        vendorsEntity.setCreatedBy(RequestContext.getCurrentUsername());
        vendorsEntity.setUpdatedBy(RequestContext.getCurrentUsername());
        
        return vendorsEntity;
    }

    public VendorsEntity toVendorsEntity(UpdateVendorRequest request) {
        VendorsEntity vendorsEntity = new VendorsEntity();
        vendorsEntity.setId(UUID.fromString(request.getId()));
        vendorsEntity.setCode(request.getCode());
        vendorsEntity.setName(request.getName());
        vendorsEntity.setEmail(request.getEmail());
        vendorsEntity.setPhone(request.getPhone());
        vendorsEntity.setMisaCode(request.getMisaCode());
        vendorsEntity.setAddress(request.getAddress());
        vendorsEntity.setTaxCode(request.getTaxCode());
        vendorsEntity.setContactPerson(request.getContactPerson());
        vendorsEntity.setUpdatedBy(RequestContext.getCurrentUsername());
        
        return vendorsEntity;
    }
}
