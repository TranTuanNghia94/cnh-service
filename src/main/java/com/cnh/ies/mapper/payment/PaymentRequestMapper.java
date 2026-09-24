package com.cnh.ies.mapper.payment;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.payment.PaymentRequestApprovalEntity;
import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestExtraFeeEntity;
import com.cnh.ies.entity.payment.PaymentRequestItemDocumentEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.mapper.purchaseorder.PurchaseOrderLineMapper;
import com.cnh.ies.mapper.purchaseorder.PurchaseOrderMapper;
import com.cnh.ies.repository.payment.PaymentRequestExtraFeeRepo;
import com.cnh.ies.repository.payment.PaymentRequestPurchaseOrderLineRepo;
import com.cnh.ies.repository.warehouse.WarehouseInboundReceiptLineRepo;
import com.cnh.ies.model.payment.CreateOrUpdatePaymentRequest;
import com.cnh.ies.model.payment.PaymentBankInfoObject;
import com.cnh.ies.model.payment.PaymentBankNoteObject;
import com.cnh.ies.model.payment.PaymentFileObject;
import com.cnh.ies.model.payment.PaymentRequestApprovalInfo;
import com.cnh.ies.model.payment.PaymentRequestFeeInfo;
import com.cnh.ies.model.payment.PaymentRequestInfo;
import com.cnh.ies.model.payment.PaymentRequestLineInfo;
import com.cnh.ies.model.purchaseorder.PurchaseOrderInfo;
import com.cnh.ies.util.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRequestMapper {

    private final ObjectMapper objectMapper;
    private final PaymentRequestMoneyMapper moneyMapper;
    private final PaymentRequestPurchaseOrderLineRepo paymentRequestPurchaseOrderLineRepo;
    private final PaymentRequestExtraFeeRepo paymentRequestExtraFeeRepo;
    private final PurchaseOrderLineMapper purchaseOrderLineMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final WarehouseInboundReceiptLineRepo warehouseInboundReceiptLineRepo;

    private static final java.util.List<String> DOCUMENT_TYPES = java.util.List.of(
            "quote", "invoice", "billOfLadding", "receiptWarehouse", "trackId");
    private static final java.util.Map<String, String> DOCUMENT_LABELS = java.util.Map.of(
            "quote", "Quote",
            "invoice", "Invoice",
            "billOfLadding", "Bill of lading",
            "receiptWarehouse", "Receipt warehouse",
            "trackId", "Track ID");

    public void applyHeaderFromCreateOrUpdate(PaymentRequestEntity entity, CreateOrUpdatePaymentRequest request,
            UserEntity requestor, VendorsEntity vendor, BigDecimal requestedAmount, BigDecimal feeAmount,
            BigDecimal lineItemsAmount,
            String papersJson, String bankInfoJson, String requestId) {
        entity.setRequestor(requestor);
        entity.setVendor(vendor);
        String normalizedCurrency = moneyMapper.normalizeCurrency(request.getCurrency());
        BigDecimal exchangeRate = moneyMapper.resolveExchangeRate(normalizedCurrency, request.getExchangeRate(), requestId);
        entity.setCurrency(normalizedCurrency);
        entity.setExchangeRate(exchangeRate);
        BigDecimal requestedAmountVnd = moneyMapper.toVnd(requestedAmount, exchangeRate);
        BigDecimal feeAmountVnd = moneyMapper.toVnd(feeAmount, exchangeRate);
        entity.setPaidPercentage(request.getPaidPercentage() == null ? BigDecimal.valueOf(100) : request.getPaidPercentage());
        entity.setPurpose(request.getPurpose());
        entity.setNotes(request.getNotes());
        entity.setPapers(papersJson);
        entity.setBankInfo(bankInfoJson);
        entity.setApprovalLevels(request.getApprovalLevels());
        entity.setRequestedAmount(requestedAmount);
        entity.setRequestedAmountVnd(requestedAmountVnd);
        entity.setFeeAmount(feeAmount);
        entity.setFeeAmountVnd(feeAmountVnd);
        BigDecimal totalAmount = requestedAmount.add(feeAmount);
        BigDecimal totalAmountVnd = requestedAmountVnd.add(feeAmountVnd);
        entity.setTotalAmount(totalAmount);
        entity.setTotalAmountVnd(totalAmountVnd);
        entity.setAmount(lineItemsAmount == null ? BigDecimal.ZERO : lineItemsAmount);
        entity.setUpdatedBy(RequestContext.getCurrentUsername());
    }

    public PaymentRequestInfo toSummaryInfo(PaymentRequestEntity entity) {
        PaymentRequestInfo info = new PaymentRequestInfo();
        info.setId(entity.getId().toString());
        info.setRequestNumber(entity.getRequestNumber());
        info.setRequestDate(entity.getRequestDate() == null ? null : entity.getRequestDate().toString());
        info.setRequestorId(entity.getRequestor() == null ? null : entity.getRequestor().getId().toString());
        info.setVendorId(entity.getVendor() == null ? null : entity.getVendor().getId().toString());
        info.setStatus(entity.getStatus());
        info.setApprovalLevels(entity.getApprovalLevels());
        info.setCurrentApprovalLevel(entity.getCurrentApprovalLevel());
        info.setCurrency(entity.getCurrency());
        info.setExchangeRate(entity.getExchangeRate());
        info.setAmount(entity.getAmount());
        info.setRequestedAmount(entity.getRequestedAmount());
        info.setRequestedAmountVnd(entity.getRequestedAmountVnd());
        info.setFeeAmount(entity.getFeeAmount());
        info.setFeeAmountVnd(entity.getFeeAmountVnd());
        info.setTotalAmount(entity.getTotalAmount());
        info.setTotalAmountVnd(entity.getTotalAmountVnd());
        info.setPaidAmount(entity.getPaidAmount());
        info.setPaidPercentage(entity.getPaidPercentage());
        info.setPaidAmountVnd(entity.getPaidAmountVnd());
        info.setPurpose(entity.getPurpose());
        info.setNotes(entity.getNotes());
        info.setCreatedBy(entity.getCreatedBy());
        return info;
    }

    /**
     * List rows: align header amounts with persisted lines + fees (same as detail), without loading documents/approvals.
     */
    public PaymentRequestInfo toSummaryInfoWithAlignedAmounts(PaymentRequestEntity entity, String requestId) {
        PaymentRequestInfo info = toSummaryInfo(entity);
        UUID id = entity.getId();
        List<PaymentRequestPurchaseOrderLineEntity> items = paymentRequestPurchaseOrderLineRepo.findByPaymentRequestId(id);
        PaymentRequestMoneyTotals totals = moneyMapper.computeMoneyTotals(
                moneyMapper.sumLineRequestedAmounts(items),
                moneyMapper.sumExtraFeeAmounts(paymentRequestExtraFeeRepo.findByPaymentRequestId(id)),
                entity.getCurrency(),
                entity.getExchangeRate(),
                requestId);
        moneyMapper.applyMoneyTotals(info, totals);
        info.setAmount(moneyMapper.sumLineItemPayableAmounts(items, entity.getCurrency(), entity.getExchangeRate(), requestId));
        return info;
    }

    public PaymentRequestInfo toDetailInfo(PaymentRequestEntity entity,
            List<PaymentRequestPurchaseOrderLineEntity> itemEntities,
            List<PaymentRequestExtraFeeEntity> feeEntities,
            List<PaymentRequestItemDocumentEntity> docEntities,
            List<PaymentRequestApprovalEntity> approvalEntities,
            String requestId) {
        PaymentRequestInfo info = toSummaryInfo(entity);
        info.setPapers(readJson(entity.getPapers(), new TypeReference<List<PaymentFileObject>>() {
        }));
        info.setBankInfo(readJson(entity.getBankInfo(), new TypeReference<PaymentBankInfoObject>() {
        }));
        info.setBankNote(readJson(entity.getBankNote(), new TypeReference<PaymentBankNoteObject>() {
        }));
        info.setPaidBy(entity.getPaidBy() == null ? null : entity.getPaidBy().getId().toString());
        info.setPaidAt(entity.getPaidAt() == null ? null : entity.getPaidAt().toString());

        List<PaymentRequestLineInfo> lines = itemEntities.stream().map(item -> toLineInfo(item, docEntities)).toList();
        attachInboundReceiptNumbers(lines, itemEntities);
        info.setItems(lines);
        info.setFees(feeEntities.stream().map(PaymentRequestMapper::toFeeInfo).toList());

        PaymentRequestMoneyTotals totals = moneyMapper.computeMoneyTotals(
                moneyMapper.sumLineRequestedAmounts(itemEntities),
                moneyMapper.sumExtraFeeAmounts(feeEntities),
                entity.getCurrency(),
                entity.getExchangeRate(),
                requestId);
        moneyMapper.applyMoneyTotals(info, totals);
        info.setAmount(moneyMapper.sumLineItemPayableAmounts(itemEntities, entity.getCurrency(), entity.getExchangeRate(),
                requestId));

        info.setApprovals(approvalEntities.stream().map(PaymentRequestMapper::toApprovalInfo).toList());

        List<PurchaseOrderInfo> purchaseOrders = itemEntities.stream()
                .map(PaymentRequestPurchaseOrderLineEntity::getPurchaseOrderLine)
                .filter(Objects::nonNull)
                .map(PurchaseOrderLineEntity::getPurchaseOrder)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PurchaseOrderEntity::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .map(purchaseOrderMapper::toPurchaseOrderInfo)
                .toList();
        info.setPurchaseOrders(purchaseOrders);

        return info;
    }

    public PaymentRequestLineInfo toLineInfo(PaymentRequestPurchaseOrderLineEntity item,
            List<PaymentRequestItemDocumentEntity> docEntities) {
        PaymentRequestLineInfo lineInfo = new PaymentRequestLineInfo();
        lineInfo.setId(item.getId().toString());
        var pol = item.getPurchaseOrderLine();
        if (pol != null) {
            lineInfo.setPurchaseOrderLineId(pol.getId().toString());
            if (pol.getPurchaseOrder() != null) {
                lineInfo.setPurchaseOrderId(pol.getPurchaseOrder().getId().toString());
            }
            lineInfo.setPurchaseOrderLine(purchaseOrderLineMapper.toPurchaseOrderLineInfo(pol));
        }
        String selectedDocuments = docEntities.stream()
                .filter(doc -> doc.getPaymentRequestItem().getId().equals(item.getId()))
                .map(PaymentRequestItemDocumentEntity::getDocumentType)
                .collect(Collectors.joining(","));
        String documents = selectedDocuments.isBlank() ? item.getSelectedDocuments() : selectedDocuments;
        lineInfo.setSelectedDocuments(documents);
        lineInfo.setDocumentLabel(documentLabel(pol, documents));
        lineInfo.setRequestedAmount(item.getRequestedAmount());
        lineInfo.setPaidAmount(item.getPaidAmount());
        lineInfo.setNote(item.getNote());
        return lineInfo;
    }

    private void attachInboundReceiptNumbers(
            List<PaymentRequestLineInfo> lines, List<PaymentRequestPurchaseOrderLineEntity> items) {
        java.util.Map<String, java.util.Set<String>> codesByLine = new java.util.LinkedHashMap<>();
        java.util.Set<String> allCodes = new java.util.LinkedHashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            java.util.Set<String> codes = lookupCodes(items.get(index).getPurchaseOrderLine());
            codesByLine.put(lines.get(index).getId(), codes);
            allCodes.addAll(codes);
        }
        if (allCodes.isEmpty()) {
            return;
        }
        java.util.Map<String, java.util.Set<String>> receiptsByCode = new java.util.HashMap<>();
        for (Object[] row : warehouseInboundReceiptLineRepo.findReceiptNumbersByDocumentCodes(allCodes)) {
            String receiptNumber = row[0] == null ? "" : row[0].toString().trim();
            if (receiptNumber.isEmpty()) {
                continue;
            }
            for (int index = 1; index < row.length; index++) {
                String code = row[index] == null ? "" : row[index].toString().trim();
                if (!code.isEmpty() && allCodes.contains(code)) {
                    receiptsByCode.computeIfAbsent(code, ignored -> new java.util.LinkedHashSet<>()).add(receiptNumber);
                }
            }
        }
        for (PaymentRequestLineInfo line : lines) {
            java.util.Set<String> numbers = new java.util.LinkedHashSet<>();
            for (String code : codesByLine.getOrDefault(line.getId(), java.util.Set.of())) {
                numbers.addAll(receiptsByCode.getOrDefault(code, java.util.Set.of()));
            }
            line.setInboundReceiptNumbers(String.join(", ", numbers));
        }
    }

    private static java.util.Set<String> lookupCodes(PurchaseOrderLineEntity pol) {
        java.util.Set<String> codes = new java.util.LinkedHashSet<>();
        if (pol == null) {
            return codes;
        }
        addCode(codes, pol.getQuote());
        addCode(codes, pol.getInvoice());
        addCode(codes, pol.getBillOfLadding());
        addCode(codes, pol.getReceiptWarehouse());
        addCode(codes, pol.getTrackId());
        addCode(codes, pol.getPurchaseContractNumber());
        if (pol.getPurchaseOrder() != null && pol.getPurchaseOrder().getOrder() != null) {
            addCode(codes, pol.getPurchaseOrder().getOrder().getContractNumber());
        }
        return codes;
    }

    private static void addCode(java.util.Set<String> codes, String value) {
        if (value != null && !value.isBlank()) {
            codes.add(value.trim());
        }
    }

    private static String documentLabel(PurchaseOrderLineEntity line, String selectedDocuments) {
        if (line == null) {
            return "";
        }
        java.util.List<String> selected = java.util.Arrays.stream(selectedDocuments == null ? new String[0] : selectedDocuments.split("[,|\\n]"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.contains(":") ? token.split(":", 2)[0].trim() : token)
                .filter(DOCUMENT_LABELS::containsKey)
                .toList();
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<String> types = selected.isEmpty()
                ? DOCUMENT_TYPES
                : DOCUMENT_TYPES.stream().filter(selected::contains).toList();
        for (String type : types) {
            String code = switch (type) {
                case "quote" -> line.getQuote();
                case "invoice" -> line.getInvoice();
                case "billOfLadding" -> line.getBillOfLadding();
                case "receiptWarehouse" -> line.getReceiptWarehouse();
                case "trackId" -> line.getTrackId();
                default -> "";
            };
            if (code != null && !code.isBlank()) {
                labels.add(code.trim() + " - " + DOCUMENT_LABELS.get(type));
            }
        }
        return String.join(", ", labels);
    }

    public static PaymentRequestFeeInfo toFeeInfo(PaymentRequestExtraFeeEntity fee) {
        PaymentRequestFeeInfo feeInfo = new PaymentRequestFeeInfo();
        feeInfo.setId(fee.getId().toString());
        feeInfo.setFeeName(fee.getFeeName());
        feeInfo.setFeeType(fee.getFeeType());
        feeInfo.setAmount(fee.getAmount());
        feeInfo.setNote(fee.getNote());
        return feeInfo;
    }

    public static PaymentRequestApprovalInfo toApprovalInfo(PaymentRequestApprovalEntity approval) {
        PaymentRequestApprovalInfo approvalInfo = new PaymentRequestApprovalInfo();
        approvalInfo.setId(approval.getId().toString());
        approvalInfo.setLevel(approval.getApprovalLevel());
        approvalInfo.setRole(approval.getApprovalRole());
        approvalInfo.setApproverId(approval.getApprover() == null ? null : approval.getApprover().getId().toString());
        approvalInfo.setStatus(approval.getStatus());
        approvalInfo.setApprovedAt(approval.getApprovedAt() == null ? null : approval.getApprovedAt().toString());
        approvalInfo.setRejectionReason(approval.getRejectionReason());
        approvalInfo.setNote(approval.getNote());
        approvalInfo.setUpdatedBy(approval.getUpdatedBy());
        return approvalInfo;
    }

    private <T> T readJson(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (Exception e) {
            return null;
        }
    }
}
