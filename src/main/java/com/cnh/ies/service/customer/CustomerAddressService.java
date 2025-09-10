package com.cnh.ies.service.customer;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;

import com.cnh.ies.repository.customer.CustomerAddressRepo;
import com.cnh.ies.mapper.customer.AddressMapper;
import com.cnh.ies.model.customer.CreateAddressRequest;
import com.cnh.ies.model.customer.CustomerAddressInfo;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.repository.customer.CustomerRepo;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerAddressService {
    private final CustomerAddressRepo customerAddressRepo;
    private final CustomerRepo customerRepo;
    private final AddressMapper addressMapper;

    public List<CustomerAddressInfo> getAllAddresses(String requestId, String customerId) {
        try {
            log.info("Getting all addresses with requestId: {} | customerId: {}", requestId, customerId);

            Optional<CustomerEntity> customer = customerRepo.findById(UUID.fromString(customerId));

            if (customer.isEmpty()) {
                log.error("Customer not found with id: {} | RequestId: {}", customerId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            List<CustomerAddressEntity> customerAddresses = customerAddressRepo.findAllByCustomerId(customer.get().getId());
            List<CustomerAddressInfo> customerAddressInfos = customerAddresses.stream().map(addressMapper::mapToCustomerAddressInfo).collect(Collectors.toList());

            log.info("Getting all addresses success with requestId: {} | customerId: {} | number of addresses: {}", requestId, customerId, customerAddressInfos.size());

            return customerAddressInfos;
        } catch (Exception e) {
            log.error("Error getting all addresses requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all addresses", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public CustomerAddressInfo createAddress(CreateAddressRequest request, String requestId) {
        try {
            log.info("Creating address with requestId: {} | request: {}", requestId, request);

            Optional<CustomerEntity> customer = customerRepo.findById(UUID.fromString(request.getCustomerId().orElse(null)));

            if (customer.isEmpty()) {
                log.error("Customer not found with id: {} | RequestId: {}", request.getCustomerId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            CustomerAddressEntity customerAddress = addressMapper.mapToCustomerAddressEntity(request, customer.get());

            customerAddressRepo.save(customerAddress);

            log.info("Address created successfully with requestId: {} | request: {}", requestId, request);

            return addressMapper.mapToCustomerAddressInfo(customerAddress);
        } catch (Exception e) {
            log.error("Error creating address requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating address", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String deleteAddress(String id, String requestId) {
        try {
            log.info("Deleting address with id: {} | RequestId: {}", id, requestId);

            Optional<CustomerAddressEntity> customerAddress = customerAddressRepo.findById(UUID.fromString(id));

            if (customerAddress.isEmpty()) {
                log.error("Address not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Address not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            customerAddress.get().setIsDeleted(true);
            customerAddress.get().setUpdatedBy(RequestContext.getCurrentUsername());
            customerAddressRepo.save(customerAddress.get());

            log.info("Address deleted successfully with id: {}", id);

            return "Address deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting address requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting address", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


}
