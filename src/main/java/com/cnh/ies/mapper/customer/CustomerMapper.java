package com.cnh.ies.mapper.customer;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.model.customer.CustomerInfo;
import com.cnh.ies.model.customer.CreateCustomerRequest;

@Component
public class CustomerMapper {

    public CustomerInfo mapToCustomerInfo(CustomerEntity customer) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(customer.getId().toString());
        customerInfo.setCode(customer.getCode());
        customerInfo.setName(customer.getName());
        customerInfo.setEmail(customer.getEmail());
        customerInfo.setPhone(customer.getPhone());
        customerInfo.setTaxCode(customer.getTaxCode());
        customerInfo.setMisaCode(customer.getMisaCode());
        customerInfo.setIsActive(customer.getIsActive());
        customerInfo.setCreatedAt(customer.getCreatedAt().toString());
        customerInfo.setUpdatedAt(customer.getUpdatedAt().toString());
        customerInfo.setCreatedBy(customer.getCreatedBy());
        customerInfo.setUpdatedBy(customer.getUpdatedBy());

        return customerInfo;
    }

    public CustomerEntity mapToCustomerEntity(CreateCustomerRequest request) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCode(request.getCode());
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setTaxCode(request.getTaxCode());
        customer.setMisaCode(request.getMisaCode());
        customer.setIsActive(true);
        
        return customer;
    }

}
