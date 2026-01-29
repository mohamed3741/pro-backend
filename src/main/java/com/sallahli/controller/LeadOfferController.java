package com.sallahli.controller;

import com.sallahli.dto.sallahli.LeadOfferDTO;
import com.sallahli.service.LeadOfferService;
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
@RequestMapping("/lead-offers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lead Offer Management", description = "APIs for managing lead offers to professionals")
public class LeadOfferController {

    private final LeadOfferService leadOfferService;

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get lead offer by ID", description = "Returns a single lead offer")
    public ResponseEntity<LeadOfferDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get lead offer {}", id);
        return ResponseEntity.ok(leadOfferService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a lead offer", description = "Creates a lead offer for a specific pro and request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or offer exists")
    })
    public ResponseEntity<LeadOfferDTO> createOffer(
            @Parameter(description = "Request ID") @RequestParam Long requestId,
            @Parameter(description = "Pro ID") @RequestParam Long proId) {
        log.debug("REST request to create lead offer for request {} and pro {}", requestId, proId);
        LeadOfferDTO created = leadOfferService.createOffer(requestId, proId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========================================================================
    // Lead Offer Actions
    // ========================================================================

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Accept lead offer", description = "Pro accepts the lead offer, deducting wallet balance and creating a job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offer accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot accept offer (expired, already accepted, insufficient balance)")
    })
    public ResponseEntity<LeadOfferDTO> acceptOffer(@PathVariable Long id) {
        log.debug("REST request to accept lead offer {}", id);
        return ResponseEntity.ok(leadOfferService.acceptOffer(id));
    }

    @PostMapping("/{id}/miss")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Miss lead offer", description = "Pro missed/rejected the lead offer")
    public ResponseEntity<LeadOfferDTO> missOffer(@PathVariable Long id) {
        log.debug("REST request to miss lead offer {}", id);
        return ResponseEntity.ok(leadOfferService.missOffer(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel lead offer", description = "Admin cancels a lead offer")
    public ResponseEntity<LeadOfferDTO> cancelOffer(@PathVariable Long id) {
        log.debug("REST request to cancel lead offer {}", id);
        return ResponseEntity.ok(leadOfferService.cancelOffer(id));
    }

    // ========================================================================
    // Query Operations
    // ========================================================================

    @GetMapping("/by-request/{requestId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @Operation(summary = "Get offers for request", description = "Returns all lead offers for a specific request")
    public ResponseEntity<List<LeadOfferDTO>> findByRequestId(@PathVariable Long requestId) {
        log.debug("REST request to get lead offers for request {}", requestId);
        return ResponseEntity.ok(leadOfferService.findByRequestId(requestId));
    }

    @GetMapping("/by-pro/{proId}/pending")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get pending offers for pro", description = "Returns all pending lead offers for a specific pro")
    public ResponseEntity<List<LeadOfferDTO>> findPendingOffersByProId(@PathVariable Long proId) {
        log.debug("REST request to get pending lead offers for pro {}", proId);
        return ResponseEntity.ok(leadOfferService.findPendingOffersByProId(proId));
    }

    @GetMapping("/by-pro/{proId}/accepted")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get accepted offers for pro", description = "Returns all accepted lead offers for a specific pro")
    public ResponseEntity<List<LeadOfferDTO>> findAcceptedOffersByProId(@PathVariable Long proId) {
        log.debug("REST request to get accepted lead offers for pro {}", proId);
        return ResponseEntity.ok(leadOfferService.findAcceptedOffersByProId(proId));
    }

    // ========================================================================
    // Admin Operations
    // ========================================================================

    @PostMapping("/expire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Expire old offers", description = "Expires all offers that have passed their expiration time")
    public ResponseEntity<Integer> expireOldOffers() {
        log.debug("REST request to expire old lead offers");
        int count = leadOfferService.expireOldOffers();
        return ResponseEntity.ok(count);
    }
}
