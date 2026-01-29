package com.sallahli.controller;

import com.sallahli.dto.sallahli.AddressDTO;
import com.sallahli.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Address Management", description = "APIs for managing addresses")
public class AddressController {

    private final AddressService addressService;

    // ========================================================================
    // Standard CRUD Operations
    // ========================================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all addresses", description = "Returns all non-archived addresses (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<AddressDTO>> findAll() {
        log.debug("REST request to get all addresses");
        return ResponseEntity.ok(addressService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Get address by ID", description = "Returns a single address by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved address"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDTO> findById(
            @Parameter(description = "Address ID") @PathVariable Long id) {
        log.debug("REST request to get address by id: {}", id);
        return ResponseEntity.ok(addressService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Create a new address", description = "Creates a standalone address")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<AddressDTO> create(@RequestBody AddressDTO addressDTO) {
        log.debug("REST request to create address: {}", addressDTO);
        AddressDTO created = addressService.create(addressDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Update an address", description = "Updates an existing address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDTO> update(
            @Parameter(description = "Address ID") @PathVariable Long id,
            @RequestBody AddressDTO addressDTO) {
        log.debug("REST request to update address id {}: {}", id, addressDTO);
        return ResponseEntity.ok(addressService.update(id, addressDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive an address", description = "Soft deletes (archives) an address (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address archived successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Address ID") @PathVariable Long id) {
        log.debug("REST request to archive address id: {}", id);
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Admin Operations
    // ========================================================================

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore an archived address", description = "Restores a previously archived address (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address restored successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDTO> restore(
            @Parameter(description = "Address ID") @PathVariable Long id) {
        log.debug("REST request to restore address id: {}", id);
        return ResponseEntity.ok(addressService.restore(id));
    }

    // ========================================================================
    // Search Operations
    // ========================================================================

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Search addresses", description = "Search addresses by formatted address text")
    public ResponseEntity<List<AddressDTO>> search(
            @Parameter(description = "Search query") @RequestParam String q) {
        log.debug("REST request to search addresses with query: {}", q);
        return ResponseEntity.ok(addressService.searchByFormattedAddress(q));
    }

    @GetMapping("/geo")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Find addresses in bounding box", description = "Find addresses within geographic coordinates")
    public ResponseEntity<List<AddressDTO>> findByBoundingBox(
            @Parameter(description = "Minimum latitude") @RequestParam Double minLat,
            @Parameter(description = "Maximum latitude") @RequestParam Double maxLat,
            @Parameter(description = "Minimum longitude") @RequestParam Double minLon,
            @Parameter(description = "Maximum longitude") @RequestParam Double maxLon) {
        log.debug("REST request to find addresses in bounding box: ({}, {}) to ({}, {})", minLat, minLon, maxLat,
                maxLon);
        return ResponseEntity.ok(addressService.findByBoundingBox(minLat, maxLat, minLon, maxLon));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Batch lookup addresses", description = "Get multiple addresses by their IDs")
    public ResponseEntity<List<AddressDTO>> findByIds(@RequestBody List<Long> ids) {
        log.debug("REST request to batch lookup addresses with ids: {}", ids);
        return ResponseEntity.ok(addressService.findByIds(ids));
    }
}
