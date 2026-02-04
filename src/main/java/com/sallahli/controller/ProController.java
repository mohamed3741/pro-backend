package com.sallahli.controller;

import com.sallahli.dto.sallahli.ProDTO;
import com.sallahli.model.Enum.KycStatus;
import com.sallahli.service.ProService;
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
@RequestMapping("/pros")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Professional Management", description = "APIs for managing service professionals")
public class ProController {

    private final ProService proService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all professionals", description = "Returns all professionals (Admin only)")
    public ResponseEntity<List<ProDTO>> findAll() {
        log.debug("REST request to get all pros");
        return ResponseEntity.ok(proService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Get pro by ID", description = "Returns a single professional profile")
    public ResponseEntity<ProDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get pro {}", id);
        return ResponseEntity.ok(proService.findById(id));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a professional", description = "Registers a new professional")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pro created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ProDTO> create(@RequestBody ProDTO dto) {
        log.debug("REST request to create pro: {}", dto);
        ProDTO created = proService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Update a professional", description = "Updates professional profile")
    public ResponseEntity<ProDTO> update(@PathVariable Long id, @RequestBody ProDTO dto) {
        log.debug("REST request to update pro {}: {}", id, dto);
        return ResponseEntity.ok(proService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive a professional", description = "Soft deletes a professional (Admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to archive pro {}", id);
        proService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    @Operation(summary = "Pro Signup", description = "Register a new professional account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pro registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate telephone")
    })
    public ResponseEntity<ProDTO> signup(@RequestBody ProDTO dto) {
        log.debug("REST request for pro signup: {}", dto.getTel());
        ProDTO created = proService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Get my profile", description = "Returns the current pro's own profile")
    public ResponseEntity<ProDTO> getMyProfile(@RequestParam Long proId) {
        log.debug("REST request to get own profile for pro {}", proId);
        return ResponseEntity.ok(proService.getMyProfile(proId));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Update my profile", description = "Pro updates their own profile information")
    public ResponseEntity<ProDTO> updateMyProfile(
            @RequestParam Long proId,
            @RequestBody ProDTO dto) {
        log.debug("REST request for pro {} to update their profile", proId);
        return ResponseEntity.ok(proService.updateProfile(proId, dto));
    }

    @PostMapping("/me/kyc/submit")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Submit KYC documents", description = "Pro submits their identity documents for verification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KYC documents submitted successfully"),
            @ApiResponse(responseCode = "400", description = "KYC already submitted or approved")
    })
    public ResponseEntity<ProDTO> submitKycDocuments(
            @RequestParam Long proId,
            @Parameter(description = "CNI Front Media ID") @RequestParam(required = false) Long cniFrontMediaId,
            @Parameter(description = "CNI Back Media ID") @RequestParam(required = false) Long cniBackMediaId,
            @Parameter(description = "Selfie Media ID") @RequestParam(required = false) Long selfieMediaId,
            @Parameter(description = "Trade Document Media ID") @RequestParam(required = false) Long tradeDocMediaId) {
        log.debug("REST request for pro {} to submit KYC documents", proId);
        return ResponseEntity.ok(
                proService.submitKycDocuments(proId, cniFrontMediaId, cniBackMediaId, selfieMediaId, tradeDocMediaId));
    }

    @GetMapping("/me/kyc/status")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Get my KYC status", description = "Returns the current pro's KYC status")
    public ResponseEntity<KycStatus> getMyKycStatus(@RequestParam Long proId) {
        log.debug("REST request to get KYC status for pro {}", proId);
        return ResponseEntity.ok(proService.getMyKycStatus(proId));
    }

    @PostMapping("/me/go-online")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Go online", description = "Pro starts accepting leads")
    public ResponseEntity<ProDTO> goOnline(@RequestParam Long proId) {
        log.debug("REST request for pro {} to go online", proId);
        return ResponseEntity.ok(proService.goOnline(proId));
    }

    @PostMapping("/me/go-offline")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Go offline", description = "Pro stops accepting leads")
    public ResponseEntity<ProDTO> goOffline(@RequestParam Long proId) {
        log.debug("REST request for pro {} to go offline", proId);
        return ResponseEntity.ok(proService.goOffline(proId));
    }

    @PutMapping("/me/location")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Update location", description = "Pro updates their current location")
    public ResponseEntity<ProDTO> updateLocation(
            @RequestParam Long proId,
            @Parameter(description = "Latitude") @RequestParam Double latitude,
            @Parameter(description = "Longitude") @RequestParam Double longitude) {
        log.debug("REST request for pro {} to update location to ({}, {})", proId, latitude, longitude);
        return ResponseEntity.ok(proService.updateLocation(proId, latitude, longitude));
    }

    @PutMapping("/me/low-balance-threshold")
    @PreAuthorize("hasRole('PRO')")
    @Operation(summary = "Update my low balance threshold", description = "Pro sets their low balance notification threshold")
    public ResponseEntity<ProDTO> updateMyLowBalanceThreshold(
            @RequestParam Long proId,
            @Parameter(description = "New threshold value") @RequestParam Long threshold) {
        log.debug("REST request for pro {} to update low balance threshold to {}", proId, threshold);
        return ResponseEntity.ok(proService.updateMyLowBalanceThreshold(proId, threshold));
    }

    @GetMapping("/by-tel/{tel}")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Find pro by telephone", description = "Returns professional by phone number")
    public ResponseEntity<ProDTO> findByTel(@PathVariable String tel) {
        log.debug("REST request to find pro by tel: {}", tel);
        return ResponseEntity.ok(proService.findByTel(tel));
    }

    @GetMapping("/kyc/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pros by KYC status", description = "Returns all professionals with a specific KYC status")
    public ResponseEntity<List<ProDTO>> findByKycStatus(@PathVariable KycStatus status) {
        log.debug("REST request to get pros with KYC status {}", status);
        return ResponseEntity.ok(proService.findByKycStatus(status));
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending KYC applications", description = "Returns all professionals awaiting KYC approval")
    public ResponseEntity<List<ProDTO>> findPendingKycApplications() {
        log.debug("REST request to get pending KYC applications");
        return ResponseEntity.ok(proService.findPendingKycApplications());
    }

    @PostMapping("/{id}/kyc/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve KYC", description = "Approves a professional's KYC application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KYC approved"),
            @ApiResponse(responseCode = "400", description = "KYC is not pending")
    })
    public ResponseEntity<ProDTO> approveKyc(
            @PathVariable Long id,
            @Parameter(description = "Approver user ID") @RequestParam Long approvedBy) {
        log.debug("REST request to approve KYC for pro {}", id);
        return ResponseEntity.ok(proService.approveKyc(id, approvedBy));
    }

    @PostMapping("/{id}/kyc/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject KYC", description = "Rejects a professional's KYC application")
    public ResponseEntity<ProDTO> rejectKyc(
            @PathVariable Long id,
            @Parameter(description = "Rejection reason") @RequestParam String reason) {
        log.debug("REST request to reject KYC for pro {} with reason: {}", id, reason);
        return ResponseEntity.ok(proService.rejectKyc(id, reason));
    }

    @PostMapping("/{id}/online")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Set online status", description = "Sets professional's online/offline status")
    public ResponseEntity<ProDTO> setOnlineStatus(
            @PathVariable Long id,
            @Parameter(description = "Online status") @RequestParam boolean online) {
        log.debug("REST request to set pro {} online status to {}", id, online);
        return ResponseEntity.ok(proService.setOnlineStatus(id, online));
    }

    @GetMapping("/online")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get online professionals", description = "Returns all currently online professionals")
    public ResponseEntity<List<ProDTO>> findOnlinePros() {
        log.debug("REST request to get online pros");
        return ResponseEntity.ok(proService.findOnlinePros());
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get available professionals", description = "Returns pros that are online, approved, and have sufficient balance")
    public ResponseEntity<List<ProDTO>> findAvailablePros(
            @Parameter(description = "Minimum wallet balance") @RequestParam(defaultValue = "0") Long minBalance) {
        log.debug("REST request to get available pros with min balance {}", minBalance);
        return ResponseEntity.ok(proService.findAvailablePros(minBalance));
    }

    @GetMapping("/available/by-trade/{tradeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get available pros by trade", description = "Returns available pros for a specific trade/category")
    public ResponseEntity<List<ProDTO>> findAvailableProsByTrade(
            @PathVariable Long tradeId,
            @RequestParam(defaultValue = "0") Long minBalance) {
        log.debug("REST request to get available pros for trade {} with min balance {}", tradeId, minBalance);
        return ResponseEntity.ok(proService.findAvailableProsByTrade(tradeId, minBalance));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate account", description = "Activates a professional's account")
    public ResponseEntity<ProDTO> activateAccount(@PathVariable Long id) {
        log.debug("REST request to activate pro account {}", id);
        return ResponseEntity.ok(proService.activateAccount(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate account", description = "Deactivates a professional's account")
    public ResponseEntity<ProDTO> deactivateAccount(@PathVariable Long id) {
        log.debug("REST request to deactivate pro account {}", id);
        return ResponseEntity.ok(proService.deactivateAccount(id));
    }

    @PutMapping("/{id}/low-balance-threshold")
    @PreAuthorize("hasAnyRole('PRO', 'ADMIN')")
    @Operation(summary = "Update low balance threshold", description = "Sets the low balance notification threshold")
    public ResponseEntity<ProDTO> updateLowBalanceThreshold(
            @PathVariable Long id,
            @Parameter(description = "New threshold value") @RequestParam Long threshold) {
        log.debug("REST request to update low balance threshold for pro {} to {}", id, threshold);
        return ResponseEntity.ok(proService.updateLowBalanceThreshold(id, threshold));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Count active pros", description = "Returns count of approved and active professionals")
    public ResponseEntity<Long> countApprovedActivePros() {
        log.debug("REST request to count approved active pros");
        return ResponseEntity.ok(proService.countApprovedActivePros());
    }

    @GetMapping("/stats/average-rating")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get average rating", description = "Returns average rating across all professionals")
    public ResponseEntity<Double> getAverageRating() {
        log.debug("REST request to get average rating");
        return ResponseEntity.ok(proService.getAverageRating());
    }
}
