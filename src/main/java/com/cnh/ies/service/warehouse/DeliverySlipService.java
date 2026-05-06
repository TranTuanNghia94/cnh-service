package com.cnh.ies.service.warehouse;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.entity.order.OrderEntity;
import com.cnh.ies.entity.warehouse.WarehouseOutboundDetailEntity;
import com.cnh.ies.entity.warehouse.WarehouseOutboundEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.warehouse.DeliverySlipInfo;
import com.cnh.ies.model.warehouse.DeliverySlipLineInfo;
import com.cnh.ies.repository.warehouse.WarehouseOutboundDetailRepo;
import com.cnh.ies.repository.warehouse.WarehouseOutboundRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliverySlipService {

    private final WarehouseOutboundRepo warehouseOutboundRepo;
    private final WarehouseOutboundDetailRepo warehouseOutboundDetailRepo;

    public DeliverySlipInfo getByWarehouseOutbound(String outboundId, String requestId) {
        UUID outboundUuid = parseUuid(outboundId, requestId);
        WarehouseOutboundEntity outbound = warehouseOutboundRepo.findByIdForDeliverySlip(outboundUuid)
                .orElseThrow(() -> new ApiException(
                        ApiException.ErrorCode.NOT_FOUND,
                        "Warehouse outbound not found",
                        HttpStatus.NOT_FOUND.value(),
                        requestId));

        List<WarehouseOutboundDetailEntity> details = warehouseOutboundDetailRepo.findByOutboundId(outbound.getId());
        return toDeliverySlipInfo(outbound, details);
    }

    public DeliverySlipInfo getByOutboundNumber(String outboundNumber, String requestId) {
        String normalizedOutboundNumber = normalize(outboundNumber);
        if (normalizedOutboundNumber == null) {
            throw new ApiException(
                    ApiException.ErrorCode.BAD_REQUEST,
                    "outboundNumber is required",
                    HttpStatus.BAD_REQUEST.value(),
                    requestId);
        }

        WarehouseOutboundEntity outbound = warehouseOutboundRepo
                .findByOutboundNumberForDeliverySlip(normalizedOutboundNumber)
                .orElseThrow(() -> new ApiException(
                        ApiException.ErrorCode.NOT_FOUND,
                        "Warehouse outbound not found",
                        HttpStatus.NOT_FOUND.value(),
                        requestId));

        List<WarehouseOutboundDetailEntity> details = warehouseOutboundDetailRepo.findByOutboundId(outbound.getId());
        return toDeliverySlipInfo(outbound, details);
    }

    private DeliverySlipInfo toDeliverySlipInfo(
            WarehouseOutboundEntity outbound,
            List<WarehouseOutboundDetailEntity> details) {
        DeliverySlipInfo info = new DeliverySlipInfo();
        OrderEntity order = outbound.getOrder();
        CustomerEntity customer = order == null ? null : order.getCustomer();
        CustomerAddressEntity customerAddress = order == null ? null : order.getCustomerAddress();

        info.setOutboundId(outbound.getId().toString());
        info.setOutboundNumber(outbound.getOutboundNumber());
        info.setOutboundDate(outbound.getOutboundDate() == null ? null : outbound.getOutboundDate().toString());
        info.setOutboundReason(outbound.getOutboundReason());
        info.setOutboundStatus(outbound.getStatus());
        info.setContractNumber(outbound.getContractNumber());
        info.setNote(outbound.getNote());
        info.setCreatedAt(outbound.getCreatedAt() == null ? null : outbound.getCreatedAt().toString());
        info.setCreatedBy(outbound.getCreatedBy());
        info.setCurrency(outbound.getCurrency());
        info.setTotalAmount(outbound.getTotalAmount());
        info.setTaxAmount(outbound.getTaxAmount());

        if (order != null) {
            info.setOrderId(order.getId().toString());
            info.setOrderNumber(order.getOrderPrefix() + order.getOrderNumber());
            info.setOrderDate(order.getOrderDate() == null ? null : order.getOrderDate().toString());
            info.setDeliveryDate(order.getDeliveryDate() == null ? null : order.getDeliveryDate().toString());
            info.setOrderStatus(order.getStatus());
        }

        if (customer != null) {
            info.setCustomerId(customer.getId().toString());
            info.setCustomerCode(customer.getCode());
            info.setCustomerName(customer.getName());
            info.setCustomerPhone(customer.getPhone());
            info.setCustomerEmail(customer.getEmail());
            info.setCustomerTaxCode(customer.getTaxCode());
        }

        if (customerAddress != null) {
            info.setCustomerAddressId(customerAddress.getId().toString());
            info.setCustomerAddress(customerAddress.getAddress());
            info.setCustomerContactPerson(customerAddress.getContactPerson());
            info.setCustomerAddressPhone(customerAddress.getPhone());
            info.setCustomerAddressEmail(customerAddress.getEmail());
        }

        info.setLines(details.stream().map(this::toDeliverySlipLineInfo).toList());
        return info;
    }

    private DeliverySlipLineInfo toDeliverySlipLineInfo(WarehouseOutboundDetailEntity detail) {
        DeliverySlipLineInfo line = new DeliverySlipLineInfo();
        line.setOutboundDetailId(detail.getId().toString());
        line.setOrderLineId(detail.getOrderLine().getId().toString());
        line.setProductId(detail.getProduct().getId().toString());
        line.setProductCode(detail.getProduct().getCode());
        line.setProductName(detail.getProduct().getName());
        line.setQuantity(detail.getQuantity());
        line.setBox(detail.getBox());
        line.setReferenceCode(detail.getReferenceCode());
        line.setUnitPrice(detail.getUnitPrice());
        line.setVat(detail.getVat());
        line.setCurrency(detail.getCurrency());
        line.setTotalAmount(detail.getTotalAmount());
        line.setTaxAmount(detail.getTaxAmount());
        line.setReceiverNote(detail.getOrderLine().getReceiverNote());
        line.setDeliveryNote(detail.getOrderLine().getDeliveryNote());
        line.setNote(detail.getNote());
        return line;
    }

    private UUID parseUuid(String value, String requestId) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ApiException(
                    ApiException.ErrorCode.BAD_REQUEST,
                    "outboundId must be a valid UUID",
                    HttpStatus.BAD_REQUEST.value(),
                    requestId);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
