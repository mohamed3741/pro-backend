package com.sallahli.repository;

import com.sallahli.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Category> findAllByArchivedFalseOrderByNameAsc();

    Optional<Category> findByIdAndArchivedFalse(Long id);
}
