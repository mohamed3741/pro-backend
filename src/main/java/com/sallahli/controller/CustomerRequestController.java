package com.sallahli.controller;

import com.sallahli.dto.sallahli.CustomerRequestDTO;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.service.CustomerRequestService;
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
@RequestMapping("/customer-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Request Management", description = "APIs for managing customer service requests")
public class CustomerRequestController {

    private final CustomerRequestService customerRequestService;

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all customer requests", description = "Returns all customer requests (Admin only)")
    public ResponseEntity<List<CustomerRequestDTO>> findAll() {
        log.debug("REST request to get all customer requests");
        return ResponseEntity.ok(customerRequestService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Get request by ID", description = "Returns a single customer request")
    public ResponseEntity<CustomerRequestDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get customer request {}", id);
        return ResponseEntity.ok(customerRequestService.findById(id));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Create a new request", description = "Creates a new customer service request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<CustomerRequestDTO> create(@RequestBody CustomerRequestDTO dto) {
        log.debug("REST request to create customer request: {}", dto);
        CustomerRequestDTO created = customerRequestService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Update a request", description = "Updates an existing customer request")
    public ResponseEntity<CustomerRequestDTO> update(
            @PathVariable Long id,
            @RequestBody CustomerRequestDTO dto) {
        log.debug("REST request to update customer request {}: {}", id, dto);
        return ResponseEntity.ok(customerRequestService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a request", description = "Deletes a customer request (Admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete customer request {}", id);
        customerRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Request Lifecycle
    // ========================================================================

    @PostMapping("/{id}/broadcast")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Broadcast request", description = "Broadcasts request to nearby available professionals")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request broadcasted successfully"),
            @ApiResponse(responseCode = "400", description = "Request cannot be broadcasted")
    })
    public ResponseEntity<CustomerRequestDTO> broadcastRequest(@PathVariable Long id) {
        log.debug("REST request to broadcast customer request {}", id);
        return ResponseEntity.ok(customerRequestService.broadcastRequest(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Cancel request", description = "Cancels a customer request")
    public ResponseEntity<CustomerRequestDTO> cancelRequest(
            @PathVariable Long id,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        log.debug("REST request to cancel customer request {} with reason: {}", id, reason);
        return ResponseEntity.ok(customerRequestService.cancelRequest(id, reason));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Complete request", description = "Marks request as completed")
    public ResponseEntity<CustomerRequestDTO> completeRequest(@PathVariable Long id) {
        log.debug("REST request to complete customer request {}", id);
        return ResponseEntity.ok(customerRequestService.completeRequest(id));
    }

    // ========================================================================
    // Query Operations
    // ========================================================================

    @GetMapping("/by-client/{clientId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get requests by client", description = "Returns all requests for a specific client")
    public ResponseEntity<List<CustomerRequestDTO>> findByClientId(@PathVariable Long clientId) {
        log.debug("REST request to get customer requests for client {}", clientId);
        return ResponseEntity.ok(customerRequestService.findByClientId(clientId));
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get requests by status", description = "Returns all requests with a specific status")
    public ResponseEntity<List<CustomerRequestDTO>> findByStatus(@PathVariable RequestStatus status) {
        log.debug("REST request to get customer requests with status {}", status);
        return ResponseEntity.ok(customerRequestService.findByStatus(status));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get active requests", description = "Returns all broadcasted and not expired requests")
    public ResponseEntity<List<CustomerRequestDTO>> findActiveBroadcastedRequests() {
        log.debug("REST request to get active broadcasted requests");
        return ResponseEntity.ok(customerRequestService.findActiveBroadcastedRequests());
    }

    @GetMapping("/geo")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Find requests in area", description = "Returns requests within geographic bounding box")
    public ResponseEntity<List<CustomerRequestDTO>> findRequestsInBoundingBox(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng) {
        log.debug("REST request to find requests in bounding box");
        return ResponseEntity.ok(customerRequestService.findRequestsInBoundingBox(minLat, maxLat, minLng, maxLng));
    }
}
