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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Management", description = "APIs for managing clients")
public class ClientController {

    private final ClientService clientService;



    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all clients", description = "Returns all clients (Admin only)")
    public ResponseEntity<List<ClientDTO>> findAll() {

        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get client by ID", description = "Returns a single client profile")
    public ResponseEntity<ClientDTO> findById(@PathVariable Long id) {

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

        ClientDTO created = clientService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Update a client", description = "Updates client profile")
    public ResponseEntity<ClientDTO> update(@PathVariable Long id, @RequestBody ClientDTO dto) {

        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive a client", description = "Soft deletes a client (Admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }



    @PostMapping("/signup")
    @Operation(summary = "Client Signup", description = "Register a new client account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate telephone")
    })
    public ResponseEntity<ClientDTO> signup(@RequestBody ClientDTO dto) {

        ClientDTO created = clientService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get my profile", description = "Returns the current client's own profile")
    public ResponseEntity<ClientDTO> getMyProfile(Authentication authentication) {
        String username = authentication.getName();

        return ResponseEntity.ok(clientService.findByUsername(username));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update my profile", description = "Client updates their own profile information")
    public ResponseEntity<ClientDTO> updateMyProfile(
            Authentication authentication,
            @RequestBody ClientDTO dto) {
        String username = authentication.getName();

        ClientDTO client = clientService.findByUsername(username);
        return ResponseEntity.ok(clientService.updateProfile(client.getId(), dto));
    }



    @GetMapping("/by-tel/{tel}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Find client by telephone", description = "Returns client by phone number")
    public ResponseEntity<ClientDTO> findByTel(@PathVariable String tel) {

        return ResponseEntity.ok(clientService.findByTel(tel));
    }

    @GetMapping("/by-customer-id/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find client by customer ID", description = "Returns client by Keycloak customer ID")
    public ResponseEntity<ClientDTO> findByCustomerId(@PathVariable String customerId) {

        return ResponseEntity.ok(clientService.findByCustomerId(customerId));
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find client by username", description = "Returns client by username")
    public ResponseEntity<ClientDTO> findByUsername(@PathVariable String username) {

        return ResponseEntity.ok(clientService.findByUsername(username));
    }

    @GetMapping("/exists/{tel}")
    @Operation(summary = "Check if client exists", description = "Returns whether a client with the given telephone exists")
    public ResponseEntity<Boolean> existsByTel(@PathVariable String tel) {

        return ResponseEntity.ok(clientService.existsByTel(tel));
    }
}
