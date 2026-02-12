package com.sallahli.controller;

import com.sallahli.dto.sallahli.AdminDTO;
import com.sallahli.model.Enum.AdminRole;
import com.sallahli.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Back-office admin management APIs")
public class AdminController {

    private final AdminService adminService;

    // ========================================================================
    // Self-service (authenticated admin)
    // ========================================================================

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'AGENT', 'CUSTOMER_SUPPORT_AGENT', 'LOGISTICS_COORDINATOR', 'ACCOUNTANT')")
    @Operation(summary = "Get current admin profile from auth context")
    public ResponseEntity<AdminDTO> getMyProfile(Authentication authentication) {
        AdminDTO admin = adminService.findByUsername(authentication.getName());
        return ResponseEntity.ok(admin);
    }

    // ========================================================================
    // CRUD (SUPER_ADMIN only)
    // ========================================================================

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "List all active admins")
    public ResponseEntity<List<AdminDTO>> findAll() {
        return ResponseEntity.ok(adminService.findActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Get admin by ID")
    public ResponseEntity<AdminDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update admin (SUPER_ADMIN only)")
    public ResponseEntity<AdminDTO> update(
            @PathVariable Long id,
            @RequestBody AdminDTO adminDTO) {
        return ResponseEntity.ok(adminService.update(id, adminDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Archive admin (soft delete, SUPER_ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Search & Filter
    // ========================================================================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Search admins by name, username, email, or phone")
    public ResponseEntity<List<AdminDTO>> search(
            @Parameter(description = "Search query") @RequestParam String q) {
        return ResponseEntity.ok(adminService.searchAdmins(q));
    }

    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Filter admins by role")
    public ResponseEntity<List<AdminDTO>> findByRole(
            @Parameter(description = "Admin role") @PathVariable AdminRole role) {
        return ResponseEntity.ok(adminService.findByRole(role));
    }

    // ========================================================================
    // Archive Management
    // ========================================================================

    @GetMapping("/archived")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List archived admins")
    public ResponseEntity<List<AdminDTO>> findArchived() {
        return ResponseEntity.ok(adminService.findArchived());
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Restore an archived admin")
    public ResponseEntity<AdminDTO> restore(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.restoreAccount(id));
    }

    // ========================================================================
    // Activate / Deactivate
    // ========================================================================

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate admin account")
    public ResponseEntity<AdminDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.activateAccount(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate admin account")
    public ResponseEntity<AdminDTO> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateAccount(id));
    }
}
