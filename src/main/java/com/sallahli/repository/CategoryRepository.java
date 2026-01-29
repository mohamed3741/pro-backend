package com.sallahli.repository;

import com.sallahli.model.Category;
import com.sallahli.model.Enum.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Category> findAllByArchivedFalseOrderByNameAsc();

    Optional<Category> findByIdAndArchivedFalse(Long id);

    // Workflow type queries
    List<Category> findByWorkflowTypeAndArchivedFalse(WorkflowType workflowType);

    // Active status queries
    List<Category> findByActiveAndArchivedFalse(Boolean active);

    // Code lookup
    Optional<Category> findByCodeIgnoreCaseAndArchivedFalse(String code);
}
