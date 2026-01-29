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
@RequestMapping("/clients/{clientId}/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Address Management", description = "APIs for managing client addresses")
public class ClientAddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PRO', 'ADMIN')")
    @Operation(summary = "Get client addresses", description = "Returns all addresses associated with a client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<List<AddressDTO>> getClientAddresses(
            @Parameter(description = "Client ID") @PathVariable Long clientId) {
        log.debug("REST request to get addresses for client: {}", clientId);
        return ResponseEntity.ok(addressService.findByClientId(clientId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Add address to client", description = "Creates a new address and links it to the client")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created and linked successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<AddressDTO> addAddressToClient(
            @Parameter(description = "Client ID") @PathVariable Long clientId,
            @RequestBody AddressDTO addressDTO) {
        log.debug("REST request to add address to client {}: {}", clientId, addressDTO);
        AddressDTO created = addressService.addAddressToClient(clientId, addressDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{addressId}/link")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Link existing address to client", description = "Links an existing address to a client")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address linked successfully"),
            @ApiResponse(responseCode = "404", description = "Client or address not found")
    })
    public ResponseEntity<Void> linkAddressToClient(
            @Parameter(description = "Client ID") @PathVariable Long clientId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        log.debug("REST request to link address {} to client {}", addressId, clientId);
        addressService.linkAddressToClient(clientId, addressId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Remove address from client", description = "Unlinks an address from a client (does not delete the address)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address unlinked successfully"),
            @ApiResponse(responseCode = "404", description = "Client or address link not found")
    })
    public ResponseEntity<Void> removeAddressFromClient(
            @Parameter(description = "Client ID") @PathVariable Long clientId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        log.debug("REST request to remove address {} from client {}", addressId, clientId);
        addressService.removeAddressFromClient(clientId, addressId);
        return ResponseEntity.noContent().build();
    }
}
