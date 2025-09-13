package com.cnh.ies.service.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.repository.customer.CustomerRepo;

import jakarta.transaction.Transactional;

import com.cnh.ies.repository.customer.CustomerAddressRepo;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.customer.CreateCustomerRequest;
import com.cnh.ies.model.customer.CustomerInfo;
import com.cnh.ies.model.customer.UpdateCustomerRequest;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.customer.CustomerMapper;
import com.cnh.ies.mapper.customer.AddressMapper;
import com.cnh.ies.util.RequestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepo customerRepo;
    private final CustomerAddressRepo customerAddressRepo;
    private final CustomerMapper customerMapper;
    private final AddressMapper addressMapper;

    public ListDataModel<CustomerInfo> getAllCustomers(String requestId, Integer page, Integer limit) {
        try {
            log.info("Getting all customers with requestId: {} page: {} limit: {}", requestId, page, limit);

            Page<CustomerEntity> customers = customerRepo.findAllAndIsDeletedFalse(PageRequest.of(page, limit));

            List<CustomerInfo> customerInfos = customers.stream().map(customerMapper::mapToCustomerInfo)
                    .collect(Collectors.toList());
            PaginationModel pagination = PaginationModel.builder()
                    .page(page)
                    .limit(limit)
                    .total(customers.getTotalElements())
                    .totalPage(customers.getTotalPages())
                    .build();

            log.info("Getting all customers success with requestId: {} page: {} limit: {}", requestId, page, limit);

            return ListDataModel.<CustomerInfo>builder()
                    .data(customerInfos)
                    .pagination(pagination)
                    .build();
        } catch (Exception e) {
            log.error("Error getting all customers requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all customers",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


    @Transactional
    public CustomerInfo createCustomer(CreateCustomerRequest request, String requestId) {
        try {
            log.info("Creating customer with 0/3 steps: {} | RequestId: {}", request, requestId);

            CustomerEntity customer = customerMapper.mapToCustomerEntity(request);
            customerRepo.save(customer);

            log.info("Customer created successfully with request 1/3: {}", requestId);

            if (request.getAddresses() != null) {
                List<CustomerAddressEntity> addresses = request.getAddresses().stream().map(address -> addressMapper.mapToCustomerAddressEntity(address, customer)).collect(Collectors.toList());
                
                customerAddressRepo.saveAll(addresses);
                log.info("Customer addresses created successfully with request 2/3: {}", requestId);
            }

            log.info("Customer created successfully with request 3/3: {}", requestId);

            return customerMapper.mapToCustomerInfo(customer);
        } catch (Exception e) {
            log.error("Error creating customer requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating customer", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


    public CustomerInfo getCustomerById(String id, String requestId) {
        try {
            log.info("Getting customer by id: {} | RequestId: {}", id, requestId);

            Optional<CustomerEntity> customer = customerRepo.findByIdAndIsDeletedFalse(UUID.fromString(id));

            if (customer.isEmpty()) {
                log.error("Customer not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Customer fetched successfully with id: {}", id);

            return customerMapper.mapToCustomerInfo(customer.get());
        } catch (Exception e) {
            log.error("Error getting customer by id requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting customer by id", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public String deleteCustomer(String id, String requestId) {
        try {
            log.info("Deleting customer with id: {} | RequestId: {}", id, requestId);

            Optional<CustomerEntity> customer = customerRepo.findById(UUID.fromString(id));

            if (customer.isEmpty()) {
                log.error("Customer not found with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (customer.get().getIsDeleted()) {
                log.error("Customer already deleted with id: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer already deleted", HttpStatus.NOT_FOUND.value(), requestId);
            }

            customer.get().setCode(customer.get().getCode() + "_" + "DELETED" + "_" + requestId);
            customer.get().setIsDeleted(true);
            customer.get().setUpdatedBy(RequestContext.getCurrentUsername());

            customerRepo.save(customer.get());

            log.info("Customer deleted successfully with id: {}", id);

            return "Customer deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting customer requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting customer", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
    

    @Transactional
    public String updateCustomer(UpdateCustomerRequest request, String requestId) {
        try {
            log.info("Updating customer with 0/3 steps RequestId: {} | Request: {}", requestId, request);


            Optional<CustomerEntity> customer = customerRepo.findById(UUID.fromString(request.getId()));

            if (customer.isEmpty()) {
                log.error("Customer not found with id: {} | RequestId: {}", request.getId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (request.getCode() == null) {
                log.error("Customer code is required | RequestId: {}", requestId);
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Customer code is required", HttpStatus.BAD_REQUEST.value(), requestId);
            }
            
            CustomerEntity customerEntity = customerMapper.mapToCustomerEntity(request);
            customerRepo.save(customerEntity);

            log.info("Customer updated successfully with request 1/3: {}", requestId);

            if (request.getAddresses() != null) {
                List<CustomerAddressEntity> addresses = request.getAddresses().stream().map(address -> addressMapper.mapToCustomerAddressEntity(address, customerEntity)).collect(Collectors.toList());
                customerAddressRepo.saveAll(addresses);
                log.info("Customer addresses updated successfully with request 2/3: {}", requestId);
            }


            log.info("Customer updated successfully with request 3/3: {}", requestId);

            return "Customer updated successfully";
        }
        catch (Exception e) {
            log.error("Error updating customer requestId: {} | error: {}", requestId, e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error updating customer", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
