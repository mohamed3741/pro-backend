package com.sallahli.service;

import com.sallahli.dto.sallahli.CategoryDTO;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.CategoryMapper;
import com.sallahli.model.Category;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.model.Media;
import com.sallahli.repository.CategoryRepository;
import com.sallahli.repository.MediaRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CategoryService extends AbstractCrudService<Category, CategoryDTO> {

    private final CategoryRepository categoryRepository;
    private final MediaRepository mediaRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
            MediaRepository mediaRepository) {
        super(categoryRepository, categoryMapper);
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.mediaRepository = mediaRepository;
    }

    // ========================================================================
    // Core CRUD operations
    // ========================================================================

    /**
     * Best practice: return only non-archived categories.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return categoryMapper.toDtos(categoryRepository.findAllByArchivedFalseOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        return categoryMapper.toDto(category);
    }

    // ========================================================================
    // Workflow Type Methods
    // ========================================================================

    /**
     * Update the workflow type for a category.
     * This determines whether the category uses LEAD_OFFER or FIRST_CLICK mode.
     *
     * @param categoryId   The category ID
     * @param workflowType The new workflow type (LEAD_OFFER or FIRST_CLICK)
     * @return Updated category DTO
     */
    @Transactional
    public CategoryDTO updateWorkflowType(Long categoryId, WorkflowType workflowType) {
        Category category = categoryRepository.findByIdAndArchivedFalse(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));

        WorkflowType previousType = category.getWorkflowType();
        category.setWorkflowType(workflowType);
        Category saved = categoryRepository.save(category);

        log.info("Updated category {} workflow type from {} to {}", categoryId, previousType, workflowType);

        return categoryMapper.toDto(saved);
    }

    /**
     * Find categories by workflow type.
     *
     * @param workflowType The workflow type to filter by
     * @return List of categories with the specified workflow type
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> findByWorkflowType(WorkflowType workflowType) {
        List<Category> categories = categoryRepository.findByWorkflowTypeAndArchivedFalse(workflowType);
        return categoryMapper.toDtos(categories);
    }

    /**
     * Find all active categories (non-archived and active=true).
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllActive() {
        List<Category> categories = categoryRepository.findByActiveAndArchivedFalse(true);
        return categoryMapper.toDtos(categories);
    }

    /**
     * Find category by code.
     */
    @Transactional(readOnly = true)
    public CategoryDTO findByCode(String code) {
        Category category = categoryRepository.findByCodeIgnoreCaseAndArchivedFalse(code)
                .orElseThrow(() -> new NotFoundException("Category not found with code: " + code));
        return categoryMapper.toDto(category);
    }

    // ========================================================================
    // Relationship handling
    // ========================================================================

    /**
     * Resolve relationships (iconMedia) here, not in mapper.
     */
    @Override
    protected void applyRelationships(Category entity, CategoryDTO dto) {
        if (dto == null)
            return;

        if (dto.getIconMedia() != null && dto.getIconMedia().getId() != null) {
            Long mediaId = dto.getIconMedia().getId();
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new NotFoundException("Media not found with id: " + mediaId));
            entity.setIconMedia(media);
        } else {
            entity.setIconMedia(null);
        }
    }

    // ========================================================================
    // Validation hooks
    // ========================================================================

    @Override
    protected void beforePersist(Category entity, CategoryDTO dto, boolean isNew) {
        // normalize code
        entity.setCode(normalizeCode(entity.getCode()));

        if (!StringUtils.hasText(entity.getCode())) {
            throw new IllegalArgumentException("Category code is required");
        }
        if (!StringUtils.hasText(entity.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }

        // leadCost validation
        if (entity.getLeadCost() == null) {
            entity.setLeadCost(BigDecimal.valueOf(50));
        }
        if (entity.getLeadCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("leadCost must be >= 0");
        }

        // matchLimit validation
        if (entity.getMatchLimit() == null) {
            entity.setMatchLimit(3);
        }
        if (entity.getMatchLimit() <= 0) {
            throw new IllegalArgumentException("matchLimit must be > 0");
        }

        // workflowType default
        if (entity.getWorkflowType() == null) {
            entity.setWorkflowType(WorkflowType.LEAD_OFFER);
        }

        // active default
        if (entity.getActive() == null) {
            entity.setActive(true);
        }

        // archived default (if field exists in DB)
        if (entity.getArchived() == null) {
            entity.setArchived(false);
        }

        // uniqueness check for code
        if (isNew) {
            if (categoryRepository.existsByCodeIgnoreCase(entity.getCode())) {
                throw new IllegalArgumentException("Category code already exists: " + entity.getCode());
            }
        } else {
            if (entity.getId() != null
                    && categoryRepository.existsByCodeIgnoreCaseAndIdNot(entity.getCode(), entity.getId())) {
                throw new IllegalArgumentException("Category code already exists: " + entity.getCode());
            }
        }
    }

    /**
     * Best practice: don't hard delete categories because other tables reference
     * them.
     * Instead, archive + deactivate.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        category.setArchived(true);
        category.setActive(false);

        categoryRepository.save(category);
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code))
            return null;
        return code.trim().toUpperCase();
    }
}
