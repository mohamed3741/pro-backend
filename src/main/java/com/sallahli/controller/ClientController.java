package com.sallahli.controller;

import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Management", description = "APIs for managing clients")
public class ClientController {

    private final ClientService clientService;

    // ========================================================================
    // Admin CRUD Operations
    // ========================================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all clients", description = "Returns all clients (Admin only)")
    public ResponseEntity<List<ClientDTO>> findAll() {
        log.debug("REST request to get all clients");
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get client by ID", description = "Returns a single client profile")
    public ResponseEntity<ClientDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get client {}", id);
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a client", description = "Registers a new client (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO dto) {
        log.debug("REST request to create client: {}", dto);
        ClientDTO created = clientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Update a client", description = "Updates client profile")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @RequestBody ClientDTO dto) {
        log.debug("REST request to update client {}: {}", id, dto);
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive a client", description = "Soft deletes a client (Admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to archive client {}", id);
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // Self-Service Endpoints (for Client user)
    // ========================================================================

    @PostMapping("/signup")
    @Operation(summary = "Client Signup", description = "Register a new client account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate telephone")
    })
    public ResponseEntity<ClientDTO> signup(@RequestBody ClientDTO dto) {
        log.debug("REST request for client signup: {}", dto.getTel());
        ClientDTO created = clientService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get my profile", description = "Returns the current client's own profile")
    public ResponseEntity<ClientDTO> getMyProfile(@RequestParam Long clientId) {
        log.debug("REST request to get own profile for client {}", clientId);
        return ResponseEntity.ok(clientService.getMyProfile(clientId));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update my profile", description = "Client updates their own profile information")
    public ResponseEntity<ClientDTO> updateMyProfile(
            @RequestParam Long clientId,
            @RequestBody ClientDTO dto) {
        log.debug("REST request for client {} to update their profile", clientId);
        return ResponseEntity.ok(clientService.updateProfile(clientId, dto));
    }

    // ========================================================================
    // Lookup Operations
    // ========================================================================

    @GetMapping("/by-tel/{tel}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Find client by telephone", description = "Returns client by phone number")
    public ResponseEntity<ClientDTO> findByTel(@PathVariable String tel) {
        log.debug("REST request to find client by tel: {}", tel);
        return ResponseEntity.ok(clientService.findByTel(tel));
    }

    @GetMapping("/by-customer-id/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find client by customer ID", description = "Returns client by Keycloak customer ID")
    public ResponseEntity<ClientDTO> findByCustomerId(@PathVariable String customerId) {
        log.debug("REST request to find client by customerId: {}", customerId);
        return ResponseEntity.ok(clientService.findByCustomerId(customerId));
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find client by username", description = "Returns client by username")
    public ResponseEntity<ClientDTO> findByUsername(@PathVariable String username) {
        log.debug("REST request to find client by username: {}", username);
        return ResponseEntity.ok(clientService.findByUsername(username));
    }

    @GetMapping("/exists/{tel}")
    @Operation(summary = "Check if client exists", description = "Returns whether a client with the given telephone exists")
    public ResponseEntity<Boolean> existsByTel(@PathVariable String tel) {
        log.debug("REST request to check if client exists by tel: {}", tel);
        return ResponseEntity.ok(clientService.existsByTel(tel));
    }
}
