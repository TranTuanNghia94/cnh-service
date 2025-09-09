package com.cnh.ies.mapper.customer;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.model.customer.CustomerAddressInfo;
import com.cnh.ies.model.customer.CreateAddressRequest;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.util.RequestContext;

@Component
public class AddressMapper {
    public CustomerAddressInfo mapToCustomerAddressInfo(CustomerAddressEntity customerAddress) {
        CustomerAddressInfo customerAddressInfo = new CustomerAddressInfo();
        customerAddressInfo.setCustomerId(customerAddress.getCustomer().getId().toString());
        customerAddressInfo.setId(customerAddress.getId().toString());
        customerAddressInfo.setAddress(customerAddress.getAddress());
        customerAddressInfo.setContactPerson(customerAddress.getContactPerson());
        customerAddressInfo.setPhone(customerAddress.getPhone());
        customerAddressInfo.setEmail(customerAddress.getEmail());
        customerAddressInfo.setCreatedAt(customerAddress.getCreatedAt().toString());
        customerAddressInfo.setUpdatedAt(customerAddress.getUpdatedAt().toString());
        customerAddressInfo.setCreatedBy(customerAddress.getCreatedBy());
        customerAddressInfo.setUpdatedBy(customerAddress.getUpdatedBy());

        return customerAddressInfo;
    }

    public CustomerAddressEntity mapToCustomerAddressEntity(CreateAddressRequest request,CustomerEntity customer) {
        CustomerAddressEntity customerAddress = new CustomerAddressEntity();
        customerAddress.setAddress(request.getAddress());
        customerAddress.setContactPerson(request.getContactPerson());
        customerAddress.setPhone(request.getPhone());
        customerAddress.setEmail(request.getEmail());
        customerAddress.setCustomer(customer);
        customerAddress.setCreatedBy(RequestContext.getCurrentUsername());
        customerAddress.setUpdatedBy(RequestContext.getCurrentUsername());
        customerAddress.setIsDeleted(false);

        return customerAddress;
    }

}
