package com.cnh.ies.service.payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.payment.PaymentRequestEntity;
import com.cnh.ies.entity.payment.PaymentRequestItemDocumentEntity;
import com.cnh.ies.entity.payment.PaymentRequestPurchaseOrderLineEntity;
import com.cnh.ies.entity.purchaseorder.PurchaseOrderLineEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.payment.PaymentRequestItemRequest;
import com.cnh.ies.repository.payment.PaymentRequestItemDocumentRepo;
import com.cnh.ies.repository.payment.PaymentRequestPurchaseOrderLineRepo;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestLineSyncService {

    private final PaymentRequestPurchaseOrderLineRepo paymentRequestPurchaseOrderLineRepo;
    private final PaymentRequestItemDocumentRepo paymentRequestItemDocumentRepo;

    public void syncItems(PaymentRequestEntity paymentRequest, List<PaymentRequestItemRequest> itemRequests,
            List<PurchaseOrderLineEntity> poLines, String requestId) {
        List<PaymentRequestPurchaseOrderLineEntity> existing = paymentRequestPurchaseOrderLineRepo
                .findByPaymentRequestId(paymentRequest.getId());
        Map<UUID, PaymentRequestPurchaseOrderLineEntity> byPoLine = new HashMap<>();
        for (PaymentRequestPurchaseOrderLineEntity line : existing) {
            byPoLine.put(line.getPurchaseOrderLine().getId(), line);
        }

        Set<UUID> newPoLineIds = itemRequests.stream()
                .map(i -> UUID.fromString(i.getPurchaseOrderLineId()))
                .collect(Collectors.toSet());
        for (PaymentRequestPurchaseOrderLineEntity line : existing) {
            if (!newPoLineIds.contains(line.getPurchaseOrderLine().getId())) {
                removeItemAndDocuments(line);
                byPoLine.remove(line.getPurchaseOrderLine().getId());
            }
        }

        Map<UUID, PurchaseOrderLineEntity> linesById = poLines.stream()
                .collect(Collectors.toMap(PurchaseOrderLineEntity::getId, l -> l));
        String user = RequestContext.getCurrentUsername();
        for (PaymentRequestItemRequest itemRequest : itemRequests) {
            UUID poLineId = UUID.fromString(itemRequest.getPurchaseOrderLineId());
            PurchaseOrderLineEntity line = linesById.get(poLineId);
            if (line == null) {
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Purchase order line not in request",
                        HttpStatus.BAD_REQUEST.value(), requestId);
            }
            PaymentRequestPurchaseOrderLineEntity item = byPoLine.get(poLineId);
            if (item != null) {
                item.setSelectedDocuments(normalizeDocuments(itemRequest.getSelectedDocumentTypes()));
                item.setRequestedAmount(itemRequest.getRequestedAmount());
                item.setNote(itemRequest.getNote());
                item.setUpdatedBy(user);
                paymentRequestPurchaseOrderLineRepo.save(item);
                syncItemDocuments(item, itemRequest, user);
            } else {
                PaymentRequestPurchaseOrderLineEntity newItem = new PaymentRequestPurchaseOrderLineEntity();
                newItem.setPaymentRequest(paymentRequest);
                newItem.setPurchaseOrderLine(line);
                newItem.setSelectedDocuments(normalizeDocuments(itemRequest.getSelectedDocumentTypes()));
                newItem.setRequestedAmount(itemRequest.getRequestedAmount());
                newItem.setPaidAmount(BigDecimal.ZERO);
                newItem.setNote(itemRequest.getNote());
                newItem.setCreatedBy(user);
                newItem.setUpdatedBy(user);
                PaymentRequestPurchaseOrderLineEntity savedItem = paymentRequestPurchaseOrderLineRepo.save(newItem);
                persistLineDocuments(savedItem, itemRequest.getSelectedDocumentTypes(), user);
            }
        }
    }

    private void persistLineDocuments(PaymentRequestPurchaseOrderLineEntity lineItem, List<String> selectedDocTypes,
            String user) {
        for (String documentType : normalizeDocumentTypes(selectedDocTypes)) {
            saveItemDocument(lineItem, documentType, user);
        }
    }

    private void saveItemDocument(PaymentRequestPurchaseOrderLineEntity lineItem, String documentType, String user) {
        PaymentRequestItemDocumentEntity doc = new PaymentRequestItemDocumentEntity();
        doc.setPaymentRequestItem(lineItem);
        doc.setDocumentType(documentType);
        doc.setCreatedBy(user);
        doc.setUpdatedBy(user);
        paymentRequestItemDocumentRepo.save(doc);
    }

    private void removeItemAndDocuments(PaymentRequestPurchaseOrderLineEntity item) {
        List<PaymentRequestItemDocumentEntity> existingDocs = paymentRequestItemDocumentRepo
                .findByPaymentRequestItemIds(List.of(item.getId()));
        if (!existingDocs.isEmpty()) {
            String user = RequestContext.getCurrentUsername();
            existingDocs.forEach(doc -> {
                doc.setIsDeleted(true);
                doc.setUpdatedBy(user);
            });
            paymentRequestItemDocumentRepo.saveAll(existingDocs);
        }
        item.setIsDeleted(true);
        item.setUpdatedBy(RequestContext.getCurrentUsername());
        paymentRequestPurchaseOrderLineRepo.save(item);
    }

    private void syncItemDocuments(PaymentRequestPurchaseOrderLineEntity item, PaymentRequestItemRequest itemRequest,
            String user) {
        Set<String> newTypes = normalizeDocumentTypes(itemRequest.getSelectedDocumentTypes());
        List<PaymentRequestItemDocumentEntity> docs = paymentRequestItemDocumentRepo
                .findByPaymentRequestItemIds(List.of(item.getId()));
        Set<String> stillActive = new HashSet<>();
        for (PaymentRequestItemDocumentEntity doc : docs) {
            String t = doc.getDocumentType() == null ? "" : doc.getDocumentType().toLowerCase();
            if (!newTypes.contains(t)) {
                doc.setIsDeleted(true);
                doc.setUpdatedBy(user);
            } else {
                stillActive.add(t);
            }
        }
        if (!docs.isEmpty()) {
            paymentRequestItemDocumentRepo.saveAll(docs);
        }
        for (String documentType : newTypes) {
            if (!stillActive.contains(documentType)) {
                saveItemDocument(item, documentType, user);
            }
        }
    }

    private String normalizeDocuments(List<String> selectedDocumentTypes) {
        return String.join(",", normalizeDocumentTypes(selectedDocumentTypes));
    }

    private Set<String> normalizeDocumentTypes(List<String> selectedDocumentTypes) {
        return selectedDocumentTypes.stream()
                .filter(it -> it != null && !it.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
