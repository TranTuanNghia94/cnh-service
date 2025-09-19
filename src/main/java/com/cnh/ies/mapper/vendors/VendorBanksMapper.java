package com.cnh.ies.mapper.vendors;

import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.model.vendors.CreateVendorBanksRequest;
import com.cnh.ies.model.vendors.UpdateVendorBanksRequest;
import com.cnh.ies.model.vendors.VendorBanksInfo;
import com.cnh.ies.util.RequestContext;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VendorBanksMapper {

    public List<VendorBanksInfo> toVendorBanksInfoList(List<VendorBanksEntity> vendorBanksEntity) {
        return vendorBanksEntity.stream().map(this::toVendorBanksInfo).collect(Collectors.toList());
    }

    public VendorBanksEntity toVendorBanksEntity(CreateVendorBanksRequest request, VendorsEntity vendor) {
        VendorBanksEntity vendorBanksEntity = new VendorBanksEntity();
        vendorBanksEntity.setVendor(vendor);
        vendorBanksEntity.setBankName(request.getBankName());
        vendorBanksEntity.setBankAccountName(request.getBankAccountName());
        vendorBanksEntity.setBankAccountNumber(request.getBankAccountNumber());
        vendorBanksEntity.setBankAccountBranch(request.getBankAccountBranch().orElse(null));
        vendorBanksEntity.setBankAccountSwift(request.getBankAccountSwift().orElse(null));
        vendorBanksEntity.setBankAccountIban(request.getBankAccountIban().orElse(null));
        vendorBanksEntity.setIsActive(true);
        vendorBanksEntity.setCreatedBy(RequestContext.getCurrentUsername());
        vendorBanksEntity.setUpdatedBy(RequestContext.getCurrentUsername());
        
        return vendorBanksEntity;
    }

    public VendorBanksInfo toVendorBanksInfo(VendorBanksEntity vendorBanksEntity) {
        VendorBanksInfo vendorBanksInfo = new VendorBanksInfo();
        vendorBanksInfo.setId(vendorBanksEntity.getId().toString());
        vendorBanksInfo.setBankName(vendorBanksEntity.getBankName());
        vendorBanksInfo.setBankAccountName(vendorBanksEntity.getBankAccountName());
        vendorBanksInfo.setBankAccountNumber(vendorBanksEntity.getBankAccountNumber());
        vendorBanksInfo.setBankAccountBranch(vendorBanksEntity.getBankAccountBranch());
        vendorBanksInfo.setBankAccountSwift(vendorBanksEntity.getBankAccountSwift());
        vendorBanksInfo.setBankAccountIban(vendorBanksEntity.getBankAccountIban());
        vendorBanksInfo.setIsActive(vendorBanksEntity.getIsActive());
        vendorBanksInfo.setCreatedAt(vendorBanksEntity.getCreatedAt());
        vendorBanksInfo.setUpdatedAt(vendorBanksEntity.getUpdatedAt());
        vendorBanksInfo.setCreatedBy(vendorBanksEntity.getCreatedBy());
        vendorBanksInfo.setUpdatedBy(vendorBanksEntity.getUpdatedBy());
        
        return vendorBanksInfo;
    }

    public VendorBanksEntity toVendorBanksEntity(UpdateVendorBanksRequest request, VendorsEntity vendor) {
        VendorBanksEntity vendorBanksEntity = new VendorBanksEntity();
        vendorBanksEntity.setVendor(vendor);
        vendorBanksEntity.setBankName(request.getBankName());
        vendorBanksEntity.setBankAccountName(request.getBankAccountName());
        vendorBanksEntity.setBankAccountNumber(request.getBankAccountNumber());
        vendorBanksEntity.setBankAccountBranch(request.getBankAccountBranch().orElse(null));
        vendorBanksEntity.setBankAccountSwift(request.getBankAccountSwift().orElse(null));
        vendorBanksEntity.setBankAccountIban(request.getBankAccountIban().orElse(null));
        vendorBanksEntity.setIsActive(true);
        vendorBanksEntity.setUpdatedBy(RequestContext.getCurrentUsername());
        vendorBanksEntity.setIsDeleted(request.getIsDeleted() != null ? request.getIsDeleted().orElse(false) : false);
        
        return vendorBanksEntity;
    }
    
}
