package com.pro.controller.sallahli;

import com.pro.dto.sallahli.LeadOfferDTO;
import com.pro.dto.sallahli.request.LeadAcceptanceRequest;
import com.pro.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/sallahli/leads")
@RequiredArgsConstructor
@Tag(name = "Lead Management", description = "APIs for managing lead offers and acceptances")
public class LeadController {

    private final LeadService leadService;

    @PostMapping("/accept")
    @Operation(summary = "Accept a lead offer")
    public ResponseEntity<Void> acceptLead(@Valid @RequestBody LeadAcceptanceRequest request, @RequestParam Long proId) {
        leadService.acceptLeadOffer(request, proId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pro/{proId}")
    @Operation(summary = "Get lead offers for a professional")
    public ResponseEntity<List<LeadOfferDTO>> getProLeadOffers(@PathVariable Long proId,
                                                              @RequestParam(required = false) String status) {
        // Parse status enum if provided
        com.pro.model.Enum.LeadOfferStatus offerStatus = null;
        if (status != null) {
            try {
                offerStatus = com.pro.model.Enum.LeadOfferStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        List<LeadOfferDTO> offers = leadService.getProLeadOffers(proId, offerStatus);
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/expire-old")
    @Operation(summary = "Expire old lead offers (System task)")
    public ResponseEntity<Void> expireOldOffers() {
        leadService.expireOldOffers();
        return ResponseEntity.ok().build();
    }
}
