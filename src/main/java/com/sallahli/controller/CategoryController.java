package com.sallahli.controller;

import com.sallahli.dto.sallahli.CategoryDTO;
import com.sallahli.model.Enum.WorkflowType;
import com.sallahli.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/all")
    @Operation(summary = "Get all categories")
    public ResponseEntity<List<CategoryDTO>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<CategoryDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active categories")
    public ResponseEntity<List<CategoryDTO>> findAllActive() {
        return ResponseEntity.ok(categoryService.findAllActive());
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get category by code")
    public ResponseEntity<CategoryDTO> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(categoryService.findByCode(code));
    }

    @PutMapping("/{id}/workflow-type")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update category workflow type")
    public ResponseEntity<CategoryDTO> updateWorkflowType(@PathVariable Long id,
            @RequestParam WorkflowType workflowType) {
        return ResponseEntity.ok(categoryService.updateWorkflowType(id, workflowType));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create a new category")
    public ResponseEntity<CategoryDTO> create(@RequestBody CategoryDTO categoryDTO) {
        CategoryDTO created = categoryService.create(categoryDTO);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update an existing category")
    public ResponseEntity<CategoryDTO> update(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO updated = categoryService.update(id, categoryDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete category")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
