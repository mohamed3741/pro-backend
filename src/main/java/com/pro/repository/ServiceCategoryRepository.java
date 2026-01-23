package com.pro.repository;

import com.pro.model.ServiceCategory;
import com.pro.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends GenericRepository<ServiceCategory> {

    Optional<ServiceCategory> findByCode(String code);

    List<ServiceCategory> findByActiveTrue();

    @Query("SELECT sc FROM ServiceCategory sc WHERE sc.active = true ORDER BY sc.name")
    List<ServiceCategory> findActiveCategoriesOrderedByName();
}
