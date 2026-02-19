package com.sallahli.service;

import com.sallahli.dto.MediaDTO;
import com.sallahli.dto.sallahli.CategoryDTO;
import com.sallahli.exceptions.NotFoundException;
import com.sallahli.mapper.CategoryMapper;
import com.sallahli.mapper.MediaMapper;
import com.sallahli.model.Category;
import com.sallahli.model.Enum.MediaEnum;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.model.Media;
import com.sallahli.repository.CategoryRepository;
import com.sallahli.repository.MediaRepository;
import com.sallahli.service.crud.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CategoryService extends AbstractCrudService<Category, CategoryDTO> {

    private final CategoryRepository categoryRepository;
    private final MediaRepository mediaRepository;
    private final CategoryMapper categoryMapper;
    private final MediaService mediaService;
    private final MediaMapper mediaMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
            MediaRepository mediaRepository, MediaService mediaService, MediaMapper mediaMapper) {
        super(categoryRepository, categoryMapper);
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.mediaRepository = mediaRepository;
        this.mediaService = mediaService;
        this.mediaMapper = mediaMapper;
    }

    // ========================================================================
    // Core CRUD operations
    // ========================================================================

    
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

    
    @Transactional(readOnly = true)
    public List<CategoryDTO> findByWorkflowType(WorkflowType workflowType) {
        List<Category> categories = categoryRepository.findByWorkflowTypeAndArchivedFalse(workflowType);
        return categoryMapper.toDtos(categories);
    }

    
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllActive() {
        List<Category> categories = categoryRepository.findByActiveAndArchivedFalse(true);
        return categoryMapper.toDtos(categories);
    }

    
    @Transactional(readOnly = true)
    public CategoryDTO findByCode(String code) {
        Category category = categoryRepository.findByCodeIgnoreCaseAndArchivedFalse(code)
                .orElseThrow(() -> new NotFoundException("Category not found with code: " + code));
        return categoryMapper.toDto(category);
    }

    // ========================================================================
    // Relationship handling
    // ========================================================================

    
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

    // ========================================================================
    // Icon Management
    // ========================================================================

    @Transactional
    public ResponseEntity<String> updateCategoryIcon(Long categoryId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be null or empty.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                return ResponseEntity.badRequest().body("File data cannot be empty.");
            }

            Category category = categoryRepository.findByIdAndArchivedFalse(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));

            Long mediaId = (category.getIconMedia() != null && category.getIconMedia().getId() != null)
                    ? category.getIconMedia().getId()
                    : null;

            MediaDTO iconMedia = mediaService.createDto(fileBytes, MediaEnum.CATEGORY, mediaId);
            Media media = mediaMapper.toModel(iconMedia);

            category.setIconMedia(media);
            categoryRepository.save(category);

            log.info("Updated icon for category {}", categoryId);
            return ResponseEntity.ok("Category icon updated successfully.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating category icon: " + e.getMessage());
        }
    }
}
