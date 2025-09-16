package com.cnh.ies.mapper.vendors;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.model.vendors.CreateVendorRequest;
import com.cnh.ies.model.vendors.VendorInfo;
import com.cnh.ies.util.RequestContext;

@Component
public class VendorsMapper {
    public static VendorInfo toVendorInfo(VendorsEntity vendorsEntity) { 
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

        return vendorInfo;
    }

    public static VendorsEntity toVendorsEntity(CreateVendorRequest request) {
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
}
