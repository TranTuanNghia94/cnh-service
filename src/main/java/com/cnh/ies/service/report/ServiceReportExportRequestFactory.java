package com.cnh.ies.service.report;

import java.util.Map;

import com.cnh.ies.model.export.ServiceReportExportParams;
import com.cnh.ies.model.report.OrderReportRequest;
import com.cnh.ies.model.report.PaymentRequestReportRequest;
import com.cnh.ies.model.report.PurchaseReportRequest;
import com.cnh.ies.model.report.WarehouseInboundReportRequest;
import com.cnh.ies.model.report.WarehouseInventoryReportRequest;
import com.cnh.ies.model.report.WarehouseOutboundReportRequest;

public final class ServiceReportExportRequestFactory {

    private ServiceReportExportRequestFactory() {}

    public static WarehouseInboundReportRequest inbound(ServiceReportExportParams params) {
        WarehouseInboundReportRequest req = new WarehouseInboundReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setCreatedBy(filter(f, "createdBy"));
        req.setInboundNumber(filter(f, "inboundNumber"));
        req.setContractNumber(filter(f, "contractNumber"));
        req.setCustomerName(filter(f, "customerName"));
        req.setOrderNumber(filter(f, "orderNumber"));
        req.setStatus(filter(f, "status"));
        return req;
    }

    public static WarehouseOutboundReportRequest outbound(ServiceReportExportParams params) {
        WarehouseOutboundReportRequest req = new WarehouseOutboundReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setCreatedBy(filter(f, "createdBy"));
        req.setOutboundNumber(filter(f, "outboundNumber"));
        req.setContractNumber(filter(f, "contractNumber"));
        req.setStatus(filter(f, "status"));
        return req;
    }

    public static WarehouseInventoryReportRequest inventory(ServiceReportExportParams params) {
        WarehouseInventoryReportRequest req = new WarehouseInventoryReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setProductCode(filter(f, "productCode"));
        req.setProductName(filter(f, "productName"));
        req.setProductCategory(filter(f, "productCategory"));
        req.setDirection(filter(f, "direction"));
        return req;
    }

    public static OrderReportRequest order(ServiceReportExportParams params) {
        OrderReportRequest req = new OrderReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setCreatedBy(filter(f, "createdBy"));
        req.setContractNumber(filter(f, "contractNumber"));
        req.setOrderNumber(filter(f, "orderNumber"));
        req.setStatus(filter(f, "status"));
        req.setCustomerName(filter(f, "customerName"));
        return req;
    }

    public static PurchaseReportRequest purchase(ServiceReportExportParams params) {
        PurchaseReportRequest req = new PurchaseReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setPurchaseOrderNumber(filter(f, "purchaseOrderNumber"));
        req.setContractNumber(filter(f, "contractNumber"));
        req.setCreatedBy(filter(f, "createdBy"));
        req.setCustomerName(filter(f, "customerName"));
        req.setStatus(filter(f, "status"));
        return req;
    }

    public static PaymentRequestReportRequest payment(ServiceReportExportParams params) {
        PaymentRequestReportRequest req = new PaymentRequestReportRequest();
        applyDates(req, params);
        Map<String, String> f = params.getFilters();
        req.setCreatedBy(filter(f, "createdBy"));
        req.setPaymentRequestNumber(filter(f, "paymentRequestNumber"));
        req.setVendorCode(filter(f, "vendorCode"));
        req.setStatus(filter(f, "status"));
        return req;
    }

    private static void applyDates(
            com.cnh.ies.model.report.ServiceReportPagedRequest req, ServiceReportExportParams params) {
        req.setFromDate(params.getFromDate());
        req.setToDate(params.getToDate());
        req.setPage(0);
        req.setLimit(50_000);
    }

    private static String filter(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        return filters.get(key);
    }
}
