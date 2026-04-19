package com.cnh.ies.mapper.purchaseorder;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.mapper.product.ProductMapper;
import com.cnh.ies.mapper.vendors.VendorsMapper;
import com.cnh.ies.model.purchaseorder.CreatePurchaseOrderLineRequest;
import com.cnh.ies.model.purchaseorder.PurchaseOrderLineInfo;
import com.cnh.ies.model.purchaseorder.UpdatePurchaseOrderLineRequest;
import com.cnh.ies.util.RequestContext;

@Component
public class PurchaseOrderLineMapper {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private VendorsMapper vendorMapper;

    public PurchaseOrderLineInfo toPurchaseOrderLineInfo(PurchaseOrderLineEntity e) {
        PurchaseOrderLineInfo info = new PurchaseOrderLineInfo();
        info.setId(e.getId() != null ? e.getId().toString() : null);
        info.setPurchaseOrderId(e.getPurchaseOrder() != null ? e.getPurchaseOrder().getId().toString() : null);
        info.setSaleOrderLineId(e.getSaleOrderLine() != null ? e.getSaleOrderLine().getId().toString() : null);
        info.setProductId(e.getProduct() != null ? e.getProduct().getId().toString() : null);
        info.setProductName(e.getProduct() != null ? e.getProduct().getName() : null);
        info.setProduct(e.getProduct() != null ? productMapper.toProductInfo(e.getProduct()) : null);

        boolean hasVendor = e.getVendor() != null;
        info.setVendorId(hasVendor ? e.getVendor().getId().toString() : null);
        info.setVendorName(hasVendor ? e.getVendor().getName() : null);
        info.setVendor(hasVendor ? vendorMapper.toVendorInfo(e.getVendor()) : null);

        info.setLink(e.getLink());
        info.setQuantity(e.getQuantity());
        info.setUom1(e.getUom1());
        info.setUom2(e.getUom2());
        info.setUnitPrice(e.getUnitPrice());
        info.setIsTaxIncluded(e.getIsTaxIncluded());
        info.setTax(e.getTax());
        info.setTotalBeforeTax(e.getTotalBeforeTax());
        info.setTotalPrice(e.getTotalPrice());
        info.setCurrency(e.getCurrency());
        info.setExchangeRate(e.getExchangeRate());
        info.setTotalPriceVnd(e.getTotalPriceVnd());
        info.setNote(e.getNote());
        info.setQuote(e.getQuote());
        info.setInvoice(e.getInvoice());
        info.setBillOfLadding(e.getBillOfLadding());
        info.setReceiptWarehouse(e.getReceiptWarehouse());
        info.setTrackId(e.getTrackId());
        info.setPurchaseContractNumber(e.getPurchaseContractNumber());
        info.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        info.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        info.setCreatedBy(e.getCreatedBy());
        info.setUpdatedBy(e.getUpdatedBy());
        return info;
    }

    public PurchaseOrderLineEntity toPurchaseOrderLineEntity(CreatePurchaseOrderLineRequest request, PurchaseOrderEntity purchaseOrder) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.setPurchaseOrder(purchaseOrder);
        entity.setIsDeleted(false);
        entity.setVersion(1L);
        entity.setLink(request.getLink());
        entity.setQuantity(request.getQuantity());
        entity.setUom1(request.getUom1());
        entity.setUom2(request.getUom2());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setIsTaxIncluded(request.getIsTaxIncluded() != null ? request.getIsTaxIncluded() : false);
        entity.setTax(request.getTax());
        entity.setTotalBeforeTax(request.getTotalBeforeTax());
        entity.setTotalPrice(request.getTotalPrice());
        entity.setCurrency(request.getCurrency());
        entity.setExchangeRate(request.getExchangeRate());
        entity.setTotalPriceVnd(request.getTotalPriceVnd());
        entity.setNote(request.getNote());
        entity.setQuote(request.getQuote());
        entity.setInvoice(request.getInvoice());
        entity.setBillOfLadding(request.getBillOfLadding());
        entity.setReceiptWarehouse(request.getReceiptWarehouse());
        entity.setTrackId(request.getTrackId());
        entity.setPurchaseContractNumber(request.getPurchaseContractNumber());
        entity.setCreatedBy(RequestContext.getCurrentUsername());
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
        return entity;
    }

    public void applyUpdate(UpdatePurchaseOrderLineRequest request, PurchaseOrderLineEntity entity) {
        entity.setLink(request.getLink());
        entity.setQuantity(request.getQuantity());
        entity.setUom1(request.getUom1());
        entity.setUom2(request.getUom2());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setIsTaxIncluded(request.getIsTaxIncluded() != null ? request.getIsTaxIncluded() : false);
        entity.setTax(request.getTax());
        entity.setTotalBeforeTax(request.getTotalBeforeTax());
        entity.setTotalPrice(request.getTotalPrice());
        entity.setCurrency(request.getCurrency());
        entity.setExchangeRate(request.getExchangeRate());
        entity.setTotalPriceVnd(request.getTotalPriceVnd());
        entity.setNote(request.getNote());
        entity.setQuote(request.getQuote());
        entity.setInvoice(request.getInvoice());
        entity.setBillOfLadding(request.getBillOfLadding());
        entity.setReceiptWarehouse(request.getReceiptWarehouse());
        entity.setTrackId(request.getTrackId());
        entity.setPurchaseContractNumber(request.getPurchaseContractNumber());
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
    }

    public List<PurchaseOrderLineInfo> toPurchaseOrderLineInfos(List<PurchaseOrderLineEntity> lines) {
        return lines.stream().map(this::toPurchaseOrderLineInfo).collect(Collectors.toList());
    }
}
