package com.cnh.ies.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cnh.ies.entity.BaseEntity;

public interface BaseRepo<T extends BaseEntity, ID> extends JpaRepository<T, ID> {
    
}
