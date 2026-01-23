package com.pro.controller.sallahli;

import com.pro.dto.sallahli.ServiceCategoryDTO;
import com.pro.service.ServiceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sallahli/categories")
@RequiredArgsConstructor
@Tag(name = "Service Categories", description = "APIs for managing service categories")
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping
    @Operation(summary = "Get all active service categories")
    public ResponseEntity<List<ServiceCategoryDTO>> getActiveCategories() {
        List<ServiceCategoryDTO> categories = serviceCategoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service category by ID")
    public ResponseEntity<ServiceCategoryDTO> getCategory(@PathVariable Long id) {
        ServiceCategoryDTO category = serviceCategoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get service category by code")
    public ResponseEntity<ServiceCategoryDTO> getCategoryByCode(@PathVariable String code) {
        ServiceCategoryDTO category = serviceCategoryService.findByCode(code);
        return ResponseEntity.ok(category);
    }
}
