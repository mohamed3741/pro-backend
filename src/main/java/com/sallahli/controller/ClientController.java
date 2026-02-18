package com.sallahli.controller;

import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.service.ClientAuthService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Management", description = "APIs for managing clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientAuthService clientAuthService;

    @GetMapping("/all")
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

    @PostMapping("/create")
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

    @PutMapping("/{id}/update")
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

    @PreAuthorize("hasAuthority('CLIENT')")
    @PutMapping("/update-profile-image")
    public ResponseEntity<String> updateProfileImage(Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        return clientService.updateClientProfileImage(authentication, file);
    }

    @PreAuthorize("hasAuthority('CLIENT')")
    @PutMapping("/update-user")
    public ResponseEntity<ClientDTO> updateProfile(Authentication authentication, @RequestBody ClientDTO clientDTO) {
        try {
            ClientDTO updatedClient = clientService.updateUser(authentication, clientDTO);
            return ResponseEntity.ok(updatedClient);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Returns the current client's own profile")
    public ClientDTO findMe(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        ClientDTO client = clientService.findByUsername(authentication.getName());
        if (client == null) {
            client = clientAuthService.initClientFromToken(authentication);
        }
        return client;
    }

    @PutMapping("/me/profile/update")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Update my profile", description = "Client updates their own profile information")
    public ResponseEntity<ClientDTO> updateMyProfile(
            Authentication authentication,
            @RequestBody ClientDTO dto) {
        String username = authentication.getName();

        ClientDTO client = clientService.findByUsername(username);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
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
        ClientDTO client = clientService.findByUsername(username);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

}
