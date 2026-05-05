package com.cnh.ies.repository.product;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.product.ProductTaxHistoryEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ProductTaxHistoryRepo extends BaseRepo<ProductTaxHistoryEntity, UUID> {
}
